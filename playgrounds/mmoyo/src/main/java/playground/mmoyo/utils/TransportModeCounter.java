package playground.mmoyo.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;

public class TransportModeCounter {

	/**
	 * Reads a TransitSchedule and counts the number of transitLines and transitRoutes using a TransportMode
	 */
	public void count(TransitSchedule transitSchedule){
		Map <String, List<Tuple<Id, Id>>>  modeMap = new TreeMap <String,List<Tuple<Id, Id>>> ();  //<Id line, Id route>

		for (TransitLine transitLine : transitSchedule.getTransitLines().values()){
			for (TransitRoute transitRoute : transitLine.getRoutes().values()){
				String mode = transitRoute.getTransportMode();

				if (!modeMap.containsKey(mode)){
					modeMap.put(mode, new ArrayList<Tuple<Id, Id>>());
				}
				
				Id lineId = transitLine.getId();
				Id routeId = transitRoute.getId();
				Tuple<Id, Id> tuple = new Tuple<Id, Id>(lineId, routeId);
				modeMap.get(mode).add(tuple);
			}
		}

		for(Map.Entry <String,List<Tuple<Id, Id>>> entry: modeMap.entrySet() ){
			System.out.println(entry.getKey() + ": " + entry.getValue().size());
			for (Tuple<Id, Id> tuple : entry.getValue()){
				System.out.println("   	   TransitLine: " + tuple.getFirst().toString() + " 	TransitRoute: " + tuple.getSecond().toString());
			}
		}
	}

	public static void main(String[] args) {
		String configFile = null;

		if (args.length>0){
			configFile = args[0];
		}else{
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
		}

		ScenarioImpl scenarioImpl = new TransScenarioLoader().loadScenarioWithTrSchedule(configFile);
		new TransportModeCounter().count(scenarioImpl.getTransitSchedule());
	}

}
