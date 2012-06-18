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
	
	private static final Logger log = Logger.getLogger(TableReader.class);
	
	public TableReader(String filename, String regex){
		this.filename = filename;
		this.regex = regex;
	}
	public TableReader(String filename){
		this.filename = filename;
		this.regex = null;
	}
	
	public void open() throws FileNotFoundException, IOException{
		bf = new BufferedReader(new FileReader(this.filename));
		String line = bf.readLine();
		if (regex == null) this.TryRegex();
		headers = Arrays.asList(line.split(this.regex));
	}
	
	public void close() throws IOException{
		this.bf.close();
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
			cells = line.split("\t");
			if (cells.length > 1) {
				this.regex = "\\s+";
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
	
	public boolean next() throws IOException{
		String line;
		boolean result = (line  = bf.readLine()) != null;
		if (result){
			String[] cells = line.split(this.regex);
			this.cr= new HashMap<String, String>();
			for (int i = 0; i<headers.size(); i++) this.cr.put(headers.get(i), cells[i]);
		}
		return result;
	}
	
	/**
	 * Returns the current record as a HashMap.
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
	 * the column data. 
	 * 
	 * @param fields: A List<String> of field headers to check.
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
