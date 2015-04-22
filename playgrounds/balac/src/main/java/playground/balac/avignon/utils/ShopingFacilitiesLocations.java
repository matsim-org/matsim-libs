package playground.balac.avignon.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

public class ShopingFacilitiesLocations {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		//MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		//networkReader.readFile(args[0]);
		new FacilitiesReaderMatsimV1(scenario).readFile(args[0]);
		Map<String, String> facilityToLinkMap = new HashMap<String, String>();
		BufferedReader reader = IOUtils.getBufferedReader(args[1]);
		
		
		BufferedWriter output = new BufferedWriter(new FileWriter(new File("C:/Users/balacm/Desktop/Shopping_facilities.txt")));

		String s = reader.readLine();
		s = reader.readLine();

		while (s != null) {
			
	    	String[] arr = s.split("\t");
	    	
	    	facilityToLinkMap.put(arr[0], arr[1]);
	    	s = reader.readLine();
			
		}
		output.write("facilityId coordX coordY linkId capacity");
		output.newLine();
		for (ActivityFacility f : scenario.getActivityFacilities().getFacilitiesForActivityType("shopping").values()) {
			
			output.write(f.getId().toString() + " ");
			output.write(f.getCoord().getX() + " " + f.getCoord().getY() + " ");
			output.write(facilityToLinkMap.get(f.getId().toString()) + " ");
			output.write(Double.toString(f.getActivityOptions().get("shopping").getCapacity()));

			output.newLine();
			
		}
		
		output.flush();
		output.close();

	}

}
