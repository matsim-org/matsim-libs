package playground.balac.utils.emissions;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;


public class EventsToTrips {
	
	double centerX = 683217.0; 
	double centerY = 247300.0;	
	
	public EventsToTrips() {
		
		
	}
	
	public void run(String s, String outputFile, String networkFile) throws IOException {
		
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
	    EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
	    EventsToTripsHandler eventsToTripsHandler = new EventsToTripsHandler();
	    
	    MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(networkFile);

    	events.addHandler(eventsToTripsHandler);
    	reader.parse(s);
		final BufferedWriter outLink = IOUtils.getBufferedWriter(outputFile);

    	HashMap<Id<Vehicle>, ArrayList<RouteInfo>> routes = eventsToTripsHandler.getRoutes();
    	int count = 0;
    	int i = 1;
    	for(ArrayList<RouteInfo> ri : routes.values()) {
    		
    		
    		for (RouteInfo routeInfo : ri) {
    			
    			
    			ArrayList<Double> travelTime = routeInfo.getTravelTime();
    			
    			int j = 0;
    			if (routeInfo.getLinks().size() != 0) {
	    			Link l = scenario.getNetwork().getLinks().get(routeInfo.getLinks().get(0));
	    			
	    			DecimalFormat myFormat = new DecimalFormat("0.00");
	    			boolean haveInside = false;
	    			for (Id<Link> linkId : routeInfo.getLinks()) {
						
	    				l = scenario.getNetwork().getLinks().get(linkId);    				
	
	    				if (Math.sqrt(Math.pow(l.getCoord().getX() - centerX, 2) +(Math.pow(l.getCoord().getY() - centerY, 2))) < 6000) {
	    					haveInside = true;
	    					break;
	    				}
	    				
	    			}
					if (haveInside) {
						outLink.write(Integer.toString(i) + ";");
		    			for (Id<Link> linkId : routeInfo.getLinks()) {
						
		    				l = scenario.getNetwork().getLinks().get(linkId);    				
		    				if(Math.sqrt(Math.pow(l.getCoord().getX() - centerX, 2) +(Math.pow(l.getCoord().getY() - centerY, 2))) < 6000)
		    					outLink.write("K" + linkId.toString() + ";" + myFormat.format(l.getLength()/(travelTime.get(j))) + ";" + Double.toString(l.getFreespeed()) + ";" + Double.toString(l.getLength()) + ";");
		    				else
		    					outLink.write("M" + linkId.toString() + ";" + myFormat.format(l.getLength()/(travelTime.get(j))) + ";" + Double.toString(l.getFreespeed()) + ";" + Double.toString(l.getLength()) + ";");

		    				j++;
		    				
		    			}
		    			outLink.newLine();
		    			i++;	
					}
	    			
	    			
    			}
    			else
    				count++;
    		}
    	}
    	outLink.flush();
    	outLink.close();
    	System.out.println(count);
	}
	
	private class EventsToTripsHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler{

		HashMap<Id<Vehicle>, Boolean> inVehicle = new HashMap<Id<Vehicle>, Boolean>();
		HashMap<Id, Double> travelTimes = new HashMap<Id, Double>();
		HashMap<Id<Vehicle>, ArrayList<RouteInfo>> routes = new HashMap<Id<Vehicle>, ArrayList<RouteInfo>>();
		
		public HashMap<Id<Vehicle>, ArrayList<RouteInfo>> getRoutes() {
			
			return this.routes;
		}
		
		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			inVehicle = new HashMap<Id<Vehicle>, Boolean>();
			routes = new HashMap<Id<Vehicle>, ArrayList<RouteInfo>>();
		}

		@Override
		public void handleEvent(PersonLeavesVehicleEvent event) {
			// TODO Auto-generated method stub
			if (!event.getVehicleId().toString().contains("_")) {
				inVehicle.put(event.getVehicleId(), false);
				travelTimes.remove(event.getPersonId());
				
			}
			
		}

		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			// TODO Auto-generated method stub
			if (!event.getVehicleId().toString().contains("_")) {
				inVehicle.put(event.getVehicleId(), true);
				if (this.routes.containsKey(event.getVehicleId())) {
					RouteInfo newRoute = new RouteInfo();
					
					routes.get(event.getVehicleId()).add(newRoute);
					
				}
				else {
					ArrayList<RouteInfo> newPoint = new ArrayList<RouteInfo>();
					RouteInfo newRoute = new RouteInfo();
					newPoint.add(newRoute);
					this.routes.put(event.getVehicleId(), newPoint);
				}
				
			}
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			// TODO Auto-generated method stub
			
			if (inVehicle.containsKey(event.getVehicleId()) && inVehicle.get(event.getVehicleId()) == true)
				if (travelTimes.containsKey(event.getVehicleId())) {
					if (routes.containsKey(event.getVehicleId())) {
						
						routes.get(event.getVehicleId()).get(routes.get(event.getVehicleId()).size() - 1).addNewLink(event.getLinkId(), -travelTimes.get(event.getVehicleId()) + event.getTime());
						travelTimes.remove(event.getVehicleId());
					}
					else {
						System.out.println("this should never happen");
						ArrayList<RouteInfo> newPoint = new ArrayList<RouteInfo>();
						RouteInfo newRoute = new RouteInfo();
						newRoute.addNewLink(event.getLinkId(), travelTimes.get(event.getVehicleId()));
						newPoint.add(newRoute);
						this.routes.put(event.getVehicleId(), newPoint);
					}
				}
					
			
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			// TODO Auto-generated method stub
			if (inVehicle.containsKey(event.getVehicleId()) && inVehicle.get(event.getVehicleId()) == true)
				travelTimes.put(event.getVehicleId(), event.getTime());
		}
		
		
	}
	
	
	public static void main(String[] args) throws IOException {
		
		EventsToTrips etp = new EventsToTrips();
		
		etp.run(args[0], args[1], args[2]);
		
		

	}

}
