package org.matsim.contrib.accessibility;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

class CellBasedAccessibilityCSVWriter {
	private static final Logger log = Logger.getLogger(CellBasedAccessibilityCSVWriter.class);

	private static final String FILE_NAME= "accessibilities.csv";
	
	private static final String SEPARATOR = "\t"; // comma is the correct choice for excel.  But gnuplot cannot deal with comma!

	private BufferedWriter writer ;

	/**
	 * writes the header of accessibility data csv file
	 */
	public CellBasedAccessibilityCSVWriter(String matsimOutputDirectory){
		log.info("Initializing  ...");
		try {
		writer = IOUtils.getBufferedWriter( matsimOutputDirectory + "/" + FILE_NAME );
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException("writer could not be instantiated") ;
		}

		if ( writer==null ) {
			throw new RuntimeException( "writer is null") ;
		}

		log.info("... done!");
	}

	public void writeField( double val ) {
		try {
			writer.write( val + SEPARATOR ) ;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}

	public void writeNewLine() {
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
	public void close(){
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
