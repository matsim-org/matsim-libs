package playground.dhosse.gap.scenario.carsharing;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.scenario.carsharing.io.CsSink;

public class CreateCsStationsFromParkingSpaces {

	public static void main(String[] args) {
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(Global.runInputDir + "merged-networkV2_20150929.xml");
		new NetworkCleaner().run(scenario.getNetwork());
		
		CreateCsStationsFromParkingSpaces.run(Global.networkDataDir + "garmisch-latest.osm", scenario);

	}
	
	public static void run(String file, Scenario scenario){
		
		Map<String, String> osmToMatsimTypeMap = new HashMap<>();
		osmToMatsimTypeMap.put("parking", "foo");
		
		Set<String> keys = new HashSet<>();
		keys.add("amenity");
		
		try {
			
			File f = new File(file);
			
			if(!f.exists()){
				
				throw new FileNotFoundException("Coud not find " + file);
				
			}
			
			CsSink sink = new CsSink(scenario, osmToMatsimTypeMap, keys);
			XmlReader reader = new XmlReader(f, false, CompressionMethod.None);
			reader.setSink(sink);
			reader.run();
			
//			this.facilities = sink.getFacilities();
//			this.facilityAttributes = sink.getFacilityAttributes();
			
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
			
		}
		
	}

}
