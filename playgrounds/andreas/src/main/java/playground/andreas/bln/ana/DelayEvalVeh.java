package playground.andreas.bln.ana;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.xml.sax.SAXException;

import de.schlichtherle.io.File;
import de.schlichtherle.io.FileWriter;

public class DelayEvalVeh {
	
	private final static Logger log = Logger.getLogger(DelayEvalVeh.class);

	
	public void readEvents(String filename){
		EventsManagerImpl events = new EventsManagerImpl();
		DelayHandler handler = new DelayHandler();
		
		
//		handler.addVehToEvaluate("veh_5");
//		handler.addVehToEvaluate("veh_6");
//		handler.addVehToEvaluate("veh_7");
		
		handler.addVehToEvaluate("veh_13");
		handler.addVehToEvaluate("veh_14");
		handler.addVehToEvaluate("veh_15");
		handler.addVehToEvaluate("veh_16");
		handler.addVehToEvaluate("veh_17");
		handler.addVehToEvaluate("veh_18");
		handler.addVehToEvaluate("veh_8");
		handler.addVehToEvaluate("veh_9");
		
		handler.createGeoMap("d:/Berlin/BVG/berlin-bvg09/pt/m4_demand/transitSchedule_kl.xml", 
				"d:/Berlin/BVG/berlin-bvg09/pt/m4_demand/network_kl.xml", "M44  ");
		
		events.addHandler(handler);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		try {
			reader.parse(filename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		handler.writeXTics();
		
	}
	
	public static void main(String[] args) {
		
		DelayEvalVeh delayEval = new DelayEvalVeh();
		delayEval.readEvents("E:/_out/0.events.xml.gz");

		


	}
	
	static class DelayHandler implements VehicleDepartsAtFacilityEventHandler, VehicleArrivesAtFacilityEventHandler{
		
		private final static Logger dhLog = Logger.getLogger(DelayHandler.class);
		private HashMap<String, BufferedWriter> writerMap = new HashMap<String, BufferedWriter>();
		HashMap<Id,Double> stopIdDistanceMap;
		
		public DelayHandler(){
			
		}
		
		public void writeXTics() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("set ytics (");
			boolean first = true;
			for (Entry<Id, Double> entry : this.stopIdDistanceMap.entrySet()) {
				
				if(first){
					first = false;
				} else {
					buffer.append(", ");					
				}
				
				buffer.append("\"");
				buffer.append(entry.getKey().toString());
				buffer.append("\" ");
				buffer.append(entry.getValue());
				
			}
			buffer.append(")");
			System.out.println(buffer.toString());
			
		}

		public void addVehToEvaluate(String veh) {
			try {
				this.writerMap.put(veh, new BufferedWriter(new FileWriter(new File("E:/_out/" + veh))));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		void createGeoMap(String transitScheduleFile, String networkFile, String line) {
			
			ScenarioImpl scenario = new ScenarioImpl();
			MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scenario);
			matsimNetReader.readFile(networkFile);
			
			TransitSchedule transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
			TransitScheduleReaderV1 transitScheduleReaderV1 = new TransitScheduleReaderV1(transitSchedule, scenario.getNetwork());
			
			HashMap<Id, Double> resultingStopDistanceMap = new HashMap<Id, Double>();
			
			try {
				transitScheduleReaderV1.readFile(transitScheduleFile);
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			for (Entry<Id, TransitLine> entry : transitSchedule.getTransitLines().entrySet()) {
				
				if(entry.getKey().toString().equalsIgnoreCase(line)){
					
					// Annahme: Es gibt maximal zwei Ketten, z.B. Hin- und Rueckrichtung
					
					LinkedList<Id> chain1 = new LinkedList<Id>();
					LinkedList<Id> chain2 = new LinkedList<Id>();
										
					for (TransitRoute route : entry.getValue().getRoutes().values()) {
						
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
								dhLog.warn("There is a third chain. Don't know what to do.");
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

					
					
					
					
					
				} 
			}
		
			this.stopIdDistanceMap = resultingStopDistanceMap;
		}

		private LinkedList<Id> reverseOrder(LinkedList<Id> chain2) {
			LinkedList<Id> newList = new LinkedList<Id>();
			for (Id id : chain2) {
				newList.addFirst(id);
			}
			return newList;
		}

		@Override
		public void reset(int iteration) {
			dhLog.warn("Should not happen, since scenario runs one iteration only.");			
		}
		
		@Override
		public void handleEvent(VehicleArrivesAtFacilityEvent event) {
			
			if(this.writerMap.containsKey(event.getVehicleId().toString())){

				BufferedWriter writer = this.writerMap.get(event.getVehicleId().toString());

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
			
			if(this.writerMap.containsKey(event.getVehicleId().toString())){

				BufferedWriter writer = this.writerMap.get(event.getVehicleId().toString());

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
		
		private double calculateDistanceBetweenCoords(Coord one, Coord two){
			return Math.sqrt(Math.pow(one.getX() - two.getX(), 2) + Math.pow(one.getY() - two.getY(), 2));
		}
				
	}


}


