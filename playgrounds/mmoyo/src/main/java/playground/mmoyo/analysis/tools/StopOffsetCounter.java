package playground.mmoyo.analysis.tools;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import playground.mmoyo.utils.DataLoader;

public class StopOffsetCounter {
	
	private void countDifferentOffsets(ScenarioImpl scenario){
		int departures=0;
		int differentOffset = 0;
		double offsetDifSume =0;
		
		for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()){
			for (TransitRoute transitRoute :transitLine.getRoutes().values()){
				for (TransitRouteStop stop : transitRoute.getStops()){
					departures++;
					if (stop.getArrivalOffset() != stop.getDepartureOffset()){
						differentOffset++;
						double offsetDif = stop.getDepartureOffset() -stop.getArrivalOffset();
						System.out.println("offsetDif:"  + offsetDif);
						offsetDifSume += offsetDif;
					}
				}
			}
		}
		System.out.println("departures        :" + departures);
		System.out.println("different Offsets :" + differentOffset); 
		System.out.println("offset Diff Sume  :" + offsetDifSume);
	}
	
	public static void main(String[] args) {
		String configFile= null;
		if (args.length>0) {
			configFile = args[0];	
		}else{
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";	
		}
		ScenarioImpl scenario = new DataLoader().loadScenarioWithTrSchedule(configFile);
		new StopOffsetCounter().countDifferentOffsets(scenario);
	}
	
}
