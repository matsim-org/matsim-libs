/**
 * 
 */
package playground.clruch.analysis.minimumfleetsize;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.DataFormatException;

import ch.ethz.idsc.tensor.io.ObjectFormat;

/** @author Claudio Ruch */
public class MinimumFleetSizeIO {

    /** Saves MinimumFleetSizeCalculator as a bitmap file
     * 
     * @param file
     * @param MinimumFleetSizeCalculator
     * @throws IOException */
    public static void toByte(File file, MinimumFleetSizeCalculator minimumFleetSizeCalculator) throws IOException {
        minimumFleetSizeCalculator.checkConsistency();
        Files.write(file.toPath(), ObjectFormat.of(minimumFleetSizeCalculator));
    }

    /** loads MinimumFleetSizeCalculator from a bitmap file
     * 
     * @param file
     * @return
     * @throws ClassNotFoundException
     * @throws DataFormatException
     * @throws IOException */
    public static MinimumFleetSizeCalculator fromByte(File file) throws ClassNotFoundException, DataFormatException, IOException {
        MinimumFleetSizeCalculator minimumFleetSizeCalculator = ObjectFormat.parse(Files.readAllBytes(file.toPath()));
        minimumFleetSizeCalculator.checkConsistency();
        return minimumFleetSizeCalculator;
    }

}
