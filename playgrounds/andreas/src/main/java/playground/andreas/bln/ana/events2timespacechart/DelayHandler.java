package playground.andreas.bln.ana.events2timespacechart;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;

class DelayHandler implements VehicleDepartsAtFacilityEventHandler, VehicleArrivesAtFacilityEventHandler{
	
	private final static Logger log = Logger.getLogger(DelayHandler.class);
	private HashMap<Id, BufferedWriter> writerMap = new HashMap<Id, BufferedWriter>();
	HashMap<Id,Double> stopIdDistanceMap;
	
	private final String outputDir;
	private final Id line;
	
	public DelayHandler(String outputDir, String line){
		this.line = new IdImpl(line);
		this.outputDir = outputDir + line.trim() + "/";
		new File(this.outputDir).mkdir();
	}
	
	void intialize(TransitSchedule transitSchedule) {
		
		// Put all its vehicles to the writer map			
		for (TransitRoute transitRoute : transitSchedule.getTransitLines().get(this.line).getRoutes().values()) {
			for (Departure departure : transitRoute.getDepartures().values()) {
				this.addVehToEvaluate(departure.getVehicleId());
			}
		}
		
		HashMap<Id, Double> resultingStopDistanceMap = new HashMap<Id, Double>();			
						
		// Annahme: Es gibt maximal zwei Ketten, z.B. Hin- und Rueckrichtung

		LinkedList<Id> chain1 = new LinkedList<Id>();
		LinkedList<Id> chain2 = new LinkedList<Id>();

		for (TransitRoute route : transitSchedule.getTransitLines().get(this.line).getRoutes().values()) {

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
			chain2 = reverseOrder(chain2);
		} else 	if(distanceLastFirst <= Math.min(distanceFirstLast, Math.min(distanceFirstFirst, distanceLastLast))){
			chain1 = reverseOrder(chain1);
		} else if(distanceFirstFirst <= Math.min(distanceLastLast, Math.min(distanceFirstLast, distanceLastFirst))){
			// Do nothing
		} else if(distanceLastLast <= Math.min(distanceFirstFirst, Math.min(distanceFirstLast, distanceLastFirst))){
			chain1 = reverseOrder(chain1);
			chain2 = reverseOrder(chain2);
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

		this.stopIdDistanceMap = resultingStopDistanceMap;
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		
		if(this.writerMap.containsKey(event.getVehicleId())){

			BufferedWriter writer = this.writerMap.get(event.getVehicleId());

			StringBuffer buffer = new StringBuffer();
			buffer.append(event.getTime()); buffer.append(", ");
			buffer.append(Time.writeTime(event.getTime())); buffer.append(", ");
			buffer.append(this.stopIdDistanceMap.get(event.getFacilityId())); buffer.append(", ");
			buffer.append(event.getFacilityId()); buffer.append(", ");
			buffer.append(event.getDelay()); buffer.append(", ");

			try {
				writer.write(buffer.toString());
				writer.newLine();
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		
		if(this.writerMap.containsKey(event.getVehicleId())){

			BufferedWriter writer = this.writerMap.get(event.getVehicleId());

			StringBuffer buffer = new StringBuffer();
			buffer.append(event.getTime()); buffer.append(", ");
			buffer.append(Time.writeTime(event.getTime())); buffer.append(", ");
			buffer.append(this.stopIdDistanceMap.get(event.getFacilityId())); buffer.append(", ");
			buffer.append(event.getFacilityId()); buffer.append(", ");
			buffer.append(event.getDelay()); buffer.append(", ");

			try {
				writer.write(buffer.toString());
				writer.newLine();
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	@Override
	public void reset(int iteration) {
		log.error("Should not happen, since scenario runs one iteration only.");			
	}

	public void addVehToEvaluate(Id veh) {
		try {
			this.writerMap.put(veh, new BufferedWriter(new FileWriter(new File(this.outputDir + veh.toString()))));
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	private double calculateDistanceBetweenCoords(Coord one, Coord two){
		return Math.sqrt(Math.pow(one.getX() - two.getX(), 2) + Math.pow(one.getY() - two.getY(), 2));
	}

	private LinkedList<Id> reverseOrder(LinkedList<Id> chain2) {
		LinkedList<Id> newList = new LinkedList<Id>();
		for (Id id : chain2) {
			newList.addFirst(id);
		}
		return newList;
	}
	
	protected void writeGnuPlot() {
		GnuFileWriter gnuFileWriter = new GnuFileWriter(this.outputDir);
		gnuFileWriter.write(this.stopIdDistanceMap, this.line.toString());		
	}
			
}