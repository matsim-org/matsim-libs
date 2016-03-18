package playground.polettif.boescpa.lib.obj;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;

/**
 *
 *
 * @author boescpa
 */
public class CSVReader {
    private static Logger log = Logger.getLogger(CSVReader.class);

    private final BufferedReader bufferedReader;

    private String delimiter = ";";

    public CSVReader(final String pathToCSVFile) {
        this.bufferedReader = IOUtils.getBufferedReader(pathToCSVFile);
    }

    /**
     *
     * @return String array with the split elements of the CSV line, or Null if the end of the file is reached.
     *
     * @throws IOException
     */
    public String[] readLine() {
        try {
            String newLine = bufferedReader.readLine();
            if (newLine != null) {
                return newLine.split(this.delimiter);
            } else {
                return null;
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public void skipLine(){
        this.readLine();
    }

    /**
     * Default delimiter is ";".
     * @param delimiter
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }
}
