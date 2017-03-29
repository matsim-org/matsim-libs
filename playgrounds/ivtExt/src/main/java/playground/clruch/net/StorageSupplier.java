package playground.clruch.net;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;

import ch.ethz.idsc.tensor.io.ObjectFormat;
import playground.clruch.utils.CompressionUtils;

public class StorageSupplier {

    public static StorageSupplier getDefault() {
        return new StorageSupplier(StorageUtils.getAvailable());
    }

    private final NavigableMap<Integer, File> navigableMap;
    private final List<File> ordered;

    private StorageSupplier(NavigableMap<Integer, File> map) {
        this.navigableMap = map;
        ordered = new ArrayList<>(map.values());
    }

    /**
     * 
     * @param index
     * @return
     * @throws Exception
     *             if anything goes wrong, for instance file not found, or object cannot be cast to SimulationObject
     */
    public SimulationObject getSimulationObject(int index) throws Exception {
        return readFromFile(ordered.get(index));
    }

    public SimulationObject getSimulationObjectForTime(int now) throws Exception {
        Entry<Integer, File> entry = navigableMap.lowerEntry(now + 1);
        return readFromFile(entry.getValue());
    }

    private static SimulationObject readFromFile(File file) throws Exception {
        byte[] bytes = CompressionUtils.decompress(Files.readAllBytes(file.toPath()));
        return (SimulationObject) ObjectFormat.from(bytes);

    }

    public int size() {
        return ordered.size();
    }

}
