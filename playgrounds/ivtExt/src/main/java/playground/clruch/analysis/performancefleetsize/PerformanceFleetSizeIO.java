/**
 * 
 */
package playground.clruch.analysis.performancefleetsize;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.DataFormatException;

import ch.ethz.idsc.tensor.io.ObjectFormat;

/**
 * @author Claudio Ruch
 *
 */
public class PerformanceFleetSizeIO {
    
    
    /** Saves PerformanceFleetSizeCalculator as a bitmap file
     * 
     * @param file
     * @param performanceFleetSizeCalculator
     * @throws IOException */
    public static void toByte(File file, PerformanceFleetSizeCalculator performanceFleetSizeCalculator) throws IOException {
        Files.write(file.toPath(), ObjectFormat.of(performanceFleetSizeCalculator));
    }

    /** loads PerformanceFleetSizeCalculator from a bitmap file
     * 
     * @param file
     * @return
     * @throws ClassNotFoundException
     * @throws DataFormatException
     * @throws IOException */
    public static PerformanceFleetSizeCalculator fromByte(File file) throws ClassNotFoundException, DataFormatException, IOException {
        PerformanceFleetSizeCalculator performanceFleetSizeCalculator = ObjectFormat.parse(Files.readAllBytes(file.toPath()));
         return performanceFleetSizeCalculator;
    }

}
