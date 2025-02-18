/*
 * Copyright (C) 2011-2025 4th Line GmbH, Switzerland and others
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * SPDX-License-Identifier: CDDL-1.0
 */
package org.jupnp.binding.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jupnp.binding.LocalServiceBindingException;
import org.jupnp.model.Constants;
import org.jupnp.model.ModelUtil;
import org.jupnp.model.action.ActionExecutor;
import org.jupnp.model.action.MethodActionExecutor;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.profile.RemoteClientInfo;
import org.jupnp.model.state.GetterStateVariableAccessor;
import org.jupnp.model.state.StateVariableAccessor;
import org.jupnp.model.types.Datatype;
import org.jupnp.util.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Bauer
 */
public class AnnotationActionBinder {

    private final Logger logger = LoggerFactory.getLogger(AnnotationLocalServiceBinder.class);

    protected UpnpAction annotation;
    protected Method method;
    protected Map<StateVariable, StateVariableAccessor> stateVariables;
    protected Set<Class> stringConvertibleTypes;

    public AnnotationActionBinder(Method method, Map<StateVariable, StateVariableAccessor> stateVariables,
            Set<Class> stringConvertibleTypes) {
        this.annotation = method.getAnnotation(UpnpAction.class);
        this.stateVariables = stateVariables;
        this.method = method;
        this.stringConvertibleTypes = stringConvertibleTypes;
    }

    public UpnpAction getAnnotation() {
        return annotation;
    }

    public Map<StateVariable, StateVariableAccessor> getStateVariables() {
        return stateVariables;
    }

    public Method getMethod() {
        return method;
    }

    public Set<Class> getStringConvertibleTypes() {
        return stringConvertibleTypes;
    }

    public Action appendAction(Map<Action, ActionExecutor> actions) throws LocalServiceBindingException {

        String name;
        if (!getAnnotation().name().isEmpty()) {
            name = getAnnotation().name();
        } else {
            name = AnnotationLocalServiceBinder.toUpnpActionName(getMethod().getName());
        }

        logger.trace("Creating action and executor: {}", name);

        List<ActionArgument> inputArguments = createInputArguments();
        Map<ActionArgument<LocalService>, StateVariableAccessor> outputArguments = createOutputArguments();

        inputArguments.addAll(outputArguments.keySet());
        ActionArgument<LocalService>[] actionArguments = inputArguments
                .toArray(new ActionArgument[inputArguments.size()]);

        Action action = new Action(name, actionArguments);
        ActionExecutor executor = createExecutor(outputArguments);

        actions.put(action, executor);
        return action;
    }

    protected ActionExecutor createExecutor(Map<ActionArgument<LocalService>, StateVariableAccessor> outputArguments) {
        // TODO: Invent an annotation for this configuration
        return new MethodActionExecutor(outputArguments, getMethod());
    }

    protected List<ActionArgument> createInputArguments() throws LocalServiceBindingException {

        List<ActionArgument> list = new ArrayList<>();

        // Input arguments are always method parameters
        int annotatedParams = 0;
        Annotation[][] params = getMethod().getParameterAnnotations();
        for (int i = 0; i < params.length; i++) {
            Annotation[] param = params[i];
            for (Annotation paramAnnotation : param) {
                if (paramAnnotation instanceof UpnpInputArgument) {
                    UpnpInputArgument inputArgumentAnnotation = (UpnpInputArgument) paramAnnotation;
                    annotatedParams++;

                    String argumentName = inputArgumentAnnotation.name();

                    StateVariable stateVariable = findRelatedStateVariable(inputArgumentAnnotation.stateVariable(),
                            argumentName, getMethod().getName());

                    if (stateVariable == null) {
                        throw new LocalServiceBindingException(
                                "Could not detected related state variable of argument: " + argumentName);
                    }

                    validateType(stateVariable, getMethod().getParameterTypes()[i]);

                    ActionArgument inputArgument = new ActionArgument(argumentName, inputArgumentAnnotation.aliases(),
                            stateVariable.getName(), ActionArgument.Direction.IN);

                    list.add(inputArgument);
                }
            }
        }
        // A method can't have any parameters that are not annotated with @UpnpInputArgument - we wouldn't know what
        // value to pass when we invoke it later on... unless the last parameter is of type RemoteClientInfo
        if (annotatedParams < getMethod().getParameterTypes().length && !RemoteClientInfo.class
                .isAssignableFrom(method.getParameterTypes()[method.getParameterTypes().length - 1])) {
            throw new LocalServiceBindingException(
                    "Method has parameters that are not input arguments: " + getMethod().getName());
        }

        return list;
    }

    protected Map<ActionArgument<LocalService>, StateVariableAccessor> createOutputArguments()
            throws LocalServiceBindingException {

        Map<ActionArgument<LocalService>, StateVariableAccessor> map = new LinkedHashMap<>(); // !!! Insertion order!

        UpnpAction actionAnnotation = getMethod().getAnnotation(UpnpAction.class);
        if (actionAnnotation.out().length == 0) {
            return map;
        }

        boolean hasMultipleOutputArguments = actionAnnotation.out().length > 1;

        for (UpnpOutputArgument outputArgumentAnnotation : actionAnnotation.out()) {

            String argumentName = outputArgumentAnnotation.name();

            StateVariable stateVariable = findRelatedStateVariable(outputArgumentAnnotation.stateVariable(),
                    argumentName, getMethod().getName());

            // Might-just-work attempt, try the name of the getter
            if (stateVariable == null && !outputArgumentAnnotation.getterName().isEmpty()) {
                stateVariable = findRelatedStateVariable(null, null, outputArgumentAnnotation.getterName());
            }

            if (stateVariable == null) {
                throw new LocalServiceBindingException(
                        "Related state variable not found for output argument: " + argumentName);
            }

            StateVariableAccessor accessor = findOutputArgumentAccessor(stateVariable,
                    outputArgumentAnnotation.getterName(), hasMultipleOutputArguments);

            logger.trace("Found related state variable for output argument '{}': {}", argumentName, stateVariable);

            ActionArgument outputArgument = new ActionArgument(argumentName, stateVariable.getName(),
                    ActionArgument.Direction.OUT, !hasMultipleOutputArguments);

            map.put(outputArgument, accessor);
        }

        return map;
    }

