package playground.artemc.plansTools;

import java.text.ParseException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class PreviousDepartureFinder {

	/**
	 * @param args
	 * @throws ParseException 
	 */
	public static String findDeparture(String time, TransitLine line, TransitRoute route, TransitStopFacility stop, TransitSchedule schedule) throws ParseException {
		
		Double timeInSeconds = TimeTools.timeStringToSeconds(time);
		
		Double previousDepartureTime = 0.0;		
		Double departureOffset = schedule.getTransitLines().get(line.getId()).getRoutes().get(route.getId()).getStop(stop).getDepartureOffset();
		
		Map<Id<Departure>, Departure> departures = schedule.getTransitLines().get(line.getId()).getRoutes().get(route.getId()).getDepartures();
		SortedMap<Double, Id<Departure>> departureTimes = new TreeMap<Double, Id<Departure>>();
		for(Id dep:departures.keySet()){
			departureTimes.put(departures.get(dep).getDepartureTime(), dep);
		}
		
		for(Double t:departureTimes.keySet()){
			if((t+departureOffset)<timeInSeconds){
				previousDepartureTime = t+departureOffset;
			}
			else{
				break;
			}
		}
		
		return TimeTools.secondsTotimeString(previousDepartureTime);
	}

}
