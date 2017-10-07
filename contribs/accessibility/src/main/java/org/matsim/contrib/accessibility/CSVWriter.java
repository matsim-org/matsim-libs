package org.matsim.contrib.accessibility;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

public final class CSVWriter {
	private static final Logger log = Logger.getLogger(CSVWriter.class);

	public static final String FILE_NAME= "accessibilities.csv";
	
	private static final String SEPARATOR = ",";
	// comma is the correct choice for excel.  But gnuplot cannot deal with comma, needs "\t"!

	private BufferedWriter writer ;

	private boolean first = true ;

	/**
	 * writes the header of accessibility data csv file
	 */
	public CSVWriter(String path){
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
			if ( first ) {
				writer.write( val + "" ) ;
				first = false ;
			} else {
				writer.write( SEPARATOR + val ) ;
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}
	
	public final void writeField( String val ) {
		try {
			if ( first ) {
				writer.write( val ) ;
				first = false ;
			} else {
				writer.write( SEPARATOR + val ) ;
			}
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
		first = true ;
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
