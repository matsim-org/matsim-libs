package playground.dhosse.utils.io;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

public abstract class AbstractCsvReader {

	static final Logger log = Logger.getLogger(AbstractCsvReader.class);
	
	String sep;
	boolean hasHeader;
	
	/**
	 * Constructs a csv reader with default settings:<br>
	 * <ul>
	 * <li>separator = ","
	 * <li>header = true
	 * </ul>
	 */
	public AbstractCsvReader(){
		
		this(",", true);
		
	}

	/**
	 * 
	 * Constructs a csv reader with the following settings:
	 * <ul>
	 * <li>separator = user defined
	 * <li>header = user defined
	 * </ul>
	 * 
	 * @param separator The character that separates the data fields of a row.
	 * @param hasHeader Defines whether the csv file has a header row or not.
	 */
	public AbstractCsvReader(String separator, boolean hasHeader){
		
		this.sep = separator;
		this.hasHeader = hasHeader;
		
	}
	
	/**
	 * 
	 * Reads the given input file.
	 * 
	 * @param file The input file.
	 * @throws IOException
	 */
	public void read(String file) {
		
		log.info("Reading file " + file + "...");
		log.info("Column separator value is '" + this.sep + "'");
		
		BufferedReader reader = IOUtils.getBufferedReader(file);
		
		String line = null;
		
		try {
		
			if(this.hasHeader){
				
				//skip one line if the file contains a header
				line = reader.readLine();
				
			}
			
			while((line = reader.readLine()) != null){
				
				this.handleRow(line.split(this.sep));
				
			}
			
			log.info("Done.");
			
			reader.close();
		
		} catch(IOException e){
			
			e.printStackTrace();
			
		}
		
	}
	
	public abstract void handleRow(String[] line);
	
}
