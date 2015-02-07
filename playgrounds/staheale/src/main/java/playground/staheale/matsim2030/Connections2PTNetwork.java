package playground.staheale.matsim2030;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

public class Connections2PTNetwork {

	private static Logger log = Logger.getLogger(Connections2PTNetwork.class);

	public static void main(String[] args) throws Exception {
		Connections2PTNetwork connections2PTNetwork = new Connections2PTNetwork();
		connections2PTNetwork.run();
	}

	public void run() throws Exception {

		List<List<String>> bezirkList = new ArrayList<List<String>>();

		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().scenario().setUseTransit(true);
		sc.getConfig().scenario().setUseVehicles(true);
		Network PTnetwork = sc.getNetwork();
		TransitSchedule PTschedule = sc.getTransitSchedule();
		Vehicles PTvehicles = sc.getTransitVehicles();

		// ------------------- read in PTnetwork ----------------------------
		log.info("Reading pt network...");	
		MatsimNetworkReader NetworkReader = new MatsimNetworkReader(sc); 
		NetworkReader.readFile("./input/02-OeV_2030+_DWV_Ref_Mit_IterationGerman_adapted.xml");
		log.info("Reading pt network...done.");
		log.info("Network contains " +PTnetwork.getLinks().size()+ " links and " +PTnetwork.getNodes().size()+ " nodes.");

		// ------------------- read in PTschedule ----------------------------
		log.info("Reading pt schedule...");	
		TransitScheduleReader ScheduleReader = new TransitScheduleReader(sc); 
		ScheduleReader.readFile("./input/uvek2030schedule_with_routes.xml");
		log.info("Reading pt schedule...done.");
		log.info("Schedule contains " +PTschedule.getTransitLines().size()+ " lines.");

		// ------------------- read in PTvehicles ----------------------------
		log.info("Reading pt vehicles...");
		VehicleReaderV1 VehicleReader = new VehicleReaderV1(sc.getTransitVehicles());
		VehicleReader.parse("./input/02-OeV_2030+_DWV_Ref_Mit_IterationGerman_adapted_PTVehicles.xml"); 
		log.info("Reading pt vehicles...done.");
		log.info("Vehicle files contains " +PTvehicles.getVehicleTypes().size()+ " vehicle types and "
				+PTvehicles.getVehicles().size()+ " vehicles");

		NetworkFactory factory = PTnetwork.getFactory();
		TransitScheduleFactory scheduleFactory = PTschedule.getFactory();
		VehiclesFactory vehicleFactory = PTvehicles.getFactory();

		// ------------------- read in csv file -----------------------
		log.info("Reading csv file...");		
		File file = new File("./input/Bezirk_Knoten_Anbindungen2030.csv");
		BufferedReader bufRdr = new BufferedReader(new FileReader(file));
		String curr_line = bufRdr.readLine();
		log.info("Start line iteration through csv file");

		// ------------------- create virtual vehicle type -----------------------
		Id<VehicleType> virtualVehicleID = Id.create("901", VehicleType.class);
		VehicleType virtualVehicleType = vehicleFactory.createVehicleType(virtualVehicleID);
		virtualVehicleType.setDescription("Virtual urban vehicle");
		int seats = 1000;
		int persons = 0;
		VehicleCapacity cap = vehicleFactory.createVehicleCapacity();
		cap.setSeats(seats);
		cap.setStandingRoom(persons);		
		virtualVehicleType.setCapacity(cap);
		PTvehicles.addVehicleType(virtualVehicleType);

		// ------------------- add connections to network and schedule -----------------------
		while ((curr_line = bufRdr.readLine()) != null) {
			String[] entries = curr_line.split(";");
			String bezirkName = entries[0].trim();
			String bezirkNummer = entries[1].trim();
			String bezirkXAchse = entries[2].trim();
			String bezirkYAchse = entries[3].trim();
			String knotenNummer = entries[4].trim();
			String knotenXAchse = entries[5].trim();
			String knotenYAchse = entries[6].trim();
			String stadtnummer = entries[7].trim();
			String distanzLuft = entries[8].trim();
			//log.info("Bezirkname = " +bezirkName+ ", Bezirknummer = " +bezirkNummer+
			//		", BezirkXAchse = " +bezirkXAchse+ ", Knotennummer = "
			//		+knotenNummer);

			Id<Node> bezirkNodeId = Id.create(bezirkNummer+knotenNummer, Node.class);

			// add district as a node to network
			double x = Double.parseDouble(bezirkXAchse);
			double y = Double.parseDouble(bezirkYAchse);
			Coord coords = sc.createCoord(x, y);
			Node bezirkNode = factory.createNode(bezirkNodeId, coords);
			PTnetwork.addNode(bezirkNode);

			bezirkList.add(Arrays.asList(bezirkNummer+knotenNummer,stadtnummer));	

			//log.info("bezirkNode " +bezirkNode.getId()+ " added to network");

			// create district as a stop facility to schedule
			TransitStopFacility bezirkStop = scheduleFactory.createTransitStopFacility(Id.create(bezirkNodeId, TransitStopFacility.class), coords, false);
			bezirkStop.setName(bezirkName);
			//log.info("stop facility " +bezirkStop.getName()+ " created");

			// add links between district and nodes to network
			double length = Math.round(Double.parseDouble(distanzLuft)*1000);
			double capLink = 99999.0;
			String linkType = "11";
			double freeSpeed = 1.3888888888888888;
			if (length != 0) {
				Id<Node> existingNodeId = Id.create(knotenNummer, Node.class);
				//log.info("existingNodeId: " +existingNodeId.toString());
				Node existingNode = PTnetwork.getNodes().get(existingNodeId);
				
				//only for connections:
//				PTnetworkAnbindungen.addNode(existingNode);
				
				Id<Link> newLinkId1 = Id.create(bezirkNummer+knotenNummer+1, Link.class);
				LinkImpl newLink1 = (LinkImpl) factory.createLink(newLinkId1, existingNode, bezirkNode);
				newLink1.setLength(length);
				newLink1.setCapacity(capLink);
				newLink1.setType(linkType);
				newLink1.setFreespeed(freeSpeed);
//				PTnetworkAnbindungen.addLink(newLink1);
				//log.info("link1 " +newLink1.getId()+ " added to network");

				Id<Link> newLinkId2 = Id.create(bezirkNummer+knotenNummer+2, Link.class);
				LinkImpl newLink2 = (LinkImpl) factory.createLink(newLinkId2, bezirkNode, existingNode);
				newLink2.setLength(length);
				newLink2.setCapacity(capLink);
				newLink2.setType(linkType);
				newLink2.setFreespeed(freeSpeed);
//				PTnetworkAnbindungen.addLink(newLink2);
				//log.info("link2 " +newLink2.getId()+ " added to network");


				// add lines between district and node to schedule
				double xNode = Double.parseDouble(knotenXAchse);
				double yNode = Double.parseDouble(knotenYAchse);
				Coord coordsNode = sc.createCoord(xNode, yNode);
				Id<TransitStopFacility> existingNodeId2 = Id.create(knotenNummer+bezirkNummer, TransitStopFacility.class);
				TransitStopFacility newNodeStop = scheduleFactory.createTransitStopFacility(existingNodeId2, coordsNode, false);
				newNodeStop.setName(bezirkName+"_Anbindung");

				// set reference link id and add to PTschedule
				newNodeStop.setLinkId(newLinkId1);
//				PTscheduleAnbindungen.addStopFacility(newNodeStop);
				log.info("stop facility " +newNodeStop+ " added to schedule");
				bezirkStop.setLinkId(newLinkId2);
//				PTscheduleAnbindungen.addStopFacility(bezirkStop);
				log.info("stop facility " +bezirkStop+ " added to schedule");

				List<Id<Link>> routeLinkIds1 = Arrays.asList(newLink1.getId());
				TransitRouteStop route1stop1 = scheduleFactory.createTransitRouteStop(bezirkStop, 0, 0);
				TransitRouteStop route1stop2 = scheduleFactory.createTransitRouteStop(newNodeStop, 300, 300);			
				List<TransitRouteStop> route1Stops = Arrays.asList(route1stop1, route1stop2);
				NetworkRoute newNetworkRoute1 = RouteUtils.createNetworkRoute(routeLinkIds1, PTnetwork);
				//				newNetworkRoute1.getLinkIds().remove(0);
				//				newNetworkRoute1.getLinkIds().add(newLink1.getId());
				Id<TransitRoute> newTransitRoute1Id = Id.create("Anbindung" +bezirkNummer+knotenNummer+1, TransitRoute.class);
				TransitRoute newTransitRoute1 = scheduleFactory.createTransitRoute(newTransitRoute1Id, newNetworkRoute1, route1Stops, "pt");
				log.info("transit route " +newTransitRoute1.getId()+ " has route stops: " +newTransitRoute1.getStops().toString());
				for (int i = 0; i<163; i++) {
					Id<Departure> depId = Id.create(i+knotenNummer+1, Departure.class);
					double time = (5*3600 + i*7*60);
					Departure dep = scheduleFactory.createDeparture(depId, time);
					Id<Vehicle> vehID = Id.create(bezirkNummer+knotenNummer+1+i, Vehicle.class);
					Vehicle virtualVehicle = vehicleFactory.createVehicle(vehID, virtualVehicleType);
					PTvehicles.addVehicle( virtualVehicle);
					dep.setVehicleId(vehID);
					newTransitRoute1.addDeparture(dep);
				}
				Id<TransitLine> newTransitLineId = Id.create("Anbindung" +bezirkNummer+knotenNummer+1, TransitLine.class);
				TransitLine newLine = scheduleFactory.createTransitLine(newTransitLineId);
				newLine.addRoute(newTransitRoute1);
//				PTscheduleAnbindungen.addTransitLine(newLine);
				//log.info("new line " +newLine.getId()+ " added to schedule");


				// opposite direction
				List<Id<Link>> routeLinkIds2 = Arrays.asList(newLink2.getId());
				TransitRouteStop route2stop1 = scheduleFactory.createTransitRouteStop(newNodeStop, 0, 0);
				TransitRouteStop route2stop2 = scheduleFactory.createTransitRouteStop(bezirkStop, 300, 300);			
				List<TransitRouteStop> route2Stops = Arrays.asList(route2stop1, route2stop2);
				NetworkRoute newNetworkRoute2 = RouteUtils.createNetworkRoute(routeLinkIds2, PTnetwork);
				//				newNetworkRoute2.getLinkIds().clear();
				//				newNetworkRoute2.getLinkIds().add(newLink2.getId());
				Id<TransitRoute> newTransitRoute2Id = Id.create("Anbindung" +bezirkNummer+knotenNummer+2, TransitRoute.class);
				TransitRoute newTransitRoute2 = scheduleFactory.createTransitRoute(newTransitRoute2Id, newNetworkRoute2, route2Stops, "pt");
				for (int i = 0; i<163; i++) {
					Id<Departure> depId2 = Id.create(i+knotenNummer+2, Departure.class);
					double time = (5*3600 + i*7*60);
					Departure dep2 = scheduleFactory.createDeparture(depId2, time);
					Id<Vehicle> vehID = Id.create(bezirkNummer+knotenNummer+2+i, Vehicle.class);
					Vehicle virtualVehicle = vehicleFactory.createVehicle(vehID, virtualVehicleType);
					PTvehicles.addVehicle( virtualVehicle);
					dep2.setVehicleId(vehID);
					newTransitRoute2.addDeparture(dep2);
				}
				Id<TransitLine> newTransitLineId2 = Id.create("Anbindung" +bezirkNummer+knotenNummer+2, TransitLine.class);
				TransitLine newLine2 = scheduleFactory.createTransitLine(newTransitLineId2);
				newLine2.addRoute(newTransitRoute2);
//				PTscheduleAnbindungen.addTransitLine(newLine2);
				//log.info("new line2 " +newLine2.getId()+ " added to schedule");
			}

		}
		bufRdr.close();
		log.info("End line iteration csv file");
		log.info("Network contains " +PTnetwork.getLinks().size()+ " links and " +PTnetwork.getNodes().size()+ " nodes.");
		log.info("Schedule contains " +PTschedule.getTransitLines().size()+ " lines.");
		log.info("Vehicle files contains " +PTvehicles.getVehicleTypes().size()+ " vehicle types and "
				+PTvehicles.getVehicles().size()+ " vehicles");
		//log.info("bezirkList has " +bezirkList.size()+ " rows");


		// add links and lines between districts
		log.info("Start adding links and lines between urban districts");
		List<String> doneList = new ArrayList<String>();
		for (List<String> arr1 : bezirkList) {
			doneList.add(arr1.get(0));
			int n = 0;
			Id<Node> currentNodeId1 = Id.create(arr1.get(0), Node.class);
			for (List<String> arr2 : bezirkList) {
				Id<Node> currentNodeId2 = Id.create(arr2.get(0), Node.class);
				if (arr2.get(1).equals(arr1.get(1)) && doneList.contains(arr2.get(0)) == false){ //&& currentNodeId1 != currentNodeId2 
					log.info("currentId1 is: " +currentNodeId1.toString()+ ", currentId2 is: " +currentNodeId2.toString());

					// add links
					Node currentNode1 = PTnetwork.getNodes().get(currentNodeId1);
					Node currentNode2 = PTnetwork.getNodes().get(currentNodeId2);
					Id<Link> nLinkId1 = Id.create(arr1.get(0)+1+n, Link.class);
					LinkImpl nLink1 = (LinkImpl) factory.createLink(nLinkId1, currentNode1, currentNode2);
					double dist = Math.round(CoordUtils.calcDistance(currentNode1.getCoord(), currentNode2.getCoord()));
					double capLink = 99999.0;
					String linkType = "11";
					double freeSpeed = 1.3888888888888888;

					nLink1.setLength(dist);
					nLink1.setCapacity(capLink);
					nLink1.setType(linkType);
					nLink1.setFreespeed(freeSpeed);
					PTnetwork.addLink(nLink1);
					//log.info("link1 " +nLink1+ " added to network");

					Id<Link> nLinkId2 = Id.create(arr1.get(0)+2+n, Link.class);
					n += 1;
					LinkImpl nLink2 = (LinkImpl) factory.createLink(nLinkId2, currentNode2, currentNode1);
					nLink2.setLength(dist);
					nLink2.setCapacity(capLink);
					nLink2.setType(linkType);
					nLink2.setFreespeed(freeSpeed);
					PTnetwork.addLink(nLink2);
					//log.info("link2 " +nLink2+ " added to network");

					// add lines
//					TransitStopFacility currentStop1 = PTschedule.getFacilities().get(currentNodeId1);
					Id<TransitStopFacility> nStopId1 = Id.create(currentNodeId1.toString()+currentNodeId2.toString(), TransitStopFacility.class);
					TransitStopFacility newddStop1 = scheduleFactory.createTransitStopFacility(nStopId1, currentNode1.getCoord(), false);
					newddStop1.setName(nStopId1+"_Verbindung");
					newddStop1.setLinkId(nLinkId1);
					PTschedule.addStopFacility(newddStop1);
					log.info("stop facility " +newddStop1+ " added to schedule");

//					TransitStopFacility currentStop2 = PTschedule.getFacilities().get(currentNodeId2);
					Id<TransitStopFacility> nStopId2 = Id.create(currentNodeId2.toString()+currentNodeId1.toString(), TransitStopFacility.class);
					TransitStopFacility newddStop2 = scheduleFactory.createTransitStopFacility(nStopId2, currentNode2.getCoord(), false);
					newddStop2.setName(nStopId2+"_Verbindung");
					newddStop2.setLinkId(nLinkId2);
					PTschedule.addStopFacility(newddStop2);
					log.info("stop facility " +newddStop2+ " added to schedule");

					List<Id<Link>> newRouteLinkIds1 = Arrays.asList(nLink1.getId());
					double speed = 20/3.6; // assumption: pt speed is 20 km/h
					double travelTime = Math.round(dist/speed);
					TransitRouteStop currentRoute1stop1 = scheduleFactory.createTransitRouteStop(newddStop1, 0, 0);
					TransitRouteStop currentRoute1stop2 = scheduleFactory.createTransitRouteStop(newddStop2, travelTime, travelTime);			
					List<TransitRouteStop> currentRoute1Stops = Arrays.asList(currentRoute1stop1, currentRoute1stop2);
					NetworkRoute nNetworkRoute1 = RouteUtils.createNetworkRoute(newRouteLinkIds1, PTnetwork);
					//    				nNetworkRoute1.getLinkIds().clear();
					//    				nNetworkRoute1.getLinkIds().add(nLink1.getId());
					Id<TransitRoute> nTransitRoute1Id = Id.create("Bezirkverbindung" +arr1.get(0)+arr2.get(0)+1, TransitRoute.class);
					TransitRoute nTransitRoute1 = scheduleFactory.createTransitRoute(nTransitRoute1Id, nNetworkRoute1, currentRoute1Stops, "pt");
					for (int i = 0; i<163; i++) {
						Id<Departure> depId = Id.create(i+arr1.get(0)+arr2.get(0)+1, Departure.class);
						double time = (5*3600 + i*7*60);
						Departure dep = scheduleFactory.createDeparture(depId, time);
						Id<Vehicle> vehID = Id.create(arr1.get(0)+arr2.get(0)+1+i, Vehicle.class);
						Vehicle virtualVehicle = vehicleFactory.createVehicle(vehID, virtualVehicleType);
						PTvehicles.addVehicle( virtualVehicle);
						dep.setVehicleId(vehID);
						nTransitRoute1.addDeparture(dep);
					}
					Id<TransitLine> nTransitLineId = Id.create("Bezirksverbindung1" +arr1.get(0)+arr2.get(0), TransitLine.class);
					TransitLine nLine = scheduleFactory.createTransitLine(nTransitLineId);
					nLine.addRoute(nTransitRoute1);
					PTschedule.addTransitLine(nLine);
					//log.info("new line " +nLine.getId()+ " added to schedule");

					List<Id<Link>> newRouteLinkIds2 = Arrays.asList(nLink2.getId());
					TransitRouteStop currentRoute2stop1 = scheduleFactory.createTransitRouteStop(newddStop2, 0, 0);
					TransitRouteStop currentRoute2stop2 = scheduleFactory.createTransitRouteStop(newddStop1, travelTime, travelTime);			
					List<TransitRouteStop> currentRoute2Stops = Arrays.asList(currentRoute2stop1, currentRoute2stop2);
					NetworkRoute nNetworkRoute2 = RouteUtils.createNetworkRoute(newRouteLinkIds2, PTnetwork);
					//    				nNetworkRoute2.getLinkIds().clear();
					//    				nNetworkRoute2.getLinkIds().add(nLink2.getId());
					Id<TransitRoute> nTransitRoute2Id = Id.create("Bezirkverbindung" +arr1.get(0)+arr2.get(0)+2, TransitRoute.class);
					TransitRoute nTransitRoute2 = scheduleFactory.createTransitRoute(nTransitRoute2Id, nNetworkRoute2, currentRoute2Stops, "pt");
					for (int i = 0; i<163; i++) {
						Id<Departure> depId = Id.create(i+arr1.get(0)+arr2.get(0)+2, Departure.class);
						double time = (5*3600 + i*7*60);
						Departure dep = scheduleFactory.createDeparture(depId, time);
						Id<Vehicle> vehID = Id.create(arr1.get(0)+arr2.get(0)+2+i, Vehicle.class);
						Vehicle virtualVehicle = vehicleFactory.createVehicle(vehID, virtualVehicleType);
						PTvehicles.addVehicle( virtualVehicle);
						dep.setVehicleId(vehID);
						nTransitRoute2.addDeparture(dep);
					}
					Id<TransitLine> nTransitLine2Id = Id.create("Bezirksverbindung2" +arr1.get(0)+arr2.get(0), TransitLine.class);
					TransitLine nLine2 = scheduleFactory.createTransitLine(nTransitLine2Id);
					nLine2.addRoute(nTransitRoute2);
					PTschedule.addTransitLine(nLine2);
					//log.info("new line " +nLine2.getId()+ " added to schedule");

				}
			}
		}
		log.info("End adding links and lines between urban districts");
		log.info("Network contains " +PTnetwork.getLinks().size()+ " links and " +PTnetwork.getNodes().size()+ " nodes.");
		log.info("Schedule contains " +PTschedule.getTransitLines().size()+ " lines.");
		log.info("Vehicle files contains " +PTvehicles.getVehicleTypes().size()+ " vehicle types and "
				+PTvehicles.getVehicles().size()+ " vehicles");

		// ------------------- write network and schedule files-----------------------
		Set<String> ptMode = new HashSet<String>();
		ptMode.add("pt");
		String PTMODE = "pt";

		for (Link link : PTnetwork.getLinks().values()) {
			link.setAllowedModes(ptMode);
		}

		for (TransitLine line : PTschedule.getTransitLines().values()){
			for (TransitRoute route : line.getRoutes().values()) {
				route.setTransportMode(PTMODE);
			}
		}

		NetworkWriter nw = new NetworkWriter(PTnetwork);
		nw.write("./output/uvek2030network_anbindungen_routes.xml");

		TransitScheduleWriter sw = new TransitScheduleWriter(PTschedule);
		sw.writeFile("./output/uvek2030schedule_anbindungen_routes.xml");

//		VehicleWriterV1 vw = new VehicleWriterV1(PTvehicles);
//		vw.writeFile("./output/uvek2030vehicles_final.xml");
	}
}

