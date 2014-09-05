package playground.ikaddoura.noise;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;

public class LKCsvWriterId2DoubleMaps {
	
	private String fileName = null;
	private int columns = 0;
	List<String> headers = new ArrayList<String>();
	List<HashMap<Id,Double>> values = new ArrayList<HashMap<Id,Double>>();

	public LKCsvWriterId2DoubleMaps (String fileName , int columns , List<String> headers , List<HashMap<Id,Double>> values) {
		this.fileName = fileName;
		this.columns = columns;
		this.headers = headers;
		this.values = values;
	}
	
	public void write () {
		
		File file = new File(fileName);
			
		// For all maps, the number of keys should be the same
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(headers.get(0));
			for(int i = 1 ; i < columns ; i++) {
				bw.write(";"+headers.get(i));
			}
			bw.newLine();
			
//			System.out.println(values);
			for(Id id : values.get(0).keySet()) {
				bw.write(id.toString());
				for(int i = 0 ; i < (columns-1) ; i++) {
					bw.write(";"+values.get(i).get(id));
				}
				bw.newLine();
			}
				
			bw.close();
			//	log.info("Output written to " + fileName);
				
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
