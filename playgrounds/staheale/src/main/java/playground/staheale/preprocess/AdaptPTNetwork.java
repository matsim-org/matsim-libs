package playground.staheale.preprocess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class AdaptPTNetwork {

	private static Logger log = Logger.getLogger(AdaptPTNetwork.class);
	private Network network;
	private TransitSchedule PTschedule;
	private TransitSchedule scheduleWrite;

	public static void main(String[] args) {
		AdaptPTNetwork adaptNetwork = new AdaptPTNetwork();
		adaptNetwork.run();
		//adaptNetwork.cleanNetwork();
		adaptNetwork.writeNetwork();
		adaptNetwork.writeSchedule();
	}

	public void run() {
		final ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().transit().setUseTransit(true);
		sc.getConfig().scenario().setUseVehicles(true);

		final ScenarioImpl sc2 = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc2.getConfig().transit().setUseTransit(true);
		sc2.getConfig().scenario().setUseVehicles(true);
		this.scheduleWrite = sc2.getTransitSchedule();

		List<Id> unservedList = new ArrayList<Id>();
		List<Id> noIncidentLinkList = new ArrayList<Id>();
		List<Id> routeStopDoubleCoordsList = new ArrayList<Id>();
		List<Id> firstStopList = new ArrayList<Id>();
		Map<Id, String> addedStopMap = new HashMap<Id, String>();
		boolean servedStop = false;
		boolean routeMissing = false;
		int count = 0;
		int countRef = 0;
		int countNode = 0;
		int countMiss = 0;
		int countUntreated = 0;
		int countUnserved = 0;
		Node rNode = null;
		Set<String> ptMode = new HashSet<String>(Arrays.asList("pt"));
		
		List<Id> referencedFacilityList = new ArrayList<Id>();


		// ------------------- read in pt network ----------------------------
		log.info("Reading pt network xml file...");
		new MatsimNetworkReader(sc).parse("./input/uvek2030network_anbindungen_routes.xml"); //uvek2005network_anbindungen_routes.xml
		this.network = sc.getNetwork();
		log.info("Reading pt network xml file...done");
		log.info("Initial network contains " +this.network.getLinks().size()+ " links.");
		log.info("Initial network contains " +this.network.getNodes().size()+ " nodes.");

		// ------------------- read in PTschedule ----------------------------
		log.info("Reading pt schedule...");	
		TransitScheduleReader ScheduleReader = new TransitScheduleReader(sc); 
		ScheduleReader.readFile("./input/uvek2030schedule_anbindungen_routes.xml"); //uvek2005schedule_anbindungen_routes_ohneRampa_mit644route.xml.gz"
		this.PTschedule = sc.getTransitSchedule();
		log.info("Reading pt schedule...done.");
		log.info("Schedule contains " +this.PTschedule.getTransitLines().size()+ " lines.");

		// ------------------- delete nodes without incident link ----------------------------
		log.info("Deleting nodes without incident link...");					
		for (Node node : this.network.getNodes().values()) {
			if (node.getInLinks().isEmpty() && node.getOutLinks().isEmpty()) {
				noIncidentLinkList.add(node.getId());
			}
		}
		for (int i=0 ; i < noIncidentLinkList.size() ; i++) {
			this.network.getNodes().remove(noIncidentLinkList.get(i));
			countNode += 1;
		}
		log.info("Deleting nodes without incident link...done");
		log.info(+countNode+ " nodes deleted.");

		// ------------------- check if every stop has a reference link ----------------------------
		for (TransitStopFacility f : this.PTschedule.getFacilities().values()) {
			if (f.getLinkId() == null) {
				log.warn("no link found for stop " +f.getId().toString());
			}
		}

		// ------------------- change mode of transit lines ----------------------------
		log.info("Changing mode of transit lines...");
		String PTMODE = "pt";
		for (TransitLine line : PTschedule.getTransitLines().values()){
			for (TransitRoute route : line.getRoutes().values()) {
				route.setTransportMode(PTMODE);
			}
		}
		log.info("Changing mode of transit lines...done");

		// ------------------- remove route profile stops with same coordinates as previous stop ----------------------------
//		for (TransitStopFacility stopPoint : this.PTschedule.getFacilities().values()){
//			this.scheduleWrite.addStopFacility(stopPoint);
//		}
		
		// ------------------- save stop facilities that are referenced to a first or last route stop to a list ----------------------------
		log.info("Creating first/last stop list...");
		for (TransitStopFacility stopPoint : this.PTschedule.getFacilities().values()){
			for (TransitLine l : this.PTschedule.getTransitLines().values()){
				for (TransitRoute r : l.getRoutes().values()) {
					TransitRouteStop rFirstSt = r.getStops().get(0);
					TransitRouteStop rLastSt = r.getStops().get((r.getStops().size()-1));
					if (rFirstSt.getStopFacility().equals(stopPoint) || rLastSt.getStopFacility().equals(stopPoint) && firstStopList.contains(stopPoint.getId()) != true) {
						firstStopList.add(stopPoint.getId());
					}
				}
			}
		}
		log.info("Creating first/last stop list...done");
		log.info("List contains " +firstStopList.size()+ " facilities");
		
		
		
		

		TransitScheduleFactory builder = this.scheduleWrite.getFactory();
		NetworkFactory factory = this.network.getFactory();
		Node startNode = null;
		Coord previousCoords = null;
		int doubleStopCount = 0;
		int countDeletedRouteStops = 0;
		boolean firstStop = false;
		for (TransitLine line : this.PTschedule.getTransitLines().values()){
			//create line
			TransitLine tLine = builder.createTransitLine(line.getId());
			for (TransitRoute route : line.getRoutes().values()) {
				// add route stop to route profile list if previous route stop has different coordinates
				List<TransitRouteStop> routeStops = new ArrayList<TransitRouteStop>();

				List<Id<Link>> newRouteLinkIds = new ArrayList<Id<Link>>();

				Id startLinkId = route.getRoute().getStartLinkId();
				startNode = this.network.getLinks().get(startLinkId).getFromNode();
				TransitRouteStop rFirstStop = route.getStops().get(0);
				for (TransitRouteStop rStop : route.getStops()) {

					// ------------------- add route link for start link of every route ----------------------------
					// first stop is the start and end node of this link
					if (rStop.equals(rFirstStop) && firstStop != true) {

						firstStop = true;
						Id<Link> newLinkId = Id.create("000"+startLinkId.toString()+startLinkId.toString(), Link.class);
						LinkImpl newLink = (LinkImpl) factory.createLink(newLinkId, startNode, startNode);
						newLink.setAllowedModes(ptMode);
						newLink.setLength(50);
						newLink.setFreespeed(10);
						newLink.setCapacity(3000);
						newLink.setType("11");
						if (this.network.getLinks().containsKey(newLinkId) != true) {
							this.network.addLink(newLink);
						}
						

						
						rStop.getStopFacility().setLinkId(newLinkId);
						newRouteLinkIds.addAll(route.getRoute().getLinkIds());
						newRouteLinkIds.add(0, route.getRoute().getStartLinkId());
						newRouteLinkIds.add(route.getRoute().getEndLinkId());
						newRouteLinkIds.add(0, newLinkId);
//						if (newLinkId.toString().equals("000141857141857")) {
//							log.warn("check if true");
//						}
						startNode = null;
					}


					if (rStop.getStopFacility().getCoord().equals(previousCoords)) {
						doubleStopCount += 1;
						if (doubleStopCount <= 1) {
							log.warn("Stop " +rStop.toString()+ " has same coordinates as previous stop! line " +line.getId());
							log.warn("Future occurences of this warning are suppressed.");
						}
					}
					else {
						//TransitRouteStop routeStop = builder.createTransitRouteStop(rStop.getStopFacility(), rStop.getArrivalOffset(), rStop.getDepartureOffset());
//						if (line.getId().toString().equals("BUS.601.802606")) {
//							log.info("transit line BUS.601.802606");
//						}
						routeStops.add(rStop);
					}
					previousCoords = rStop.getStopFacility().getCoord();
				}
				firstStop = false;
				previousCoords = null;
				// create route
				NetworkRoute newNetworkRoute = RouteUtils.createNetworkRoute(newRouteLinkIds, this.network);

				// ------------------- adapt link reference for the stops according to the route ----------------------------

				int endKey = 0;
				
				for (int i = 1; i < (routeStops.size()); i++) {
					Id<Link> lastLinkId = null;
					int startKey = 0;
					int vMem = 0;
					TransitRouteStop precedingStop = routeStops.get(i-1);
					TransitRouteStop currentStop = routeStops.get(i);
					Coord startCoord = precedingStop.getStopFacility().getCoord();
					double xStart = startCoord.getX();
					double yStart = startCoord.getY();
					Coord endCoord = currentStop.getStopFacility().getCoord();
					double xEnd = endCoord.getX();
					double yEnd = endCoord.getY();
					if (line.getId().toString().equals("BUS.32.803101")) {
						log.info("transit line BUS.32.803101");
					}

					// go through network route until precedingStop is reached
					for (int v = endKey; v < newNetworkRoute.getLinkIds().size(); v++) {
						Id<Link> linkId = newNetworkRoute.getLinkIds().get(v);
						Link link = this.network.getLinks().get(linkId);
						Coord linkCoord = link.getFromNode().getCoord();
						double xLink = linkCoord.getX();
						double yLink = linkCoord.getY();
						if ((Math.abs(xStart-xLink)+Math.abs(yStart-yLink)) < 0.01) {
							startKey = v;
							vMem = v;
							break;
						}
					}
					if (precedingStop.getDepartureOffset() != 0.0 && startKey == 0 && i == (routeStops.size()-2)) {
						// TODO: deal with case of where bus stop is located away from route
						// see TransitRouteStop stop=300878_Tro,3091,TPG___120482378 within route Tro,3091,TPG___.1.Tro,3095,TPG___.>
						// TODO: deal with case where consecutive stops refer to the same route link
						// see TransitRouteStop stop=8575100_BUS.1.8027811422934 within route BUS.1.802781.1.BUS.1.802781.>
						
						log.info("preceding stop could not be reached!");
						log.info("precedingStop is " +precedingStop.toString()+ " within route " +route.getId().toString());
					}
					// go through network route until currentStop is reached and memorize the id of the last link
					for (int z = startKey; z < newNetworkRoute.getLinkIds().size(); z++) {
						Id<Link> linkId2 = newNetworkRoute.getLinkIds().get(z);
						Link link2 = this.network.getLinks().get(linkId2);
						Coord link2Coord = link2.getFromNode().getCoord();
						Coord link2CoordTo = link2.getToNode().getCoord();
						double xLink2 = link2Coord.getX();
						double yLink2 = link2Coord.getY();
						if ((Math.abs(xEnd-xLink2)+Math.abs(yEnd-yLink2)) < 0.01 && link2Coord.equals(link2CoordTo) != true) {
							lastLinkId = newNetworkRoute.getLinkIds().get(z);
							endKey = z;
							if (line.getId().toString().equals("BUS.32.803101")) {
								log.info("transit line BUS.32.803101");
							}
							break;
						}

					}
					
					if (i == (routeStops.size()-2)) {
						Id<Link> lastLinkId2 = newNetworkRoute.getEndLinkId();
						Link lastLink2 = this.network.getLinks().get(lastLinkId2);
						Coord lastLink2Coord = lastLink2.getFromNode().getCoord();
						Coord lastLink2CoordTo = lastLink2.getToNode().getCoord();
						double xLastLink2 = lastLink2Coord.getX();
						double yLastLink2 = lastLink2Coord.getY();
						if ((Math.abs(xEnd-xLastLink2)+Math.abs(yEnd-yLastLink2)) < 0.01 && lastLink2Coord.equals(lastLink2CoordTo) != true) {
							lastLinkId = newNetworkRoute.getEndLinkId();
							if (line.getId().toString().equals("BUS.32.803101")) {
								log.info("transit line BUS.32.803101");
							}
						}
					}

					
					if (i == (routeStops.size()-1)) {
						lastLinkId = newNetworkRoute.getEndLinkId();
					}




					if (lastLinkId == null) {
						if (newNetworkRoute.getLinkIds().contains(currentStop.getStopFacility().getLinkId())) {

//							// create new facility with coords of from-link-node of reference node
//							Id oldStopId = currentStop.getStopFacility().getId();
//							String oldStopName = currentStop.getStopFacility().getName();
//							Link refLink = this.network.getLinks().get(currentStop.getStopFacility().getLinkId());
//							Coord newCoord = refLink.getFromNode().getCoord();
//							Id newFacilityId = sc.createId("010" + oldStopId + route.getId());
//							TransitStopFacility newStopFacility = builder.createTransitStopFacility(newFacilityId, newCoord, false);
//							newStopFacility.setName(oldStopName);
//							newStopFacility.setLinkId(currentStop.getStopFacility().getId());
//							//						if (line.getId().toString().equals("AT D 1196")) {
//							//							log.info("transit line AT D 1196");
//							//						}
//							this.scheduleWrite.addStopFacility(newStopFacility);
//							currentStop.setStopFacility(newStopFacility);
//							
//							// AGAIN: go through network route until currentStop is reached and memorize the id of the last link
//							for (int z = startKey; z < newNetworkRoute.getLinkIds().size(); z++) {
//								Id linkId2 = newNetworkRoute.getLinkIds().get(z);
//								Link link2 = this.network.getLinks().get(linkId2);
//								Coord link2Coord = link2.getFromNode().getCoord();
//								double xLink2 = link2Coord.getX();
//								double yLink2 = link2Coord.getY();
//								if ((Math.abs(xEnd-xLink2)+Math.abs(yEnd-yLink2)) < 0.01) {
//									lastLinkId = newNetworkRoute.getLinkIds().get(z);
//									//								if (line.getId().toString().equals("AT D 1196")) {
//									//									log.info("transit line AT D 1196");
//									//								}
//									break;
//								}
//							}
//							
							//log.warn("no link found for currentStop " +currentStop.toString()+ "within route " + route.getId());
							
						}
						else {
							//log.warn("no link found for currentStop " +currentStop.toString()+ "within route " + route.getId());
						}
					}
					
//					if (lastLinkId == null) {
//						log.warn("no link found for currentStop " +currentStop.toString()+ "within route " + route.getId());
//					}

					
					if (lastLinkId != null) {
					
					
					
					// search for facility with correct reference link
					boolean foundStopFacility = false;
					Id<TransitStopFacility> correctReferenceFacility = null;
					for (TransitStopFacility stopPoint : this.PTschedule.getFacilities().values()){
						double xStop = stopPoint.getCoord().getX();
						double yStop = stopPoint.getCoord().getY();					
						if (stopPoint.getLinkId().equals(lastLinkId) && (Math.abs(xEnd-xStop)+Math.abs(yEnd-yStop)) < 0.01 && firstStopList.contains(stopPoint.getId()) != true) {
							correctReferenceFacility = stopPoint.getId();
							foundStopFacility = true;
							break;
						}
					}

					// if facility is found, set reference for stop, otherwise create new one
					if (foundStopFacility) {
						if (line.getId().toString().equals("BUS.32.803101")) {
							log.info("transit line BUS.32.803101");
						}
						currentStop.setStopFacility(this.PTschedule.getFacilities().get(correctReferenceFacility));
						foundStopFacility = false;
						if (line.getId().toString().equals("BUS.32.803101")) {
							log.info("transit line BUS.32.803101");
						}
					}

					// create new facility with correct reference link
					else {
						Id oldStopId = currentStop.getStopFacility().getId();
						String oldStopName = currentStop.getStopFacility().getName();
						Coord oldStopCoord = currentStop.getStopFacility().getCoord();
						Id<TransitStopFacility> newFacilityId = Id.create("000" + oldStopId + route.getId(), TransitStopFacility.class);
						TransitStopFacility newStopFacility = builder.createTransitStopFacility(newFacilityId, oldStopCoord, false);
						newStopFacility.setName(oldStopName);
						newStopFacility.setLinkId(lastLinkId);
						if (line.getId().toString().equals("BUS.32.803101")) {
							log.info("transit line BUS.32.803101");
						}
						this.PTschedule.addStopFacility(newStopFacility);
						currentStop.setStopFacility(newStopFacility);
					}

				}
					
					else {
						Id nlinkId = null;
						// create facility with reference to the following link of preceding stop (v+1)
						if (i == (routeStops.size()-2)) {
							nlinkId = newNetworkRoute.getEndLinkId();
						}
						else {
							// TODO: look for closest link
							if (vMem == 0) {
								for (int z = 0; z < newNetworkRoute.getLinkIds().size(); z++) {
									if (newNetworkRoute.getLinkIds().get(z).equals(precedingStop.getStopFacility().getLinkId())) {
										nlinkId = newNetworkRoute.getLinkIds().get(z+1);
									}
								}							
							}
							else {
								nlinkId = newNetworkRoute.getLinkIds().get(vMem+1);

							}

						}
						if (nlinkId != null) {
							Id oldStopId = currentStop.getStopFacility().getId();
							String oldStopName = currentStop.getStopFacility().getName();
							Coord oldStopCoord = currentStop.getStopFacility().getCoord();
							Id<TransitStopFacility> newFacilityId = Id.create("020" + oldStopId + route.getId(), TransitStopFacility.class);
							TransitStopFacility newStopFacility = builder.createTransitStopFacility(newFacilityId, oldStopCoord, false);
							newStopFacility.setName(oldStopName);
							newStopFacility.setLinkId(nlinkId);
							if (line.getId().toString().equals("BUS.32.803101")) {
								log.info("transit line BUS.32.803101");
							}
							this.PTschedule.addStopFacility(newStopFacility);
							currentStop.setStopFacility(newStopFacility);
							
							//countUntreated += 1;
						}
						else {
							log.warn("stop reference unchanged for: " +currentStop.getStopFacility()+ ", " +route.getId()+ ", " +line.getId());
						}
					}

				}

				// create route profile
				TransitRoute tRoute = builder.createTransitRoute(route.getId(), newNetworkRoute, routeStops, route.getTransportMode()); //newNetworkRoute
				// add departures to route
				for (Departure dep : route.getDepartures().values()) {
					Departure d = builder.createDeparture(dep.getId(), dep.getDepartureTime());
					d.setVehicleId(dep.getVehicleId());
					tRoute.addDeparture(d);
				}	
				if (line.getId().toString().equals("BUS.32.803101")) {
					log.info("transit line BUS.32.803101");
				}
				tLine.addRoute(tRoute);
			}

			this.scheduleWrite.addTransitLine(tLine);

		}
		log.info("number of route profile stops with same coordinates as previous stop: " +doubleStopCount);
		
		// ------------------- delete unreferenced stop facilities ----------------------------
		log.info("Deleting unreferenced stop facilities...");	
		boolean referenced = false;
		for (TransitStopFacility fac : this.PTschedule.getFacilities().values()) {
			for (TransitLine line : this.PTschedule.getTransitLines().values()){
				for (TransitRoute route : line.getRoutes().values()) {
					for (TransitRouteStop rStop : route.getStops()) {
						if (rStop.getStopFacility().getId().equals(fac.getId())) {
							referenced = true;
							break;
						}
					}
				}
			}
			if (referenced != true) {
				unservedList.add(fac.getId());
			}
			referenced = false;
		}
		for (TransitStopFacility stopFac : this.PTschedule.getFacilities().values()){
			if (unservedList.contains(stopFac.getId()) != true) {
				this.scheduleWrite.addStopFacility(stopFac);
			}
		}
		log.info("Deleting unreferenced stop facilities...done");
		log.info("Number of stop facilities before " +this.PTschedule.getFacilities().size());
		log.info("Number of stop facilities now " +this.scheduleWrite.getFacilities().size());

	}

	public void cleanNetwork() {
		new NetworkCleaner().run(this.network);
	}

	public void writeNetwork() {
		log.info("Final network contains " +this.network.getLinks().size()+ " links.");
		log.info("Final network contains " +this.network.getNodes().size()+ " nodes.");
		new NetworkWriter(this.network).write("./output/uvek2030network_final_cleaned.xml.gz"); //uvek2005network_final_cleaned.xml.gz
	}

	public void writeSchedule() {
		TransitScheduleWriter sw = new TransitScheduleWriter(this.scheduleWrite);
		sw.writeFile("./output/uvek2030schedule_final_cleaned_test.xml.gz"); //uvek2005schedule_final_cleaned_test.xml.gz
	}
}
