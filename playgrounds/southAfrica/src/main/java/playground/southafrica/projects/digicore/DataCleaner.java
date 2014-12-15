package playground.southafrica.projects.digicore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.Header;

/**
 * Class to remove all accelerometer records for identified individuals.
 *
 * @author jwjoubert
 */
public class DataCleaner {
	private final static Logger LOG = Logger.getLogger(DataCleaner.class);
	private final static List<String> LIST = new ArrayList<String>();

	/**
	 * Implementation of the data cleaner for Digicore accelerometer data.
	 * 
	 * @param args The following arguments are required, and in the following 
	 * order:
	 * <ol>
	 * 	<li> uncleaned input file;
	 * 	<li> cleaned output file.
	 * </ol>
	 */
	public static void main(String[] args) {
		Header.printHeader(DataCleaner.class.toString(), args);

		/* Populate the list. */
		LIST.add("731eec4e9f49399907581e0fbb105a8a");	/* Scattered records indicative of loose unit. */
		LIST.add("1dc6a2682d57603ad29b09a0881fceac");	/* Many zero acceleration records (20141215). */
		
		BufferedReader br = IOUtils.getBufferedReader(args[0]);
		BufferedWriter bw = IOUtils.getBufferedWriter(args[1]);
		
		Counter counter = new Counter("   lines # ");
		int recordsRemoved = 0;
		try{
			String line = null;
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				String id = sa[1];
				if(!LIST.contains(id)){
					bw.write(line);
					bw.newLine();
				} else{
					recordsRemoved++;
				}
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Oops, cannot read/write");
		} finally{
			try {
				br.close();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close files.");
			}
		}
		counter.printCounter();
		LOG.info("Number of records removed: " + recordsRemoved);
		
		Header.printFooter();
	}

}
