package playground.anhorni.counts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;


public class CountsReaderYear {
	
	private final static Logger log = Logger.getLogger(CountsReaderYear.class);
	TreeMap<String, Vector<RawCount>> rawCounts = new TreeMap<String, Vector<RawCount>>();
	
	public void read(String datasetsfile) {
		List<String> paths = new Vector<String>();
		try {
			FileReader fileReader = new FileReader(datasetsfile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);	
			
			String curr_line;			
			while ((curr_line = bufferedReader.readLine()) != null) {
				String dataset = curr_line;
				paths.add(dataset);
			}	
			bufferedReader.close();
			fileReader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		this.readFiles(paths);
	}
	
	private void readFiles(final List<String> paths) {
		try {
			
			for (int i = 0; i < paths.size(); i++) {
				FileReader fileReader = new FileReader(paths.get(i));
				BufferedReader bufferedReader = new BufferedReader(fileReader);				
				log.info("Reading: " + paths.get(i));
				
				String curr_line = bufferedReader.readLine(); // Skip header
				
				int counter = 0;
				int nextMsg = 1;				
				while ((curr_line = bufferedReader.readLine()) != null) {
					counter++;
					if (counter % nextMsg == 0) {
						nextMsg *= 2;
						log.info(" line # " + counter);
					}
					String[] entries = curr_line.split("\t", -1);											
					String dataset = entries[0].trim();
					String nr = entries[1].trim();
					String year = entries[3].trim();
					String month = entries[4].trim();
					String day = entries[5].trim();
					String hour = entries[6].trim();					
					String vol1 = entries[7].trim();
					String vol2 = entries[8].trim();
					
					if (nr.length() == 1) {
						nr = "0" + nr;
					}
					if (month.length() == 1) {
						month = "0" + month;
					}
					if (day.length() == 1) {
						day = "0" + day;
					}
					if (hour.length() == 1) {
						hour = "0" + hour;
					}
					String id = dataset+nr;
					
					if (vol2.length() == 0) {
						vol2 = "-1";
					}
					if (vol1.length() == 0) {
						vol1 = "-1";
					}
					
					//log.info(id + " " + year + " " +  month + " " +  day + " " +  hour);

					RawCount rawCount = new RawCount(id, year, month, day, hour, vol1, vol2);
					
					//if (Integer.parseInt(vol1) > -0.5 && Integer.parseInt(vol2) > 0.5) {
						if (rawCounts.get(id) == null) {
							rawCounts.put(id, new Vector<RawCount>());
						}
						rawCounts.get(id).add(rawCount);	
					//}
				}
				log.info("reading finished -------------------");
				bufferedReader.close();
				fileReader.close();
			}
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	public TreeMap<String, Vector<RawCount>> getRawCounts() {
		return rawCounts;
	}
}
