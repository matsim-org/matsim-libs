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

public class ScheduleAdjustedNetwork {

	private static Logger log = Logger.getLogger(NetworkRoute2Schedule.class);
	
	List<Id<Link>> newRouteLinkIds = new ArrayList<Id<Link>>();
	int nId = 0;
	int newId = 1;
	int newCountId = 1;
	Node fromNode = null;
	double previousOffset = 0;
	List<TransitRouteStop> routeStops = new ArrayList<TransitRouteStop>();
	Set<String> ptMode = new HashSet<String>(Arrays.asList("pt"));
	boolean nodeExisting = false;
	boolean linkExisting = false;
	boolean facilityExisting = false;
	TransitStopFacility existingFac = null;

	public static void main(String[] args) throws Exception {
		
		ScheduleAdjustedNetwork scheduleAdjustedNetwork = new ScheduleAdjustedNetwork();
		scheduleAdjustedNetwork.run();
		
	}

	public void run() throws Exception {
		
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
		MatsimNetworkReader NetworkReader = new MatsimNetworkReader(sc); 
		NetworkReader.readFile("./input/uvek2030network_anbindungen_routes.xml"); //uvek2005network_final_cleaned.xml.gz
		log.info("Reading pt network...done.");
		log.info("Network contains " +PTnetwork.getLinks().size()+ " links and " +PTnetwork.getNodes().size()+ " nodes.");

		//////////////////////////////////////////////////////////////////////
		// read in PTschedule
		
		log.info("Reading pt schedule...");	
		TransitScheduleReader ScheduleReader = new TransitScheduleReader(sc); 
		ScheduleReader.readFile("./input/uvek2030schedule_anbindungen_routes.xml"); //uvek2005schedule_final_cleaned_test.xml.gz
		log.info("Reading pt schedule...done.");
		log.info("Schedule contains " +PTschedule.getTransitLines().size()+ " lines.");
		
		//////////////////////////////////////////////////////////////////////
		// create links between every stop point
		
		log.info("Start adjusting network and schedule...");
		for (TransitLine line : PTschedule.getTransitLines().values()){
			Id<TransitLine> newLineId = Id.create(line.getId().toString().replaceAll("\\s",""), TransitLine.class);
			TransitLine nLine = scheduleFactory.createTransitLine(newLineId);
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop rStop : route.getStops()) {
					
					//////////////////////////////////////////////////////////////////////
					// case 1: first stop and new route list
					if (route.getStops().get(0).equals(rStop)) {
						TransitStopFacility firstFac = rStop.getStopFacility();
						Coord firstFacCoords = firstFac.getCoord();
						
						// correct bias of Visum-file 2030
						if (firstFacCoords.getX() > 450 && firstFacCoords.getX() < 900
								&& firstFacCoords.getY() > 75 && firstFacCoords.getY() < 350) {
							firstFacCoords.setX((firstFacCoords.getX()*1000));
							firstFacCoords.setY((firstFacCoords.getY()*1000));
						}
						
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
						previousOffset = rStop.getArrivalOffset();
												
						// add link to route
						newRouteLinkIds.add(nLinkId);
						
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
							rStop.setStopFacility(firstFacnew);
							routeStops.add(rStop);
							newPTschedule.addStopFacility(firstFacnew);
						}
						// otherwise refer to existing facility
						else {
							rStop.setStopFacility(existingFac);
							routeStops.add(rStop);
						}
						facilityExisting = false;
					}
					
					//////////////////////////////////////////////////////////////////////
					// case 2: last stop
					else if (route.getStops().get((route.getStops().size()-1)).equals(rStop)) {
						TransitStopFacility lastFac = rStop.getStopFacility();
						Coord lastFacCoords = lastFac.getCoord();
						
						// correct bias of Visum-file 2030
						if (lastFacCoords.getX() > 450 && lastFacCoords.getX() < 900
								&& lastFacCoords.getY() > 75 && lastFacCoords.getY() < 350) {
							lastFacCoords.setX((lastFacCoords.getX()*1000));
							lastFacCoords.setY((lastFacCoords.getY()*1000));
						}
						
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
						double capLink = 99999.0;
						String linkType = "11";
						double freeSpeed = dist/(rStop.getArrivalOffset()-previousOffset);
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
						previousOffset = rStop.getArrivalOffset();
						
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
							rStop.setStopFacility(lastFacnew);
							routeStops.add(rStop);
							newPTschedule.addStopFacility(lastFacnew);
						}
						// otherwise refer to existing facility
						else {
							rStop.setStopFacility(existingFac);
							routeStops.add(rStop);
						}
						facilityExisting = false;
						
					}
					
					//////////////////////////////////////////////////////////////////////
					// case 3: stop in between
					else {
						TransitStopFacility betweenFac = rStop.getStopFacility();
						Coord FacCoords = betweenFac.getCoord();
						
						// correct bias of Visum-file 2030
						if (FacCoords.getX() > 450 && FacCoords.getX() < 900
								&& FacCoords.getY() > 75 && FacCoords.getY() < 350) {
							FacCoords.setX((FacCoords.getX()*1000));
							FacCoords.setY((FacCoords.getY()*1000));
						}
						
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
						double capLink = 99999.0;
						String linkType = "11";
						double freeSpeed = dist/(rStop.getArrivalOffset()-previousOffset);
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
						previousOffset = rStop.getArrivalOffset();
						
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
							rStop.setStopFacility(betweenFacnew);
							routeStops.add(rStop);
							newPTschedule.addStopFacility(betweenFacnew);
						}
						// otherwise refer to existing facility
						else {
							rStop.setStopFacility(existingFac);
							routeStops.add(rStop);
						}
						facilityExisting = false;
					}
					
					
				}
				
				// create network route
				NetworkRoute newNetworkRoute = RouteUtils.createNetworkRoute(newRouteLinkIds, newPTnetwork);
				// create transit route
				Id<TransitRoute> newRouteId = Id.create(route.getId().toString().replaceAll("\\s",""), TransitRoute.class);
				TransitRoute nTransitRoute = scheduleFactory.createTransitRoute(newRouteId, newNetworkRoute, routeStops, "pt");
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
		
		NetworkWriter nw = new NetworkWriter(newPTnetwork);
		nw.write("./output/uvek2030network_adjusted.xml.gz"); //uvek2005network_adjusted.xml.gz

		TransitScheduleWriter sw = new TransitScheduleWriter(newPTschedule);
		sw.writeFile("./output/uvek2030schedule_adjusted.xml.gz"); //uvek2005schedule_adjusted.xml.gz
		
	
	}

}
