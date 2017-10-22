/**
 * 
 */
package playground.clruch.analysis;

import java.nio.file.Files;
import java.nio.file.Paths;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.io.CsvFormat;
import ch.ethz.idsc.tensor.io.MathematicaFormat;
import ch.ethz.idsc.tensor.io.MatlabExport;

/** @author Claudio Ruch */
public enum SaveUtils {
    ;

    public static void saveFile(Tensor table, String name, String dataFolderName) throws Exception {
        Files.write(Paths.get(dataFolderName + "/" + name + ".csv"), (Iterable<String>) CsvFormat.of(table)::iterator);
        Files.write(Paths.get(dataFolderName + "/" + name + ".mathematica"), (Iterable<String>) MathematicaFormat.of(table)::iterator);
        Files.write(Paths.get(dataFolderName + "/" + name + ".m"), (Iterable<String>) MatlabExport.of(table)::iterator);
    }

}
