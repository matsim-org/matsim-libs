package playground.anhorni.locationchoice.preprocess.plans.modifications;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;

public class MZDistanceDistrReader {
	
	private final static Logger log = Logger.getLogger(MZDistanceDistrReader.class);
	private boolean zhScenario = true;
	
	public MZDistanceDistrReader(boolean zhScenario) {
		this.zhScenario = zhScenario;
	}
	
	public void readMZDistributions(String mode, int numberOfLeisureActs, DistanceBins distanceBins) {
		try {
			
			String file = "output/analyzeMz/trips/zh/" + mode + "_leisure_Trips.txt";
			if (!this.zhScenario) {
				file = "output/analyzeMz/trips/ch/" + mode + "_leisure_Trips.txt";
			}
			log.info("Reading distribution: " + file);
			
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			
			String curr_line = bufferedReader.readLine(); // Skip header			
			while ((curr_line = bufferedReader.readLine()) != null) {
								
				String[] entries = curr_line.split("\t", -1);							
				double dist = Double.parseDouble(entries[5].trim());
				
				// do not assign distances of trip back home (code -99)
				int exactType = Integer.parseInt(entries[7].trim());
				if (exactType > 0) {
					distanceBins.addDistance(dist);
				}
			}
		} catch (IOException e) {
				Gbl.errorMsg(e);
		}
		distanceBins.finish(numberOfLeisureActs);
	}

}
