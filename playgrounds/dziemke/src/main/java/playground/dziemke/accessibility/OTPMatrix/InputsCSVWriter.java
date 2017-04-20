package playground.dziemke.accessibility.OTPMatrix;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author dziemke
 */
public final class InputsCSVWriter {
	private static final Logger LOG = Logger.getLogger(InputsCSVWriter.class);

//	private static final String SEPARATOR = ",";
	private String separator;
	private BufferedWriter writer ;

	
	/**
	 * writes the header
	 */
	public InputsCSVWriter(String path, String separator){
		LOG.info("Initializing  ...");
		
		this.separator = separator;
		
		try {
		writer = IOUtils.getBufferedWriter( path );
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException("writer could not be instantiated") ;
		}

		LOG.info("... done!");
	}

	
	public final void writeField( double val ) {
		try {
//			writer.write( val + SEPARATOR ) ;
			writer.write( val + this.separator ) ;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}

	
	public final void writeField( int val ) {
		try {
//			writer.write( val + SEPARATOR ) ;
			writer.write( val + this.separator ) ;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}

	
	public final void writeField( Id<?> val ) {
		try {
//			writer.write( val + SEPARATOR ) ;
			writer.write( val + this.separator ) ;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}
	
	public final void writeField( String val ) {
		try {
//			writer.write( val + SEPARATOR ) ;
			writer.write( val + this.separator ) ;
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
			LOG.info("Closing ...");
			assert(writer != null);
			writer.flush();
			writer.close();
			LOG.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
}
