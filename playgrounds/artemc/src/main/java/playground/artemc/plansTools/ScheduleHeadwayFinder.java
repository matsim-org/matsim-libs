package playground.artemc.plansTools;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class ScheduleHeadwayFinder {

	private Double previousDepartureTime = 0.0;		
	private Double nextDepartureTime = 0.0;
	private Double headway = 0.0;
	private Double departureOffset;
	private Double timeInSeconds = 0.0;
	
	private Map<Id<Departure>,Departure> departures = new HashMap();
	
	/**
	 * @param args
	 * @throws ParseException 
	 */
	
	public String findHeadway(String time, TransitLine line, TransitRoute route, TransitStopFacility stop, TransitSchedule schedule) throws ParseException {
		
		//System.out.println("Checking the headway...");
		
		timeInSeconds = TimeTools.timeStringToSeconds(time);

		previousDepartureTime = 0.0;		
		nextDepartureTime = 0.0;
		headway = 0.0;
		departureOffset = schedule.getTransitLines().get(line.getId()).getRoutes().get(route.getId()).getStop(stop).getDepartureOffset();
		
		departures = schedule.getTransitLines().get(line.getId()).getRoutes().get(route.getId()).getDepartures();
		SortedMap<Double,Id> departureTimes = new TreeMap<Double, Id>();
		for(Id dep:departures.keySet()){
			departureTimes.put(departures.get(dep).getDepartureTime(), dep);
		}
		
		
		for(Double t:departureTimes.keySet()){
			//System.out.println("Route: "+route.getId().toString()+"   t: "+t+"   departureOffset:"+departureOffset+"   timeInSeconds:"+timeInSeconds);
			if((t+departureOffset)<timeInSeconds){
				previousDepartureTime = t;
			}
			else{
				nextDepartureTime = t;
				break;
			}
		}
		
		headway = nextDepartureTime - previousDepartureTime;
		if(headway>1500.0){
			headway=600.0;
		}
		//System.out.println("Headway: "+headway+" = "+nextDepartureTime+" - "+previousDepartureTime);
		return TimeTools.secondsTotimeString(headway);
	}

}
