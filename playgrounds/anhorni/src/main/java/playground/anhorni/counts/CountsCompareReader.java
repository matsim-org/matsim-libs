package playground.anhorni.counts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;

public class CountsCompareReader {
	
	private final static Logger log = Logger.getLogger(CountsCompareReader.class);
	String countsCompareFile = "../../matsim/input/counts/countscompare.txt";
	private Stations stations;
	
	public CountsCompareReader(Stations stations) {
		this.stations = stations;
	}
	
	private String readNetworkName() {
		String networkName = null;
		try {
			FileReader fileReader = new FileReader("../../matsim/input/counts/networkName.txt");
			BufferedReader bufferedReader = new BufferedReader(fileReader);	
			
			String curr_line;
			while ((curr_line = bufferedReader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1); 
				networkName = entries[0].trim();
			}	
			bufferedReader.close();
			fileReader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		return networkName;
	}
	
	public void read() {
		
		String networkName = this.readNetworkName();
		
		try {
			FileReader fileReader = new FileReader(countsCompareFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);	
			
			String curr_line = bufferedReader.readLine(); // Skip header
			while ((curr_line = bufferedReader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				
				String linkId = entries[0].trim();
				int hour = Integer.parseInt(entries[1].trim()) - 1;
				String matsimVolString = entries[2].trim().replaceAll(",", "");
				double matsimVol = Double.parseDouble(matsimVolString);
				
				if (!stations.addSimValforLinkId(networkName, linkId, hour, matsimVol)) {
//					log.error("Error with: " + linkId + "\thour:" + hour);
				}
			}	
			bufferedReader.close();
			fileReader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
}
