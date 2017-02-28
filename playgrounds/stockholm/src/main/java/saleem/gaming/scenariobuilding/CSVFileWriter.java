package saleem.gaming.scenariobuilding;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

/**
 * A class to write CSV files.
 * 
 */
public final class CSVFileWriter {
	private static final Logger log = Logger.getLogger(CSVFileWriter.class);

	private String separator;
	private BufferedWriter writer ;

	
	/**
	 * writes the header
	 */
	public CSVFileWriter(String path, String separator){
		log.info("Initializing the writer.");
		
		this.separator = separator;
		
		try {
		writer = IOUtils.getBufferedWriter(path);
		} catch (Exception ee) {
			ee.printStackTrace();
			throw new RuntimeException("writer could not be instantiated");
		}

		if (writer==null) {
			throw new RuntimeException("writer is null");
		}

		log.info("... done!");
	}

	
	public final void writeField(double value) {
		try {
			writer.write(value + this.separator);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}

	
	public final void writeField(int value) {
		try {
			writer.write(value + this.separator);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}

	
	public final void writeField(Id<?> value) {
		try {
			writer.write(value + this.separator);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}
	
	
	public final void writeField(String value) {
		try {
			writer.write(value + this.separator);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}

	
	public final void writeNewLine() {
		try {
			writer.newLine() ;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("i/o failure") ;
		}
	}

	
	/**
	 * finalize and close csv file
	 */
	public final void close(){
		try {
			log.info("Closing ...");
			assert(writer != null);
			writer.flush();
			writer.close();
			log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}