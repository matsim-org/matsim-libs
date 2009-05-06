package playground.anhorni.locationchoice.valid.counts;

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
		List<String> datasets = new Vector<String>();
		try {
			FileReader fileReader = new FileReader(datasetsfile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);	
			
			String curr_line;			
			while ((curr_line = bufferedReader.readLine()) != null) {
				String dataset = curr_line;
				log.info("Reading: " + dataset);
				
				datasets.add(dataset);
			}	
			bufferedReader.close();
			fileReader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		this.readFiles(datasets);
	}
	
	private void readFiles(final List<String> paths) {
		try {
			
			for (int i = 0; i < paths.size(); i++) {
				FileReader fileReader = new FileReader(paths.get(i));
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				
				String curr_line = bufferedReader.readLine(); // Skip header
				while ((curr_line = bufferedReader.readLine()) != null) {
					
					String[] entries = curr_line.split("\t", -1);											
					String dataset = entries[0].trim();
					String nr = entries[1].trim();
					String year = entries[3].trim();
					String month = entries[4].trim();
					String day = entries[5].trim();
					String hour = entries[6].trim();
					String vol1 = entries[7].trim();
					String vol2 = entries[8].trim();
												
					RawCount rawCount = new RawCount(dataset+nr, year, month, day, hour, vol1, vol2);
					
					if (rawCounts.get(dataset + nr) == null) {
						rawCounts.put(dataset + nr, new Vector<RawCount>());
					}
					rawCounts.get(dataset + nr).add(rawCount);					
				}	
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
