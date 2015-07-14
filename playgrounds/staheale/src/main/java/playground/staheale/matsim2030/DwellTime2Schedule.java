package playground.staheale.matsim2030;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class DwellTime2Schedule {
	
	private static Logger log = Logger.getLogger(DwellTime2Schedule.class);

	List<Id<Link>> newRouteLinkIds = new ArrayList<Id<Link>>();
	int nId = 0;
	int newId = 1;
	int newCountId = 1;
	Node fromNode = null;
	double previousOffset = 0;
	double newArrivalOffset = 0;
	List<TransitRouteStop> routeStops = new ArrayList<TransitRouteStop>();
	Set<String> ptMode = new HashSet<String>(Arrays.asList("pt"));
	boolean nodeExisting = false;
	boolean linkExisting = false;
	boolean facilityExisting = false;
	TransitStopFacility existingFac = null;
	int count = 0;
	int countDist0 = 0;

	public static void main(String[] args) {
		DwellTime2Schedule dwellTime2Schedule = new DwellTime2Schedule();
		dwellTime2Schedule.run();
	}

	public void run() {
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().transit().setUseTransit(true);
		sc.getConfig().scenario().setUseVehicles(true);
		Network PTnetwork = sc.getNetwork();
		TransitSchedule PTschedule = sc.getTransitSchedule();
		
		ScenarioImpl scWrite = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scWrite.getConfig().transit().setUseTransit(true);
		scWrite.getConfig().scenario().setUseVehicles(true);
		Network newPTnetwork = scWrite.getNetwork();
		TransitSchedule newPTschedule = scWrite.getTransitSchedule();
		
		TransitScheduleFactory scheduleFactory = newPTschedule.getFactory();
		NetworkFactory factory = newPTnetwork.getFactory();

		//////////////////////////////////////////////////////////////////////
		// read in PTnetwork
		
		log.info("Reading pt network...");	
		MatsimNetworkReader NetworkReader = new MatsimNetworkReader(sc); //run2010baselineDez1/uvek2005network_adjusted.xml.gz
		NetworkReader.readFile("C:/Users/staha/Documents/SimulationInput/run2030baseline/uvek2030network_adjusted.xml.gz"); //run2030baseline/uvek2030network_adjusted.xml.gz
		log.info("Reading pt network...done.");
		log.info("Network contains " +PTnetwork.getLinks().size()+ " links and " +PTnetwork.getNodes().size()+ " nodes.");

		//////////////////////////////////////////////////////////////////////
		// read in PTschedule
		
		log.info("Reading pt schedule...");	
		TransitScheduleReader ScheduleReader = new TransitScheduleReader(sc); //run2010baselineDez1/uvek2005schedule_adjustedCORR.xml.gz
		ScheduleReader.readFile("C:/Users/staha/Documents/SimulationInput/run2030baseline/uvek2030schedule_adjustedCORR.xml.gz"); //sampleLine.xml
		log.info("Reading pt schedule...done."); //run2030baseline/uvek2030schedule_adjustedCORR.xml.gz
		log.info("Schedule contains " +PTschedule.getTransitLines().size()+ " lines.");
		
		//////////////////////////////////////////////////////////////////////
		// create links between every stop point
		
		log.info("Start adjusting network and schedule...");
		for (TransitLine line : PTschedule.getTransitLines().values()){
			TransitLine nLine = scheduleFactory.createTransitLine(line.getId());
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop rStop : route.getStops()) {
					
					//////////////////////////////////////////////////////////////////////
					// case 1: first stop and new route list
					if (route.getStops().get(0).equals(rStop)) {
						TransitStopFacility firstFac = rStop.getStopFacility();
						Coord firstFacCoords = firstFac.getCoord();
						
						// create node and add it to network
						nId += 1;
						Id<Node> newNodeId = Id.create(Integer.toString(nId), Node.class);
						NodeImpl newNode = (NodeImpl) factory.createNode(newNodeId, firstFacCoords);
						for (Node node : newPTnetwork.getNodes().values()) {
							if ((Math.abs(newNode.getCoord().getX()-node.getCoord().getX())
									+Math.abs(newNode.getCoord().getY()-node.getCoord().getY())) < 0.01) {
								nodeExisting = true;
								newNode = (NodeImpl) node;
								newNodeId = node.getId();
								newNode.setCoord(node.getCoord());
								break;
							}
						}
						if (nodeExisting != true) {
							newPTnetwork.addNode(newNode);
						}
						nodeExisting = false;
						
						// create link and add it to network
						Id<Link> nLinkId = Id.create(newNodeId.toString()+newNodeId.toString()+Integer.toString(newId), Link.class);
						LinkImpl nLink = (LinkImpl) factory.createLink(nLinkId, newNode, newNode);
						double dist = 0.001;
						double capLink = 99999.0;
						String linkType = "11";
						double freeSpeed = 30.0;
						nLink.setLength(dist);
						nLink.setCapacity(capLink);
						nLink.setType(linkType);
						nLink.setFreespeed(freeSpeed);
						nLink.setAllowedModes(ptMode);
						for (Link link : newPTnetwork.getLinks().values()) {
							if ((Math.abs(nLink.getFromNode().getCoord().getX()-link.getFromNode().getCoord().getX())
									+Math.abs(nLink.getFromNode().getCoord().getY()-link.getFromNode().getCoord().getY())) < 0.01
									&& (Math.abs(nLink.getToNode().getCoord().getX()-link.getToNode().getCoord().getX())
											+Math.abs(nLink.getToNode().getCoord().getY()-link.getToNode().getCoord().getY())) < 0.01
											&& Math.abs(nLink.getFreespeed()-link.getFreespeed()) < 0.1) {
								linkExisting = true;
								nLink = (LinkImpl) link;
								nLinkId = link.getId();
								break;
							}
						}
						if (linkExisting != true) {
							newPTnetwork.addLink(nLink);
							newId += 1;
						}
						linkExisting = false;
						
						fromNode = newNode;
						
												
						// add link to route
						newRouteLinkIds.add(nLinkId);
						
						// add dwell time of 60s
						double newDepOffset = 0;
						if (route.getStops().get(1).getArrivalOffset()-route.getStops().get(0).getDepartureOffset() <= 60) {
							newDepOffset = rStop.getDepartureOffset() + 30;
						}
						else {
							newDepOffset = rStop.getDepartureOffset() + 60;
						}
						TransitRouteStop newStop = scheduleFactory.createTransitRouteStop(rStop.getStopFacility(),
								rStop.getArrivalOffset(), newDepOffset);
						previousOffset = newStop.getDepartureOffset();
						
						// add stop to route and stop facility to schedule
						// check if facility with same coords and link reference already exists
						for (TransitStopFacility f : newPTschedule.getFacilities().values()) {
							if (f.getCoord().equals(firstFac.getCoord()) && f.getLinkId().equals(nLinkId)) {
								facilityExisting = true;
								existingFac = f;
								break;
							}
						}
						// if not: create and add facility to schedule
						if (facilityExisting != true) {
							Id<TransitStopFacility> newFacId = Id.create(firstFac.getId().toString()+Integer.toString(newCountId), TransitStopFacility.class);
							newCountId += 1;
							TransitStopFacility firstFacnew = scheduleFactory.createTransitStopFacility(newFacId, firstFac.getCoord(), false);
							firstFacnew.setName(firstFac.getName());
							firstFacnew.setLinkId(nLinkId);
							newStop.setStopFacility(firstFacnew);
							newStop.setAwaitDepartureTime(true);
							routeStops.add(newStop);
							newPTschedule.addStopFacility(firstFacnew);
						}
						// otherwise refer to existing facility
						else {
							newStop.setStopFacility(existingFac);
							newStop.setAwaitDepartureTime(true);
							routeStops.add(newStop);
						}
						facilityExisting = false;
					}
					
					//////////////////////////////////////////////////////////////////////
					// case 2: last stop
					else if (route.getStops().get((route.getStops().size()-1)).equals(rStop)) {
						TransitStopFacility lastFac = rStop.getStopFacility();
						Coord lastFacCoords = lastFac.getCoord();
						
						// create new stop
						if (previousOffset >= rStop.getArrivalOffset()) {
							newArrivalOffset = previousOffset + 30;
							double tempDiff = previousOffset-rStop.getArrivalOffset();
							count += 1;
							if (count <= 10) {
								log.warn("last stop of route " +route.getId()+ " gets served later, time difference: " +tempDiff);
							}
							else if (count == 11) {
								log.warn("future occurrences of later arrivals at terminal station are suppressed.");
							}
						}
						else {
							newArrivalOffset = rStop.getArrivalOffset();
						}
						double newDepOffset = newArrivalOffset + 60;
						TransitRouteStop newStop = scheduleFactory.createTransitRouteStop(rStop.getStopFacility(),
								newArrivalOffset, newDepOffset);
						
						// create node and add it to network
						nId += 1;
						Id<Node> newNodeId = Id.create(Integer.toString(nId), Node.class);
						NodeImpl newNode = (NodeImpl) factory.createNode(newNodeId, lastFacCoords);
						for (Node node : newPTnetwork.getNodes().values()) {
							if ((Math.abs(newNode.getCoord().getX()-node.getCoord().getX())
									+Math.abs(newNode.getCoord().getY()-node.getCoord().getY())) < 0.01) {
								nodeExisting = true;
								newNode = (NodeImpl) node;
								newNodeId = node.getId();
								newNode.setCoord(node.getCoord());
								break;
							}
						}
						if (nodeExisting != true) {
							newPTnetwork.addNode(newNode);
						}
						nodeExisting = false;
						
						// create link and add it to network
						Id<Link> nLinkId = Id.create(fromNode.getId().toString()+newNodeId.toString()+Integer.toString(newId), Link.class);
						LinkImpl nLink = (LinkImpl) factory.createLink(nLinkId, fromNode, newNode);
						double dist = Math.round(CoordUtils.calcDistance(fromNode.getCoord(), newNode.getCoord()));
						if (dist == 0) {
							countDist0 += 1;
							dist = 0.01;
						}
						double capLink = 99999.0;
						String linkType = "11";
						// reduce link travel time by leaveLinkEvent time of 1s
						double freeSpeed = 0;
						freeSpeed = dist/(newArrivalOffset-previousOffset-1);
						if (freeSpeed <= 0) {
							log.warn("negative speed!");
						}
						nLink.setLength(dist);
						nLink.setCapacity(capLink);
						nLink.setType(linkType);
						nLink.setFreespeed(freeSpeed);
						nLink.setAllowedModes(ptMode);
						for (Link link : newPTnetwork.getLinks().values()) {
							if ((Math.abs(nLink.getFromNode().getCoord().getX()-link.getFromNode().getCoord().getX())
									+Math.abs(nLink.getFromNode().getCoord().getY()-link.getFromNode().getCoord().getY())) < 0.01
									&& (Math.abs(nLink.getToNode().getCoord().getX()-link.getToNode().getCoord().getX())
											+Math.abs(nLink.getToNode().getCoord().getY()-link.getToNode().getCoord().getY())) < 0.01
											&& Math.abs(nLink.getFreespeed()-link.getFreespeed()) < 0.1) {
								linkExisting = true;
								nLink = (LinkImpl) link;
								nLinkId = link.getId();
								break;
							}
						}
						if (linkExisting != true) {
							newPTnetwork.addLink(nLink);
							newId += 1;
						}
						linkExisting = false;
						
						fromNode = newNode;
						previousOffset = newStop.getDepartureOffset();
						
						// add link to route
						newRouteLinkIds.add(nLinkId);
						
						// add stop to route and stop facility to schedule
						// check if facility with same coords and link reference already exists
						for (TransitStopFacility f : newPTschedule.getFacilities().values()) {
							if (f.getCoord().equals(lastFac.getCoord()) && f.getLinkId().equals(nLinkId)) {
								facilityExisting = true;
								existingFac = f;
								break;
							}
						}
						// if not: create and add facility to schedule
						if (facilityExisting != true) {
							Id<TransitStopFacility> newFacId = Id.create(lastFac.getId().toString()+Integer.toString(newCountId), TransitStopFacility.class);
							newCountId += 1;
							TransitStopFacility lastFacnew = scheduleFactory.createTransitStopFacility(newFacId, lastFac.getCoord(), false);
							lastFacnew.setName(lastFac.getName());
							lastFacnew.setLinkId(nLinkId);
							newStop.setStopFacility(lastFacnew);
							newStop.setAwaitDepartureTime(true);
							routeStops.add(newStop);
							newPTschedule.addStopFacility(lastFacnew);
						}
						// otherwise refer to existing facility
						else {
							newStop.setStopFacility(existingFac);
							newStop.setAwaitDepartureTime(true);
							routeStops.add(newStop);
						}
						facilityExisting = false;
						
					}
					
					//////////////////////////////////////////////////////////////////////
					// case 3: stop in between
					else {
						TransitStopFacility betweenFac = rStop.getStopFacility();
						Coord FacCoords = betweenFac.getCoord();

						// check if arrivalOffset has to be adjusted
						if (previousOffset >= rStop.getArrivalOffset()) {
							newArrivalOffset = previousOffset + 10;
						}
						else {
							newArrivalOffset = rStop.getArrivalOffset();
						}
						
						// add dwell time of 60s
						int v = 0;
						double newDepOffset = 0;
						for (int i=0 ; i < route.getStops().size() ; i++) {
							if (route.getStops().get(i).equals(rStop)) {
								v = i;
							}
						}
						if (v==0) {log.warn("routeStop " +rStop+ " not found in route " +route.getId());}
						if (route.getStops().get(v+1).getArrivalOffset()-route.getStops().get(v).getDepartureOffset() <= 60) {
							newDepOffset = newArrivalOffset + 30;
						}
						else {
							newDepOffset = newArrivalOffset + 60;
						}
						TransitRouteStop newStop = scheduleFactory.createTransitRouteStop(rStop.getStopFacility(),
							newArrivalOffset, newDepOffset);
						
						// create node and add it to network
						nId += 1;
						Id<Node> newNodeId = Id.create(Integer.toString(nId), Node.class);
						NodeImpl newNode = (NodeImpl) factory.createNode(newNodeId, FacCoords);
						for (Node node : newPTnetwork.getNodes().values()) {
							if ((Math.abs(newNode.getCoord().getX()-node.getCoord().getX())
									+Math.abs(newNode.getCoord().getY()-node.getCoord().getY())) < 0.01) {
								nodeExisting = true;
								newNode = (NodeImpl) node;
								newNodeId = node.getId();
								newNode.setCoord(node.getCoord());
								break;
							}
						}
						if (nodeExisting != true) {
							newPTnetwork.addNode(newNode);
						}
						nodeExisting = false;
						
						// create link and add it to network
						Id<Link> nLinkId = Id.create(fromNode.getId().toString()+newNodeId.toString()+Integer.toString(newId), Link.class);
						LinkImpl nLink = (LinkImpl) factory.createLink(nLinkId, fromNode, newNode);
						double dist = Math.round(CoordUtils.calcDistance(fromNode.getCoord(), newNode.getCoord()));
						if (dist == 0) {
							countDist0 += 1;
							dist = 0.01;
						}
						double capLink = 99999.0;
						String linkType = "11";
						// reduce link travel time by leaveLinkEvent time of 1s
						double freeSpeed = dist/(newArrivalOffset-previousOffset-1);
						if (freeSpeed <= 0) {
							log.warn("negative speed!");
						}
						nLink.setLength(dist);
						nLink.setCapacity(capLink);
						nLink.setType(linkType);
						nLink.setFreespeed(freeSpeed);
						nLink.setAllowedModes(ptMode);
						for (Link link : newPTnetwork.getLinks().values()) {
							if ((Math.abs(nLink.getFromNode().getCoord().getX()-link.getFromNode().getCoord().getX())
									+Math.abs(nLink.getFromNode().getCoord().getY()-link.getFromNode().getCoord().getY())) < 0.01
									&& (Math.abs(nLink.getToNode().getCoord().getX()-link.getToNode().getCoord().getX())
											+Math.abs(nLink.getToNode().getCoord().getY()-link.getToNode().getCoord().getY())) < 0.01
											&& Math.abs(nLink.getFreespeed()-link.getFreespeed()) < 0.1) {
								linkExisting = true;
								nLink = (LinkImpl) link;
								nLinkId = link.getId();
								break;
							}
						}
						if (linkExisting != true) {
							newPTnetwork.addLink(nLink);
							newId += 1;
						}
						linkExisting = false;
												
						fromNode = newNode;
						previousOffset = newStop.getDepartureOffset();
						
						// add link to route
						newRouteLinkIds.add(nLinkId);
						
						// add stop to route and stop facility to schedule
						// check if facility with same coords and link reference already exists
						for (TransitStopFacility f : newPTschedule.getFacilities().values()) {
							if (f.getCoord().equals(betweenFac.getCoord()) && f.getLinkId().equals(nLinkId)) {
								facilityExisting = true;
								existingFac = f;
								break;
							}
						}
						// if not: create and add facility to schedule
						if (facilityExisting != true) {
							Id<TransitStopFacility> newFacId = Id.create(betweenFac.getId().toString()+Integer.toString(newCountId), TransitStopFacility.class);
							newCountId += 1;
							TransitStopFacility betweenFacnew = scheduleFactory.createTransitStopFacility(newFacId, betweenFac.getCoord(), false);
							betweenFacnew.setName(betweenFac.getName());
							betweenFacnew.setLinkId(nLinkId);
							newStop.setStopFacility(betweenFacnew);
							newStop.setAwaitDepartureTime(true);
							routeStops.add(newStop);
							newPTschedule.addStopFacility(betweenFacnew);
						}
						// otherwise refer to existing facility
						else {
							newStop.setStopFacility(existingFac);
							newStop.setAwaitDepartureTime(true);
							routeStops.add(newStop);
						}
						facilityExisting = false;
					}
					
					
				}
				
				// create network route
				NetworkRoute newNetworkRoute = RouteUtils.createNetworkRoute(newRouteLinkIds, newPTnetwork);
				// create transit route
				TransitRoute nTransitRoute = scheduleFactory.createTransitRoute(route.getId(), newNetworkRoute, routeStops, "pt");
				// copy departures
				for (Departure dep : route.getDepartures().values()) {
					nTransitRoute.addDeparture(dep);
				}
				// add route to line
				nLine.addRoute(nTransitRoute);
				//clear network route and route stops list
				newRouteLinkIds.clear();
				routeStops.clear();
				
			}
			
			// add line to schedule
			newPTschedule.addTransitLine(nLine);
			
		}
		log.info("Start adjusting network and schedule...done.");
		log.info("Schedule contains " +newPTschedule.getTransitLines().size()+ " lines.");
		log.info("Later arrivals at terminal station occurred " +count+ " times.");
		log.info("Distances was " +countDist0+ " times zero.");
		
		NetworkWriter nw = new NetworkWriter(newPTnetwork);
		nw.write("./output/uvek2030networkFINAL.xml.gz"); //uvek2005networkFINAL.xml.gz

		TransitScheduleWriter sw = new TransitScheduleWriter(newPTschedule);
		sw.writeFile("./output/uvek2030scheduleFINAL.xml.gz"); //uvek2005scheduleFINAL.xml.gz
		
	
		
	}
}
