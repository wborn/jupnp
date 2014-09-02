/**
 * Copyright (C) 2014 4th Line GmbH, Switzerland and others
 *
 * The contents of this file are subject to the terms of either the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.jupnp.binding.staging;

import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Christian Bauer
 */
public class MutableAction {

    public String name;
    public List<MutableActionArgument> arguments = new ArrayList();

    public Action build() {
        return new Action(name, createActionArgumennts());
    }

    public ActionArgument[] createActionArgumennts() {
        ActionArgument[] array = new ActionArgument[arguments.size()];
        int i = 0;
        for (MutableActionArgument argument : arguments) {
            array[i++] = argument.build();
        }
        return array;
    }

}