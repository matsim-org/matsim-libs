/**
 * 
 */
package playground.clruch.io.fleet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** @author Claudio Ruch */
public class ZHFileReader {

    public File OUTPUT = new File("output"); // folder created by MATSim
    public final File filesDirectory;
    private final List<File> trailFiles;

    public ZHFileReader(File filesDirectory, String sharedFileName) {
        this.filesDirectory = filesDirectory;
        trailFiles = getFahrStreckenProtokolle(sharedFileName);
    }

    /** @param sharedFileName
     * @return all files in filesDirectory that have the sequence @param sharedFileName in their filename */
    private List<File> getFahrStreckenProtokolle(String sharedFileName) {
        List<File> relevantFiles = new ArrayList<>();
        if (filesDirectory.isDirectory())
            for (File file : filesDirectory.listFiles()) {
                if (file.getName().contains(sharedFileName)) {
                    relevantFiles.add(file);
                }
            }
        return relevantFiles;

    }

    public List<File> getTrailFiles() {
        return Collections.unmodifiableList(trailFiles);
    }

}
