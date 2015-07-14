package playground.staheale.matsim2030;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class NetworkRoute2Schedule {

	private static Logger log = Logger.getLogger(NetworkRoute2Schedule.class);
	List<Id<Link>> newRouteLinkIds = new ArrayList<Id<Link>>();
	String oldStartTransitRouteId = null;
	String oldEndTransitRouteId = null;
	Id oldId = null;
	int oldIstHP = 1;
	Id newRouteLinkId = null;
	int countRoutes = 0;
	int countTransitRoutes = 0;
	int countSameCoord = 0;
	int numberOfLines = 485544;
	int ind = 0;
	double progress = 0;
	TransitRoute modRoute = null;
	String oldLinName = null;
	String oldRouteName = null;
	String oldIndex = null;
	List<Id> unservedList = new ArrayList<Id>();
	boolean servedStop = false;
	int countStops = 0;
	int i=0;


	public static void main(String[] args) throws Exception {
		NetworkRoute2Schedule networkRoute2Schedule = new NetworkRoute2Schedule();
		networkRoute2Schedule.run();
	}

	public void run() throws Exception {

		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().transit().setUseTransit(true);
		sc.getConfig().scenario().setUseVehicles(true);
		Network PTnetwork = sc.getNetwork();
		TransitSchedule PTschedule = sc.getTransitSchedule();
		TransitScheduleFactory scheduleFactory = PTschedule.getFactory();

		// ------------------- read in PTnetwork ----------------------------
		log.info("Reading pt network...");	
		MatsimNetworkReader NetworkReader = new MatsimNetworkReader(sc); 
		NetworkReader.readFile("./input/02-OeV_2030+_DWV_Ref_Mit_IterationGerman_adapted.xml");
		log.info("Reading pt network...done.");
		log.info("Network contains " +PTnetwork.getLinks().size()+ " links and " +PTnetwork.getNodes().size()+ " nodes.");

		// ------------------- read in PTschedule ----------------------------
		log.info("Reading pt schedule...");	
		TransitScheduleReader ScheduleReader = new TransitScheduleReader(sc); 
		ScheduleReader.readFile("./input/02-OeV_2030+_DWV_Ref_Mit_IterationGerman_adapted_TransitSchedule.xml");
		log.info("Reading pt schedule...done.");
		log.info("Schedule contains " +PTschedule.getTransitLines().size()+ " lines.");

		// ------------------- read in csv file -----------------------
		log.info("Reading csv file...");		
		File file = new File("./input/uvek2030_routes.csv");
		BufferedReader bufRdr = new BufferedReader(new FileReader(file));
		String curr_line = bufRdr.readLine();
		log.info("Start line iteration through csv file");


		// ------------------- create network routes -----------------------
		while ((curr_line = bufRdr.readLine()) != null) {
			String[] entries = curr_line.split(";");
			String linName = entries[0].trim();
			String linRouteName = entries[1].trim();
			String rCode = entries[2].trim();
			String count = entries[3].trim();
			String istHaltepunkt = entries[4].trim();
			String knotenNr = entries[5].trim();
			//log.info("LinName = " +linName+ ", LinRouteName = " +linRouteName+
			//		", rCode = " +rCode+ ", index = "
			//		+index+	", knotenNr = " +knotenNr);


			int index = Integer.parseInt(count);
			int istHP = Integer.parseInt(istHaltepunkt);
			Id<TransitRoute> newRouteNodeId = Id.create(knotenNr, TransitRoute.class);

			// -------- case 1: new route list --------
			if (index == 1) {
				//log.info("index = 1");
				if (newRouteLinkIds.isEmpty() != true) {
					//log.info("newRouteLinkIds: " +newRouteLinkIds.toString());
					NetworkRoute newNetworkRoute = RouteUtils.createNetworkRoute(newRouteLinkIds, PTnetwork);
					//log.info("NetworkRoute: " +newNetworkRoute.getLinkIds());
					for (TransitLine line : PTschedule.getTransitLines().values()){
						for (TransitRoute route : line.getRoutes().values()) {
							String routeId = route.getId().toString();
							if (routeId.startsWith(oldStartTransitRouteId) && routeId.endsWith(oldEndTransitRouteId)) {
								//								if (oldStartTransitRouteId.equals("BUS.1.803523.1") && oldEndTransitRouteId.equals(">")) {
								//									log.info("TransitRoute: " +route);
								//								}
								//log.info("TransitRoute: " +route);
								route.setRoute(newNetworkRoute);
								countRoutes += 1;
							}
						}
					}

					// -------- create new stop, set link as reference for stop point --------
					TransitStopFacility oldIdStop = PTschedule.getFacilities().get(oldId);
					// create stop
					//log.info("oldIdStop: " +oldIdStop.getId());
					if (oldIdStop.getLinkId() == null) {
						Id<TransitStopFacility> newStopId = Id.create(oldId.toString()+"_"+oldLinName+oldRouteName+oldIndex, TransitStopFacility.class);
						TransitStopFacility newStop = scheduleFactory.createTransitStopFacility(newStopId, oldIdStop.getCoord(), false);
						newStop.setName(oldIdStop.getName()+ " "+linName+linRouteName);
						newStop.setLinkId(newRouteLinkId);
						PTschedule.addStopFacility(newStop);
						//log.info("linkId: " +newRouteLinkId+ " set for stopId: " +newStop);

						// add to route profile
						for (TransitLine line : PTschedule.getTransitLines().values()){
							for (TransitRoute route : line.getRoutes().values()) {
								String routeId = route.getId().toString();
								if (routeId.startsWith(oldStartTransitRouteId) && routeId.endsWith(oldEndTransitRouteId)) {
									modRoute = route;
									//log.info("routeId: " +routeId+ ", looking for " +oldIdStop.getId().toString());
									for (int i = 0 ; i < (route.getStops().size()) ; i++) {
										TransitRouteStop rs = route.getStops().get(i);
										//log.info("rs: " +rs.getStopFacility().getId());
										if (rs.getStopFacility().getId().equals(oldIdStop.getId())) {
											//log.info("ids are equal!");
											rs.setStopFacility(newStop);
											break;
										}
									}


								}
							}
						}
					}
					else if (oldIdStop.getLinkId().equals(newRouteLinkId) != true) {
						Id<TransitStopFacility> newStopId = Id.create(oldId.toString()+"_"+oldLinName+oldRouteName+oldIndex, TransitStopFacility.class);
						TransitStopFacility newStop = scheduleFactory.createTransitStopFacility(newStopId, oldIdStop.getCoord(), false);
						newStop.setName(oldIdStop.getName()+ " "+linName+linRouteName);
						newStop.setLinkId(newRouteLinkId);
						PTschedule.addStopFacility(newStop);
						//log.info("linkId: " +newRouteLinkId+ " set for stopId: " +newStop);

						// add to route profile
						for (TransitLine line : PTschedule.getTransitLines().values()){
							for (TransitRoute route : line.getRoutes().values()) {
								String routeId = route.getId().toString();
								if (routeId.startsWith(oldStartTransitRouteId) && routeId.endsWith(oldEndTransitRouteId)) {
									modRoute = route;
									//log.info("routeId: " +routeId+ ", looking for " +oldIdStop.getId().toString());
									for (int i = 0 ; i < (route.getStops().size()) ; i++) {
										TransitRouteStop rs = route.getStops().get(i);
										//log.info("rs: " +rs.getStopFacility().getId());
										if (rs.getStopFacility().getId().equals(oldIdStop.getId())) {
											//log.info("ids are equal!");
											rs.setStopFacility(newStop);
											break;
										}
									}
								}
							}
						}
					}



				}
				newRouteLinkIds.clear();
			}

			// -------- case 2: ongoing route list --------
			else {
				//log.info("startNode: " +oldId.toString());
				//log.info("endNode: " +newRouteNodeId.toString());
				//log.info("newRouteLinkIds: " +newRouteLinkIds.toString());

				Node oldIdNode = PTnetwork.getNodes().get(oldId);
				Coord oldIdCoord = null;
				if (oldIdNode == null) {
					oldIdCoord = PTschedule.getFacilities().get(oldId).getCoord();
				}
				else {
					oldIdCoord = PTnetwork.getNodes().get(oldId).getCoord();
				}
				Node newRouteNode = PTnetwork.getNodes().get(newRouteNodeId);
				Coord newRouteNodeIdCoord = null;
				if (newRouteNode == null) {
					TransitStopFacility fac = PTschedule.getFacilities().get(newRouteNodeId);
					if (fac == null) {
						
						// stop has same coordinates-->leave newRouteNodeId out						
						newRouteNodeId = oldId;
						newRouteNodeIdCoord = oldIdCoord;
						
					}
					else {
						newRouteNodeIdCoord = fac.getCoord();
					}
				}
				else {
					newRouteNodeIdCoord = PTnetwork.getNodes().get(newRouteNodeId).getCoord();
				}

				// -------- count stop pairs with same coordinates --------
				if (oldIdCoord.equals(newRouteNodeIdCoord)) {
					//log.warn("line " +linName+ " and rCode " +rCode+ ": startNode " +oldId.toString()+ " and endnode " +newRouteNodeId.toString()+ " have the same coords.");
					countSameCoord += 1;
					//TODO: what if stops have the same coords?
				}

				// -------- search for a link between the stop points --------
				for (Link link : PTnetwork.getLinks().values()) {
					if (link.getFromNode().getCoord().equals(oldIdCoord) && link.getToNode().getCoord().equals(newRouteNodeIdCoord)) {
						newRouteLinkId = link.getId();
						break;
					}
				}

				// -------- create new stop, set link as reference for stop point --------
				if (oldIstHP == 1) {
					TransitStopFacility oldIdStop = PTschedule.getFacilities().get(oldId);
					//log.info("oldId " +oldId);
					// create stop
					if (oldIdStop.getLinkId() == null) {
						Id<TransitStopFacility> newStopId = Id.create(oldId.toString()+"_"+linName+linRouteName+count+i, TransitStopFacility.class);
						TransitStopFacility newStop = scheduleFactory.createTransitStopFacility(newStopId, oldIdStop.getCoord(), false);
						newStop.setName(oldIdStop.getName()+ " "+linName+linRouteName);
						newStop.setLinkId(newRouteLinkId);
						PTschedule.addStopFacility(newStop);
						//log.info("linkId: " +newRouteLinkId+ " set for stopId: " +newStop);

						// add to route profile
						for (TransitLine line : PTschedule.getTransitLines().values()){
							for (TransitRoute route : line.getRoutes().values()) {
								String routeId = route.getId().toString();
								if (routeId.startsWith(oldStartTransitRouteId) && routeId.endsWith(oldEndTransitRouteId)) {
									modRoute = route;
									//log.info("routeId: " +routeId+ ", looking for " +oldIdStop.getId().toString());
									for (int i = 0 ; i < (route.getStops().size()) ; i++) {
										TransitRouteStop rs = route.getStops().get(i);
										//log.info("rs: " +rs.getStopFacility().getId());
										if (rs.getStopFacility().getId().equals(oldIdStop.getId())) {
											//log.info("ids are equal!");
											rs.setStopFacility(newStop);
											break;
										}
									}


								}
							}
						}
					}
					else if (oldIdStop.getLinkId().equals(newRouteLinkId) != true) {
						Id<TransitStopFacility> newStopId = Id.create(oldId.toString()+"_"+linName+linRouteName+count, TransitStopFacility.class);
						TransitStopFacility newStop = scheduleFactory.createTransitStopFacility(newStopId, oldIdStop.getCoord(), false);
						newStop.setName(oldIdStop.getName()+ " "+linName+linRouteName);
						newStop.setLinkId(newRouteLinkId);
						PTschedule.addStopFacility(newStop);
						//log.info("linkId: " +newRouteLinkId+ " set for stopId: " +newStop);

						// add to route profile
						for (TransitLine line : PTschedule.getTransitLines().values()){
							for (TransitRoute route : line.getRoutes().values()) {
								String routeId = route.getId().toString();
								if (routeId.startsWith(oldStartTransitRouteId) && routeId.endsWith(oldEndTransitRouteId)) {
									modRoute = route;
									//log.info("routeId: " +routeId+ ", looking for " +oldIdStop.getId().toString());
									for (int i = 0 ; i < (route.getStops().size()) ; i++) {
										TransitRouteStop rs = route.getStops().get(i);
										//log.info("rs: " +rs.getStopFacility().getId());
										if (rs.getStopFacility().getId().equals(oldIdStop.getId())) {
											//log.info("ids are equal!");
											rs.setStopFacility(newStop);
											break;
										}
									}


								}
							}
						}

						//						// add to route profile
						//						TransitRouteStop s = modRoute.getStops().get(ind);
						//						s.setStopFacility(newStop);
						//						TransitRouteStop newRouteStop = scheduleFactory.createTransitRouteStop(newStop, s.getArrivalOffset(), s.getDepartureOffset());
						//						modRoute.getStops().remove(ind);
						//						modRoute.getStops().add(ind, newRouteStop);
						//						log.info("newRouteStop: " +newRouteStop+ " set for rs: " +s);


					}
				}

				// -------- add link id to list if not added one line before (TODO: how to handle stop point pairs with same coordinates) --------
				if (newRouteLinkIds.isEmpty() != true) {
					if (newRouteLinkIds.get((newRouteLinkIds.size()-1)).equals(newRouteLinkId) != true) {
						newRouteLinkIds.add(newRouteLinkId);	
					}
				}
				else {
					if (newRouteLinkId != null && oldIdCoord.equals(newRouteNodeIdCoord) != true) {
						newRouteLinkIds.add(newRouteLinkId);
					}
				}

				// -------- handle last line (TODO: remove ugly hack with hard coded values) --------
				if (linName.equals("YU 841") && linRouteName.equals("_") && count.equals("101") && knotenNr.equals("10022401")) {
					//					log.info("startNode: " +oldId.toString());
					//					log.info("endNode: " +newRouteNodeId.toString());
					//					log.info("newRouteLinkIds: " +newRouteLinkIds.toString());
					NetworkRoute newNetworkRoute = RouteUtils.createNetworkRoute(newRouteLinkIds, PTnetwork);
					for (TransitLine line : PTschedule.getTransitLines().values()){
						for (TransitRoute route : line.getRoutes().values()) {
							String routeId = route.getId().toString();
							if (routeId.startsWith(oldStartTransitRouteId) && routeId.endsWith(oldEndTransitRouteId)) {
								//log.info("TransitRoute: " +route);
								route.setRoute(newNetworkRoute);
								countRoutes += 1;
							}
						}
					}
				}
			}

			oldStartTransitRouteId = linName+ "." +linRouteName;
			oldLinName = linName;
			oldRouteName = linRouteName;
			oldIndex = count;
			oldEndTransitRouteId = rCode;
			oldId = newRouteNodeId;
			oldIstHP = istHP;
			i += 1;
		}
		log.info("End line iteration through csv file");

		// -------- check if every transit route has a route defined --------
		//		for (TransitLine line : PTschedule.getTransitLines().values()) {
		//			countTransitRoutes += line.getRoutes().size();
		//			for (TransitRoute route : line.getRoutes().values()) {
		//				if (route.getRoute() == null) {
		//					log.info("no route found for transit route " +route.getId());
		//				}
		//			}
		//		}

		log.info(countSameCoord+ " pairs have the same coords.");
		log.info(countRoutes+ " routes were added (schedule contains " +countTransitRoutes+ " transit routes)");
		bufRdr.close();
		
		// ------------------- delete unserved stops ----------------------------
		log.info("Searching for unserved stops...");	
		for (TransitStopFacility stopPoint : PTschedule.getFacilities().values()){
			Id stopId = stopPoint.getId();
			for (TransitLine line : PTschedule.getTransitLines().values()){
				for (TransitRoute route : line.getRoutes().values()) {
					for (TransitRouteStop rStop : route.getStops()) {
						if (rStop.getStopFacility().getId().equals(stopId))
							servedStop = true;
					}
				}
			}
			if (servedStop != true) {
				unservedList.add(stopId);
			}
			servedStop = false;
		}
		log.info("Searching for unserved stops...done");
		log.info("Number of unserved stops: " +unservedList.size());
		log.info("Deleting unserved stops...");
		for (int i=0 ; i < unservedList.size() ; i++) {
			PTschedule.removeStopFacility(PTschedule.getFacilities().get(unservedList.get(i)));
			countStops += 1;
		}
		log.info("Deleting unserved stops...done");
		log.info(+countStops+ " stops deleted.");

		// ------------------- write schedule file-----------------------
		TransitScheduleWriter sw = new TransitScheduleWriter(PTschedule);
		sw.writeFile("./output/uvek2030schedule_with_routes.xml");
	}
}