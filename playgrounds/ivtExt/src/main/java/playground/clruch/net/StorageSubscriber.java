package playground.clruch.net;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import ch.ethz.idsc.tensor.io.ObjectFormat;
import playground.clruch.utils.CompressionUtils;
import playground.clruch.utils.GlobalAssert;

public class StorageSubscriber implements SimulationSubscriber {
    @Override
    public void handle(SimulationObject simulationObject) {
        final byte[] bytes = ObjectFormat.of(simulationObject);
        try {
            File file = StorageUtils.getFileForStorageOf(simulationObject);
            Files.write(Paths.get(file.toString()), CompressionUtils.compress(bytes));
        } catch (Exception exception) {
            exception.printStackTrace();
            GlobalAssert.that(false);
        }
    }
}