    protected StateVariableAccessor findOutputArgumentAccessor(StateVariable stateVariable, String getterName,
            boolean multipleArguments) throws LocalServiceBindingException {

        boolean isVoid = getMethod().getReturnType().equals(Void.TYPE);

        if (isVoid) {

            if (getterName != null && !getterName.isEmpty()) {
                logger.trace("Action method is void, will use getter method named: {}", getterName);

                // Use the same class as the action method
                Method getter = Reflections.getMethod(getMethod().getDeclaringClass(), getterName);
                if (getter == null) {
                    throw new LocalServiceBindingException("Declared getter method '" + getterName + "' not found on: "
                            + getMethod().getDeclaringClass());
                }

                validateType(stateVariable, getter.getReturnType());

                return new GetterStateVariableAccessor(getter);

            } else {
                logger.trace("Action method is void, trying to find existing accessor of related: {}", stateVariable);
                return getStateVariables().get(stateVariable);
            }

        } else if (getterName != null && !getterName.isEmpty()) {
            logger.trace("Action method is not void, will use getter method on returned instance: {}", getterName);

            // Use the returned class
            Method getter = Reflections.getMethod(getMethod().getReturnType(), getterName);
            if (getter == null) {
                throw new LocalServiceBindingException("Declared getter method '" + getterName
                        + "' not found on return type: " + getMethod().getReturnType());
            }

            validateType(stateVariable, getter.getReturnType());

            return new GetterStateVariableAccessor(getter);

        } else if (!multipleArguments) {
            logger.trace("Action method is not void, will use the returned instance: {}", getMethod().getReturnType());
            validateType(stateVariable, getMethod().getReturnType());
        }

        return null;
    }

    protected StateVariable findRelatedStateVariable(String declaredName, String argumentName, String methodName)
            throws LocalServiceBindingException {

        StateVariable relatedStateVariable = null;

        if (declaredName != null && !declaredName.isEmpty()) {
            relatedStateVariable = getStateVariable(declaredName);
        }

        if (relatedStateVariable == null && argumentName != null && !argumentName.isEmpty()) {
            String actualName = AnnotationLocalServiceBinder.toUpnpStateVariableName(argumentName);
            logger.trace("Finding related state variable with argument name (converted to UPnP name): {}", actualName);
            relatedStateVariable = getStateVariable(argumentName);
        }

        if (relatedStateVariable == null && argumentName != null && !argumentName.isEmpty()) {
            // Try with A_ARG_TYPE prefix
            String actualName = AnnotationLocalServiceBinder.toUpnpStateVariableName(argumentName);
            actualName = Constants.ARG_TYPE_PREFIX + actualName;
            logger.trace("Finding related state variable with prefixed argument name (converted to UPnP name): {}",
                    actualName);
            relatedStateVariable = getStateVariable(actualName);
        }

        if (relatedStateVariable == null && methodName != null && !methodName.isEmpty()) {
            // TODO: Well, this is often a nice shortcut but sometimes might have false positives
            String methodPropertyName = Reflections.getMethodPropertyName(methodName);
            if (methodPropertyName != null) {
                logger.trace("Finding related state variable with method property name: {}", methodPropertyName);
                relatedStateVariable = getStateVariable(
                        AnnotationLocalServiceBinder.toUpnpStateVariableName(methodPropertyName));
            }
        }

        return relatedStateVariable;
    }

    protected void validateType(StateVariable stateVariable, Class type) throws LocalServiceBindingException {

        // Validate datatype as good as we can
        // (for enums and other convertible types, the state variable type should be STRING)

        Datatype.Default expectedDefaultMapping = ModelUtil.isStringConvertibleType(getStringConvertibleTypes(), type)
                ? Datatype.Default.STRING
                : Datatype.Default.getByJavaType(type);

        logger.trace("Expecting '{}' to match default mapping: {}", stateVariable, expectedDefaultMapping);

        if (expectedDefaultMapping != null && !stateVariable.getTypeDetails().getDatatype()
                .isHandlingJavaType(expectedDefaultMapping.getJavaType())) {

            // TODO: Consider custom types?!
            throw new LocalServiceBindingException(
                    "State variable '" + stateVariable + "' datatype can't handle action "
                            + "argument's Java type (change one): " + expectedDefaultMapping.getJavaType());

        } else if (expectedDefaultMapping == null
                && stateVariable.getTypeDetails().getDatatype().getBuiltin() != null) {
            throw new LocalServiceBindingException("State variable '" + stateVariable + "' should be custom datatype "
                    + "(action argument type is unknown Java type): " + type.getSimpleName());
        }

        logger.trace("State variable matches required argument datatype (or can't be validated because it is custom)");
    }

    protected StateVariable getStateVariable(String name) {
        for (StateVariable stateVariable : getStateVariables().keySet()) {
            if (stateVariable.getName().equals(name)) {
                return stateVariable;
            }
        }
        return null;
    }
}
