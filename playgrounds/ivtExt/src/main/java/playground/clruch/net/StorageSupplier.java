package playground.clruch.net;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ch.ethz.idsc.tensor.io.ObjectFormat;
import playground.clruch.utils.CompressionUtils;

public class StorageSupplier {

    public static StorageSupplier getDefault() {
        return new StorageSupplier(StorageUtils.getAvailable());
    }

    private final Map<Integer, File> map;
    private final List<File> ordered;

    private StorageSupplier(Map<Integer, File> map) {
        this.map = map;
        ordered = new ArrayList<>(map.values());
    }

    public SimulationObject getSimulationObject(int index) throws Exception {
        File file = ordered.get(index);
        byte[] bytes = CompressionUtils.decompress(Files.readAllBytes(file.toPath()));
        return (SimulationObject) ObjectFormat.from(bytes);
    }

    public int size() {
        return ordered.size();
    }

}
