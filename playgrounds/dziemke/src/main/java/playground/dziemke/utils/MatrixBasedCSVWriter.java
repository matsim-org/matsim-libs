package playground.dziemke.utils;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

public final class MatrixBasedCSVWriter {
	private static final Logger log = Logger.getLogger(MatrixBasedCSVWriter.class);

//	public static final String FILE_NAME= "accessibilities.csv";
//	
	private static final String SEPARATOR = "\t";
	// comma is the correct choice for excel.  But gnuplot cannot deal with comma, needs "\t"!

	private BufferedWriter writer ;

	/**
	 * writes the header of accessibility data csv file
	 */
	public MatrixBasedCSVWriter(String path){
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

	public final void writeField( double val ) {
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
