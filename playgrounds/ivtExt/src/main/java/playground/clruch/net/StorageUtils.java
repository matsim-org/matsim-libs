// code by jph
// removed hard-coded "output" clruch
package playground.clruch.net;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import ch.ethz.idsc.queuey.util.GlobalAssert;

public class StorageUtils {

    /** the output folder is created by MATSim */
    private final File defaultOutputDir = new File("output");
    private final File OUTPUT;
    private final File DIRECTORY;

    public StorageUtils(File outputdirectory) {
        if (outputdirectory.exists()) {
            OUTPUT = outputdirectory;

        } else {
            System.out.println("supplied outputdircetory does not exist, using default");
            System.out.println("outputdirectory = " + defaultOutputDir.getAbsolutePath());
            OUTPUT = defaultOutputDir;
        }

        DIRECTORY = new File(OUTPUT, "simobj");
    }

    public void printStorageProperties() {
        System.out.println("StorageUtils object has properties:");
        System.out.println("OUTPUT File: " + OUTPUT.getAbsolutePath());
        System.out.println("DIRECTORY File: " + DIRECTORY.getAbsolutePath());
        if (OUTPUT.isDirectory()) {
            System.out.println("OUTPUT is present");
        } else {
            System.out.println("OUTPUT not present");
        }

        if (DIRECTORY.isDirectory()) {
            System.out.println("DIRECTORY is present");
        } else {
            System.out.println("DIRECTORY not present");
        }

    }

    /** @return {@link List} of {@link IterationFolder} where simulation results for visualization are stored. */
    public List<IterationFolder> getAvailableIterations() {
        if (!DIRECTORY.isDirectory()) {
            System.out.println("no iterations found");
            return Collections.emptyList();
        }

        List<IterationFolder> returnList = new ArrayList<>();
        Stream.of(DIRECTORY.listFiles()).sorted()//
                .forEach(f -> returnList.add(new IterationFolder(f, this)));
        return returnList;

        // TODO remove this old version.
        // return Stream.of(DIRECTORY.listFiles()).sorted() //
        // .map(IterationFolder::new) //
        // .collect(Collectors.toList());
    }

    /** @return {@link NavigableMap} to with {@link Integer} and {@link File} for the first
     *         available iteration. */
    public NavigableMap<Integer, File> getFirstAvailableIteration() {
        if (!DIRECTORY.isDirectory()) { // no simobj directory
            System.out.println("no files found");
            return Collections.emptyNavigableMap();
        }

        File[] files = Stream.of(DIRECTORY.listFiles()).sorted().toArray(File[]::new);

        if (files.length == 0) {
            System.out.println("no files found");
            return Collections.emptyNavigableMap();
        }

        File lastIter = files[files.length - 1];
        System.out.println("loading last Iter = " + lastIter);
        return getFrom(lastIter);

    }

    /** function only called from {@link StorageSubscriber} when data is recorded during simulation
     * 
     * @param simulationObject

     * @return file to store given simulationObject */
    /* package */ File getFileForStorageOf(SimulationObject simulationObject) {
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

    /** @param itDir {@link File} with iteration folder
     * @return {@link NavigableMap} with time as {@link Integer} and {@link File} with iteration result. */
    /* package */ NavigableMap<Integer, File> getFrom(File itDir) {
        NavigableMap<Integer, File> navigableMap = new TreeMap<>();
        for (File dir : itDir.listFiles())
            if (dir.isDirectory())
                for (File file : dir.listFiles())
                    if (file.isFile())
                        navigableMap.put(Integer.parseInt(file.getName().substring(0, 7)), file);
        return navigableMap;
    }

}
