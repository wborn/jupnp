package example.binarylight;

import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.test.data.SampleData;

/**
 * @author Christian Bauer
 */
public class BinaryLightSampleData {

    public static LocalDevice createDevice(Class<?> serviceClass) throws Exception {
        return createDevice(
                SampleData.readService(
                        new AnnotationLocalServiceBinder(),
                        serviceClass
                )
        );
    }

    public static LocalDevice createDevice(LocalService service) throws Exception {
        return new LocalDevice(
                SampleData.createLocalDeviceIdentity(),
                new UDADeviceType("BinaryLight", 1),
                new DeviceDetails("Example Binary Light"),
                service
        );
    }

}
