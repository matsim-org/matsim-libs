package playground.alexander;

import java.io.BufferedReader;
import java.io.IOException;

import org.matsim.utils.StringUtils;
import org.matsim.utils.io.IOUtils;

public class Reader {

	private BufferedReader infile = null;

	public void readfile(String filename) {
		try {
			this.infile = IOUtils.getBufferedReader(filename);
			String line = this.infile.readLine();
			while ( (line = this.infile.readLine()) != null) {
				parseLine(line);
			}
			this.infile.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void parseLine(String line) {
		String[] result = StringUtils.explode(line, '\t');
		if(Integer.parseInt(result[0]) == 1){
			
		}
		if(Integer.parseInt(result[1] == 1)){
			
		}
		if(Integer.parseInt(result[2] == 1)){
			
		}
		
		
		
//		if (result.length == 7) {
//			createEvent(this.events, Integer.parseInt(result[0]),	// time
//									result[1],		// vehID
//									Integer.parseInt(result[2]),		// legNumber
//									result[3],		// linkID
//									Integer.parseInt(result[4]),		// nodeID
//									Integer.parseInt(result[5]),		// flag
//									result[6], 0,"");		// description
//		}
	}
}
	
	

