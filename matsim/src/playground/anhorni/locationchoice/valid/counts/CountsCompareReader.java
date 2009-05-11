package playground.anhorni.locationchoice.valid.counts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;

public class CountsCompareReader {
	
	private final static Logger log = Logger.getLogger(CountsCompareReader.class);
	String countsCompareFile = "input/counts/countscompare.txt";
	private Stations stations;
	
	public CountsCompareReader(Stations stations) {
		this.stations = stations;
	}
	
	public void read() {
		try {
			FileReader fileReader = new FileReader(countsCompareFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);	
			
			String curr_line = bufferedReader.readLine(); // Skip header
			while ((curr_line = bufferedReader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				
				String linkId = entries[0].trim();
				int hour = Integer.parseInt(entries[1].trim()) - 1;
				double matsimVol = Double.parseDouble( entries[2].trim());
				
				if (!stations.addSimValforLinkId("ivtch", linkId, hour, matsimVol)) {
					log.error("Error with: " + linkId + "\thour:" + hour);
				}
			}	
			bufferedReader.close();
			fileReader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
}
