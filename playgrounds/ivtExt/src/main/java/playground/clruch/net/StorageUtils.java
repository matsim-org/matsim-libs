// code by jph
package playground.clruch.net;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.ethz.idsc.queuey.util.GlobalAssert;

public enum StorageUtils {
    ;
    // ---
    public static File OUTPUT = new File("output"); // folder created by MATSim
    public static File DIRECTORY = new File(OUTPUT, "simobj");

    /**
     * function only called from {@link StorageSubscriber} when data is recorded during simulation
     * 
     * @param simulationObject
     * @return file to store given simulationObject
     */
    public static File getFileForStorageOf(SimulationObject simulationObject) {
        GlobalAssert.that(OUTPUT.exists());
        DIRECTORY.mkdir();
        File iter = new File(DIRECTORY, String.format("it.%02d", simulationObject.iteration));
        iter.mkdir();
        long floor = (simulationObject.now / 1000) * 1000;
        File folder = new File(iter, String.format("%07d", floor));
        folder.mkdir();
        GlobalAssert.that(folder.exists());
        return new File(folder, String.format("%07d.bin", simulationObject.now));
    }

    public static List<IterationFolder> getAvailableIterations() {
        if (!DIRECTORY.isDirectory()) {
            System.out.println("no iterations found");
            return Collections.emptyList();
        }
        return Stream.of(DIRECTORY.listFiles()).sorted() //
                .map(IterationFolder::new) //
                .collect(Collectors.toList());
    }

    public static NavigableMap<Integer, File> getFrom(File itDir) {
        NavigableMap<Integer, File> navigableMap = new TreeMap<>();
        for (File dir : itDir.listFiles())
            if (dir.isDirectory())
                for (File file : dir.listFiles())
                    if (file.isFile())
                        navigableMap.put(Integer.parseInt(file.getName().substring(0, 7)), file);
        return navigableMap;
    }

    static NavigableMap<Integer, File> getAvailable() {
        if (!DIRECTORY.isDirectory()) {
            System.out.println("no files found");
            return Collections.emptyNavigableMap();
        }
        File[] files = DIRECTORY.listFiles(); // TODO probably not sorted
        if (files.length == 0)
            return Collections.emptyNavigableMap();
        // NavigableMap<Integer, File> navigableMap = new TreeMap<>();
        File lastIter = files[files.length - 1];
        System.out.println("loading last Iter = " + lastIter);
        return getFrom(lastIter);
        // for (File dir : lastIter.listFiles())
        // if (dir.isDirectory())
        // for (File file : dir.listFiles())
        // if (file.isFile())
        // navigableMap.put(Integer.parseInt(file.getName().substring(0, 7)), file);
        // return navigableMap;
    }

}
