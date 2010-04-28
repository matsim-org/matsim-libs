package playground.andreas.bln.ana.events2counts;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;

public class TransitSchedule2MainLine {

	private final static Logger log = Logger.getLogger(TransitSchedule2MainLine.class);
	
	public static Map<Id, List<List<Id>>> createMainLinesFromTransitSchedule(TransitSchedule transitSchedule) {
		
		Map<Id, List<List<Id>>> line2MainLinesMap = new HashMap<Id, List<List<Id>>>();
		
		for (Entry<Id, TransitLine> transitLineEntry : transitSchedule.getTransitLines().entrySet()) {
			// Annahme: Es gibt maximal zwei Ketten, z.B. Hin- und Rueckrichtung, andere Routen sind Untermengen

			HashMap<Id, Double> resultingStopDistanceMap = new HashMap<Id, Double>();	
			
			LinkedList<Id> chain1 = new LinkedList<Id>();
			LinkedList<Id> chain2 = new LinkedList<Id>();
			
		
			for (TransitRoute route : transitLineEntry.getValue().getRoutes().values()) {

				LinkedList<Id> chainToUse = null;

				// Identify chain
				for (TransitRouteStop stop : route.getStops()) {
					if(chain1.contains(stop.getStopFacility().getId())){
						chainToUse = chain1;
						break;
					}						
					if(chain2.contains(stop.getStopFacility().getId())){
						chainToUse = chain2;
						break;
					}
				}

				if(chainToUse == null){
					// no chain found, so claim one
					if(chain1.size() == 0){
						chainToUse = chain1;
					} else if(chain2.size() == 0){
						chainToUse = chain2;								
					} else {
						log.error("There is a third chain. Don't know what to do.");
					}
					for (TransitRouteStop stop : route.getStops()) {
						chainToUse.add(stop.getStopFacility().getId());									
					}
				} else {

					boolean oneStopWasAlreadyKownToChain = false; // so append the rest of the stops, instead of adding them first
					int numberOfStopsAlreadyAddedAtBeginningOfChain = 0;
					
					for (TransitRouteStop stop : route.getStops()) {						
						
						if(chainToUse.contains(stop.getStopFacility().getId())){
							oneStopWasAlreadyKownToChain = true;
							continue;
						} 
						
						if(!oneStopWasAlreadyKownToChain){
							chainToUse.add(numberOfStopsAlreadyAddedAtBeginningOfChain, stop.getStopFacility().getId());
							numberOfStopsAlreadyAddedAtBeginningOfChain++;
							continue;
						} else if(oneStopWasAlreadyKownToChain){
							chainToUse.add(stop.getStopFacility().getId());
							continue;
						}						
						
					}	
				}		
			}
				
			// find point zero and sort chains accordingly				
			double distanceFirstFirst = calculateDistanceBetweenCoords(transitSchedule.getFacilities().get(chain1.getFirst()).getCoord(), transitSchedule.getFacilities().get(chain2.getFirst()).getCoord());
			double distanceLastLast = calculateDistanceBetweenCoords(transitSchedule.getFacilities().get(chain1.getLast()).getCoord(), transitSchedule.getFacilities().get(chain2.getLast()).getCoord());
			double distanceFirstLast = calculateDistanceBetweenCoords(transitSchedule.getFacilities().get(chain1.getFirst()).getCoord(), transitSchedule.getFacilities().get(chain2.getLast()).getCoord());
			double distanceLastFirst = calculateDistanceBetweenCoords(transitSchedule.getFacilities().get(chain1.getLast()).getCoord(), transitSchedule.getFacilities().get(chain2.getFirst()).getCoord());

			if(distanceFirstLast <= Math.min(distanceLastFirst, Math.min(distanceFirstFirst, distanceLastLast))){
//				chain2 = reverseOrder(chain2);
			} else 	if(distanceLastFirst <= Math.min(distanceFirstLast, Math.min(distanceFirstFirst, distanceLastLast))){
//				chain1 = reverseOrder(chain1);
			} else if(distanceFirstFirst <= Math.min(distanceLastLast, Math.min(distanceFirstLast, distanceLastFirst))){
				// Do nothing
			} else if(distanceLastLast <= Math.min(distanceFirstFirst, Math.min(distanceFirstLast, distanceLastFirst))){
//				chain1 = reverseOrder(chain1);
//				chain2 = reverseOrder(chain2);
			}					

			// Get distance from the end				
			double distanceFromStart = 0.0;
			Coord lastCoord = transitSchedule.getFacilities().get(chain1.getFirst()).getCoord();
			
			for (int i = 0; i < chain1.size(); i++) {
				distanceFromStart += calculateDistanceBetweenCoords(lastCoord, transitSchedule.getFacilities().get(chain1.get(i)).getCoord());
				resultingStopDistanceMap.put(chain1.get(i), Double.valueOf(distanceFromStart));
				lastCoord = transitSchedule.getFacilities().get(chain1.get(i)).getCoord();
			}

			distanceFromStart = 0.0;
			lastCoord = transitSchedule.getFacilities().get(chain2.getFirst()).getCoord();

			for (int i = 0; i < chain2.size(); i++) {
				distanceFromStart += calculateDistanceBetweenCoords(lastCoord, transitSchedule.getFacilities().get(chain2.get(i)).getCoord());
				resultingStopDistanceMap.put(chain2.get(i), Double.valueOf(distanceFromStart));
				lastCoord = transitSchedule.getFacilities().get(chain2.get(i)).getCoord();
			}			

			List<List<Id>> chainLists = new LinkedList<List<Id>>();
			chainLists.add(chain1);
			chainLists.add(chain2);
//			resultingStopDistanceMap
			line2MainLinesMap.put(transitLineEntry.getKey(), chainLists);
			
		}

		return line2MainLinesMap;
	}
	
	private static double calculateDistanceBetweenCoords(Coord one, Coord two){
		return Math.sqrt(Math.pow(one.getX() - two.getX(), 2) + Math.pow(one.getY() - two.getY(), 2));
	}

	private static LinkedList<Id> reverseOrder(LinkedList<Id> chain2) {
		LinkedList<Id> newList = new LinkedList<Id>();
		for (Id id : chain2) {
			newList.addFirst(id);
		}
		return newList;
	}
}
