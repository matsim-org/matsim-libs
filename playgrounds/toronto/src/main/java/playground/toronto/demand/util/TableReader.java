package playground.toronto.demand.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * A simple wrapper around a BufferedReader which reads in a list
 * of delimited records, where the delimiter can be a comma, tab,
 * or several spaces. A custom delimiter can also be specified.
 * A header record must be the first line in the file.
 * 
 * Use TableReader.open() to open the file; this will load the
 * list of headers. To loop through the file, simply use 
 * while(TableReader.next()) as your check statement. To access
 * the current record, use TableReader.current().get(attribute_name) 
 * 
 * @author pkucirek
 *
 */
public class TableReader {

	private String filename;
	private BufferedReader bf;
	private List<String> headers;
	private HashMap<String, String> cr; //current record
	private String regex;
	private int BUFFER_SIZE = 1000; //Maximum buffer size for BufferedReader.mark(int readAheadLimit)
	private boolean ignoreTrailingBlanks = false;
	private boolean truncatingTrailingData = true;
	
	private static final Logger log = Logger.getLogger(TableReader.class);
	
	public TableReader(String filename, String regex){
		this.filename = filename;
		this.regex = regex;
	}
	public TableReader(String filename){
		this.filename = filename;
		this.regex = null;
	}
	
	/**
	 * Opens the current table for reading. Guesses the delimiter, if
	 * none is specified, and loads the headers.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void open() throws FileNotFoundException, IOException{
		bf = new BufferedReader(new FileReader(this.filename));
		String line = bf.readLine();
		if (regex == null) this.TryRegex();
		headers = Arrays.asList(line.split(this.regex));
	}
	
	/**
	 * Closes the current file.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException{
		this.bf.close();
	}
	
	/**
	 * Sets the ignoreTrailingBlanks property. This boolean switch
	 * makes the TableReader ignore records whose length is fewer
	 * than the length of the headers; it assumes that these values
	 * are empty. The remaining records do not have their positions
	 * adjusted (ie, they can be considered 'left-justified'). The
	 * default value for this switch is false.
	 * 
	 * @param t = the boolean value to be set.
	 */
	public void ignoreTrailingBlanks(boolean t){
		this.ignoreTrailingBlanks = t;
	}
	
	/**
	 * Returns the ignoreTrailingBlanks property. This boolean switch
	 * makes the TableReader ignore records whose length is fewer
	 * than the length of the headers; it assumes that these values
	 * are empty. The remaining records do not have their positions
	 * adjusted (ie, they can be considered 'left-justified'). The
	 * default value for this switch is false.
	 * 
	 * @return
	 */
	public boolean isIgnoringTrailingBlanks(){
		return this.ignoreTrailingBlanks;
	}
	
	/**
	 * Sets the truncatingTrailingData property. This boolean switch
	 * makes the TableReader throw an IOException when it encounters
	 * a record whose length is greater than the headers. The
	 * default behaviour is that subsequent records are ignored. The
	 * default value for this switch is true.
	 * 
	 * @param t
	 */
	public void truncateTrailingData(boolean t){
		this.truncatingTrailingData = t;
	}
	/**
	 * Returns the truncatingTrailingData property. This boolean switch
	 * makes the TableReader throw an IOException when it encounters
	 * a record whose length is greater than the headers. The
	 * default behaviour is that subsequent records are ignored. The
	 * default value for this switch is true.
	 * 
	 * @return
	 */
	public boolean isTruncatingTrailingData(){
		return this.truncatingTrailingData;
	}
	
	private void TryRegex() throws IOException{
		//Tries three common delimiters: comma, tab, and whitespace
		bf.mark(BUFFER_SIZE);
		String line = bf.readLine();
		String[] cells = line.split(",");
		if (cells.length > 1){
			this.regex = ",";
		}
		else{
			cells = line.split("\\t");
			if (cells.length > 1) {
				this.regex = "\\t";
			}
			else{
				cells = line.split("\\s+");
				if (cells.length > 1) {
					this.regex = "\\s+";
				}
				else{
					throw new IOException("Could not guess an unspecified delimiter for " + this.filename);
				}
			}
		}
		bf.reset();
		log.info("TableReader: Recognized \"" + this.regex + "\" as the record delimiter");
	}
	
	/**
	 * Reads the next line in the file; returns true if the line is
	 * not null. Should be used in a while loop, for example:
	 * 
	 * TableReader tr = new TableReader(filename)
	 * tr.open();
	 * while (tr.next()){
	 * 		...
	 * 
	 * @return true if the next line is not null.
	 * @throws IOException
	 */
	public boolean next() throws IOException{
		String line;
		boolean result = (line  = bf.readLine()) != null;
		if (result){
			String[] cells = line.split(this.regex);
			if (cells.length < this.headers.size()){
				if (this.ignoreTrailingBlanks){
					String newline = line;
					for (int i = cells.length; i < this.headers.size(); i++) 
						newline += this.regex;
					cells = newline.split(this.regex,headers.size());
				}else throw new IndexOutOfBoundsException();
			}else if(cells.length > this.headers.size() && !this.truncatingTrailingData) throw new IndexOutOfBoundsException();
			
			this.cr= new HashMap<String, String>();
			for (int i = 0; i<headers.size(); i++) this.cr.put(headers.get(i), cells[i]);
		}
		return result;
	}
	
	/**
	 * Returns the current record as a HashMap<String,String>. 
	 * Fields specified in the header line can be accessed via
	 * the get(String attribute_name) function. The field name
	 * specified must match exactly the name of the field header
	 * specified at the top of the file. 
	 * 
	 * Example:
	 * 
	 * To get the value for the field "street_name", use
	 * TableReader tr = new TableReader(filename)
	 * tr.open();
	 * while (tr.next()){
	 * 		String streetName = tr.current().get("street_name");
	 * 		...
	 * 
	 * @return
	 */
	public HashMap<String,String> current(){
		return this.cr;
	}
	
	/**
	 * Checks the headers in the table against a user-supplied
	 * list of required headers. Returns true IFF all supplied
	 * headers are present. Does NOT check for consistency in
	 * the column data (ie, if a String is stored where a Double
	 * is expected, this method WILL NOT catch this problem).
	 * 
	 * @param 
	 * 		fields A List<String> of field headers to check.
	 * @return
	 */
	public boolean checkHeaders(List<String> fields){
		
		for (String s : fields) if(!this.headers.contains(s)) return false;
		
		return true;
		
	}
	
	public void SetBufferSize(int i){
		this.BUFFER_SIZE = i;
	}
	
	
}
