// code by jph
package playground.clruch.net;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.queuey.view.util.net.ObjectHandler;
import ch.ethz.idsc.tensor.io.ObjectFormat;

public class StorageSubscriber implements ObjectHandler {
    @Override
    public void handle(Object object) {
        try {
            SimulationObject simulationObject = (SimulationObject) object;
            File file = StorageUtils.getFileForStorageOf(simulationObject);
            final byte[] bytes = ObjectFormat.of(simulationObject);
            Files.write(Paths.get(file.toString()), bytes);
        } catch (Exception exception) {
            exception.printStackTrace();
            GlobalAssert.that(false);
        }
    }
}
