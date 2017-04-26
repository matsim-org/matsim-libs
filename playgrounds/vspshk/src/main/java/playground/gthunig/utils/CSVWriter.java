package playground.gthunig.utils;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * @author GabrielT on 07.11.2016.
 */
public class CSVWriter {

	private static final Logger log = Logger.getLogger(org.matsim.contrib.accessibility.CSVWriter.class);

	private static String SEPARATOR = ",";
	// comma is the correct choice for excel.  But gnuplot cannot deal with comma, needs "\t"!

	private BufferedWriter writer ;

	public CSVWriter( String path) {
		initialize(path);
	}

	public CSVWriter( String path, String seperator ) {
		SEPARATOR = seperator;
		initialize(path);
	}

	/**
	 * writes the header of accessibility data csv file
	 */
	private void initialize( String path ){
		log.info("Initializing  ...");
		try {
			writer = IOUtils.getBufferedWriter( path );
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException("writer could not be instantiated") ;
		}

		if ( writer==null ) {
			throw new RuntimeException( "writer is null") ;
		}

		log.info("... done!");
	}

	public final void writeLine( String[] line ) {
		for (String field : line) {
			writeField(field);
		}
		writeNewLine();
	}

	public final void writeLine( String line ) {
		try {
			writer.write( line ) ;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
		writeNewLine();
	}

	public final void writeField( String val ) {
		try {
			writer.write( val + SEPARATOR ) ;
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
