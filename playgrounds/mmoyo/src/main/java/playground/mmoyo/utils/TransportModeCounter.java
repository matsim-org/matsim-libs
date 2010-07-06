package playground.mmoyo.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;

public class TransportModeCounter {

	/**
	 * Reads a TransitSchedule and counts the number of transitLines and transitRoutes using a TransportMode
	 */
	public void count(TransitSchedule transitSchedule){
		Map <String, List<Tuple<TransitLine, TransitRoute>>>  modeMap = new TreeMap <String,List<Tuple<TransitLine, TransitRoute>>> ();

		for (String mode : new String[] { TransportMode.car, TransportMode.ride, TransportMode.bike, TransportMode.walk, TransportMode.transit_walk, TransportMode.pt }) {
			modeMap.put(mode, new ArrayList<Tuple<TransitLine, TransitRoute>>());
		}

		for (TransitLine transitLine : transitSchedule.getTransitLines().values()){
			for (TransitRoute transitRoute : transitLine.getRoutes().values()){
				//Tuple<TransitLine, TransitRoute> tuple = new Tuple<TransitLine, TransitRoute>(transitLine, transitRoute);
				modeMap.get(transitRoute.getTransportMode()).add(new Tuple<TransitLine, TransitRoute>(transitLine, transitRoute));
			}
		}

		for(Map.Entry <String,List<Tuple<TransitLine, TransitRoute>>> entry: modeMap.entrySet() ){
			System.out.println(entry.getKey() + ": " + entry.getValue().size());
			for (Tuple<TransitLine, TransitRoute> tuple : entry.getValue()){
				System.out.println("   	   TransitLine:" + tuple.getFirst().getId() + " 	TransitRoute:" + tuple.getSecond().getId());
			}
		}
	}

	public static void main(String[] args) {
		String configFile = null;

		if (args.length>0){
			configFile = args[0];
		}else{
			configFile = "../playgrounds/mmoyo/output/comparison/Berlin/16plans/0config_5x_4plans.xml";
		}

		ScenarioImpl scenarioImpl = new TransScenarioLoader().loadScenario(configFile);
		new TransportModeCounter().count(scenarioImpl.getTransitSchedule());
	}

}
