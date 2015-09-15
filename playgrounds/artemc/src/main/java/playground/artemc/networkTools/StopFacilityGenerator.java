package playground.artemc.networkTools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

public class StopFacilityGenerator {

	private String serviceInterval="00:05:00";
	
	private Integer stopDistance;
	private Double initialStopOffset = 10.0;	
	private Double sideOffset = 5.0;
	private Set<String> allowedModes = new HashSet<String>();
	private ScenarioImpl sc;	

	private HashMap<Id, List<Id>> removedLinks =  new HashMap<Id, List<Id>>(); 
	private HashMap<Id, Id> linksWithStops =  new HashMap<Id, Id>(); 
	private HashMap<Id, Double> stopOffsets =  new HashMap<Id, Double>(); 

	private TransitScheduleFactory transitScheduleFactory = new TransitScheduleFactoryImpl();
	private TransitSchedule transitSchedule = transitScheduleFactory.createTransitSchedule();
	
	TransitStopFacility lastStopFacility = null;

	private NetworkImpl network;

	public NetworkImpl getNetwork() {
		return network;
	}

	public TransitSchedule getTransitSchedule() {
		return transitSchedule;
	}

	//Main method
	/**
	 * @param args:
	 * 0 - Input network file
	 * 1 - Output network file
	 * 2 - Name of the network
	 * 3 - Output transit schedule file 
	 * 4 - Output vehicles files
	 * @throws IOException 
	 * @throws Exception 
	 */

	public static void main(String[] args) throws IOException {

		//		String distance = args[1];
		//		try { 
		//			stopDistance = Integer.parseInt(distance); 
		//		} 
		//		catch(NumberFormatException nFE) { 
		//			System.out.println("The 2nd argument has to be an integer (= Meters betweed bus stops");
		//		}

		String inputNetwork = args[0];
		String inputRouteData = args[1];
		String outputNetwork = args[2];
		String outputNetworkName = args[3];
		String outputTransitSchedule = args[4];
		String outputVehicles = args[5];

		String inputLinkMap = null;

		if(args.length==7){
			inputLinkMap = args[6];
		}
		
		ArrayList<String[]> currentRoute;	
		ArrayList<String[]> linkMapLines;
		HashMap<String,ArrayList<String>> linkMap = new HashMap<String,ArrayList<String>>();

		/*Create transit stops, routes and modify network*/
		StopFacilityGenerator stopFacilityGenerator = new StopFacilityGenerator(inputNetwork, 600);
		currentRoute = stopFacilityGenerator.readCSV(inputRouteData);

		if(inputLinkMap==null){
			for(String[] route:currentRoute){
				System.out.println(route[0]);
				String[] routeLinks = new String[route.length-2];
				for(int i=0;i<routeLinks.length;i++)
				{
					routeLinks[i]=route[i+2];
				}
				stopFacilityGenerator.generateNewTransitLine(route[0],route[1],routeLinks);	
			}
		}
		else{
			linkMapLines = stopFacilityGenerator.readCSV(inputLinkMap);
			for(String[] l:linkMapLines){
				linkMap.put(l[0], new ArrayList<String>());
				for(int linkNo=1;linkNo<l.length;linkNo++){
					linkMap.get(l[0]).add(l[linkNo]);
				}
			}

			for(String[] route:currentRoute){
				System.out.println(route[0]);
				ArrayList<String> routeLinksArray = new ArrayList<String>();
				for(int i=0;i<route.length-2;i++)
				{
					for(String newRouteLink:linkMap.get(route[i+2])){
						routeLinksArray.add(newRouteLink);
					}
				}
				String[] routeLinks = new String[routeLinksArray.size()];
				for(int i=0;i<routeLinksArray.size();i++){
					routeLinks[i]=routeLinksArray.get(i);
				}
				System.out.println("Generation Route: "+route[0]+","+route[1]);
				stopFacilityGenerator.generateNewTransitLine(route[0],route[1],routeLinks);	
			}
		}

		/*Create vehicles*/
		stopFacilityGenerator.createVehicles(stopFacilityGenerator.getTransitSchedule());

		/*Write created transit schedule and vehicle files*/
		new TransitScheduleWriter(stopFacilityGenerator.getTransitSchedule()).writeFile(outputTransitSchedule);
		new VehicleWriterV1(stopFacilityGenerator.sc.getTransitVehicles()).writeFile(outputVehicles);

		/*Remove old links before writing*/
		System.out.print("DELETING LINKS: ");
		for(Id linkToDelete:stopFacilityGenerator.removedLinks.keySet()){
			System.out.print(linkToDelete+",");
			stopFacilityGenerator.getNetwork().removeLink(linkToDelete);
		}

		/*Write modified network*/
		stopFacilityGenerator.getNetwork().setName(outputNetworkName);
		NetworkWriter networkWriter =  new NetworkWriter(stopFacilityGenerator.getNetwork());
		networkWriter.write(outputNetwork);		
	}	

	private void createVehicles(TransitSchedule ts) {
		Vehicles vehicles = this.sc.getTransitVehicles();
		VehiclesFactory vehicleFactory = vehicles.getFactory();
		VehicleType standardBus = vehicleFactory.createVehicleType(Id.create("Bus MAN NL323F", VehicleType.class));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(38));
		capacity.setStandingRoom(Integer.valueOf(52));
		standardBus.setCapacity(capacity);
		standardBus.setAccessTime(1.0);
		standardBus.setEgressTime(1.0);
		vehicles.addVehicleType(standardBus);


		for (TransitLine line : ts.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {					
					Vehicle veh = vehicleFactory.createVehicle(departure.getVehicleId(), standardBus);
					vehicles.addVehicle(veh);
				}
			}
		}		
	}

	public StopFacilityGenerator(String network, Integer distanceBetweenStops){
		this.sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		prepareConfig();
		new MatsimNetworkReader(sc).readFile(network);
		this.network = (NetworkImpl) sc.getNetwork();
		this.stopDistance = distanceBetweenStops;	
		this.allowedModes.add("car");
		this.allowedModes.add("pt");
		this.allowedModes.add("bus");
	}

	private void generateNewTransitLine(String transitLineName, String mode,  String[] routeLinks) {	

		List<Id<Link>> routeLinkIds = new ArrayList<Id<Link>>();
		for(String linkID:routeLinks){
			routeLinkIds.add(Id.create(linkID, Link.class));
		}

		double arrival = Time.parseTime("00:00:00");
		double departure = Time.parseTime("00:00:00");
		double difference =  Time.parseTime("00:01:00");
		double startTime = Time.parseTime("06:00:00");
		double endTime = Time.parseTime("23:00:00");
		double frequency = Time.parseTime(serviceInterval);

		Double stopOffset = initialStopOffset;

		TransitLine transitLine = transitScheduleFactory.createTransitLine(Id.create(transitLineName, TransitLine.class));
		transitSchedule.addTransitLine(transitLine);

		List<Node> nodes = new ArrayList<Node>();
		List<Link> links = new ArrayList<Link>();
		List<Link> newLinks = new ArrayList<Link>();			
		double restLinkLengthFromStop = 0.0;
		List<TransitRouteStop> transitRouteStops = new ArrayList<TransitRouteStop>();	

		/*Check if there is already a stop in the opposite direction for the first link and place first stop of the route opposite to it*/	
		if(lastStopFacility!=null){
			Id<Link> lastStopLink = lastStopFacility.getLinkId();
			Id<Node> toNode = network.getLinks().get(lastStopLink).getToNode().getId();
			
			double x_diff = (network.getLinks().get(lastStopLink).getToNode().getCoord().getX() - network.getLinks().get(lastStopLink).getFromNode().getCoord().getX());
			double y_diff = (network.getLinks().get(lastStopLink).getToNode().getCoord().getY() - network.getLinks().get(lastStopLink).getFromNode().getCoord().getY());
			double thetarad = Math.atan2(y_diff, x_diff);
			
			double x_diff2 = (network.getLinks().get(routeLinkIds.get(0)).getFromNode().getCoord().getX() - network.getLinks().get(routeLinkIds.get(0)).getToNode().getCoord().getX());
			double y_diff2 = (network.getLinks().get(routeLinkIds.get(0)).getFromNode().getCoord().getY() - network.getLinks().get(routeLinkIds.get(0)).getToNode().getCoord().getY());
			double thetarad2 = Math.atan2(y_diff2, x_diff2);
			
			if(Math.abs(thetarad - thetarad2)<Math.PI/6 && toNode.toString().equals(network.getLinks().get(routeLinkIds.get(0)).getFromNode().getId().toString())){
				x_diff = network.getLinks().get(lastStopLink).getToNode().getCoord().getX() - lastStopFacility.getCoord().getX();
				y_diff = network.getLinks().get(lastStopLink).getToNode().getCoord().getY() - lastStopFacility.getCoord().getY();
				stopOffset  = Math.sqrt((x_diff*x_diff)+(y_diff*y_diff));	
				System.out.println("!!!! LINE IN THE OPPOSITE DIRECTION DETECTED !!!");
			}
			
		}
		
		
		/*Loop through links of the route*/
		for(Id linkId:routeLinkIds){
			List<Link> linksToEdit = new ArrayList<Link>();
			Link originalLink = network.getLinks().get(linkId);

			boolean linkAlreadyRemoved = false;

			if(removedLinks.containsKey(originalLink.getId())){
				linkAlreadyRemoved = true;
				for(Id l:removedLinks.get(originalLink)){
					linksToEdit.add(network.getLinks().get(l));
				}
			}
			else{
				removedLinks.put(originalLink.getId(),new ArrayList<Id>());
				linksToEdit.add(originalLink);
			}

			for(Link linkToEdit:linksToEdit){

				System.out.print(" Link to edit: "+linkToEdit.getId().toString());
				/*Check for link already containing a stop*/

				if(linksWithStops.containsKey(linkToEdit.getId())){
					System.out.println("Stop at already exist at link: "+(linkToEdit.getId()));
				} 

				
				/*Check if link for the new stop already contains a stop facility*/
				if(linksWithStops.containsKey(linkToEdit.getId()) && stopOffset<=linkToEdit.getLength()){
					arrival = arrival + difference;
					departure = departure + difference;					
					transitRouteStops.add(transitScheduleFactory.createTransitRouteStop(transitSchedule.getFacilities().get(linksWithStops.get(linkToEdit.getId())),arrival,departure));
					stopOffset = stopDistance - (linkToEdit.getLength() - stopOffsets.get(linkToEdit.getId()));
					links.add(linkToEdit);
					if(removedLinks.containsKey(linkToEdit.getId())){
						removedLinks.remove(linkToEdit.getId());
					}
				}
				else{

					//Link length on map
					double x_diff = (linkToEdit.getToNode().getCoord().getX() - linkToEdit.getFromNode().getCoord().getX());
					double y_diff = (linkToEdit.getToNode().getCoord().getY() - linkToEdit.getFromNode().getCoord().getY());
					double totalLinkLengthOnMap = Math.sqrt((x_diff*x_diff)+(y_diff*y_diff));		
					
					System.out.println("    Link length: "+linkToEdit.getLength()+"   Distance on Map: "+totalLinkLengthOnMap);
					
					Coord newNodeXY = null;
					Id<Link> newLinkId = null;

					nodes.add(linkToEdit.getFromNode());
					//newNumberOfLinks = (int) Math.ceil(_(originalLink.getLength() - stopOffset)/stopDistance);

					
					if(stopOffset<=linkToEdit.getLength()){
						while(stopOffset<=linkToEdit.getLength()){
							restLinkLengthFromStop = linkToEdit.getLength() - stopOffset;
							
							//Check for need to split the link in order to add more bus stops
							if(restLinkLengthFromStop>stopDistance){
							
								//Length of the new link
								double nodeDistanceOnMap = totalLinkLengthOnMap * ((stopOffset+stopDistance)/linkToEdit.getLength());   				
								//double nodeDistanceOnMap = (totalLinkLengthOnMap/newNumberOfLinks) * nodes.size();  

								//Create new node
								int c=0;
								Id<Node> newNodeId = null;
								do{
									newNodeId = Id.create(linkToEdit.getFromNode().getId().toString()+"_"+(nodes.size()+c), Node.class);
									c++;
								}while(network.getNodes().containsKey(newNodeId));

								newNodeXY = convertDistanceToCoordinates(linkToEdit,nodeDistanceOnMap);	
								network.createAndAddNode(newNodeId, newNodeXY);
								nodes.add(network.getNodes().get(newNodeId));

								//Create new Link
								newLinkId = Id.create(linkToEdit.getId().toString()+"_"+(nodes.size()-1), Link.class);									
								network.createAndAddLink(newLinkId, nodes.get(nodes.size()-2), nodes.get(nodes.size()-1), stopDistance, linkToEdit.getFreespeed(), linkToEdit.getCapacity(), linkToEdit.getNumberOfLanes());
								network.getLinks().get(newLinkId).setAllowedModes(allowedModes);
								links.add(network.getLinks().get(newLinkId));
								removedLinks.get(linkToEdit.getId()).add(newLinkId);
								System.out.println("    ADDING Link: "+newLinkId.toString());
							}	
							else{
								
								//Check if new links have been created
								if(nodes.size()>1){
									//Create last new Link
									newLinkId = Id.create(linkToEdit.getId().toString()+"_"+(nodes.size()), Link.class);									
									network.createAndAddLink(newLinkId, nodes.get(nodes.size()-1), linkToEdit.getToNode(), restLinkLengthFromStop, linkToEdit.getFreespeed(), linkToEdit.getCapacity(), linkToEdit.getNumberOfLanes());
									network.getLinks().get(newLinkId).setAllowedModes(allowedModes);									
									links.add(network.getLinks().get(newLinkId));	
									removedLinks.get(linkToEdit.getId()).add(newLinkId);
									System.out.println("    ADDING LAST Link: "+newLinkId.toString());
								}
								else{
									links.add(linkToEdit);
									network.getLinks().get(linkToEdit.getId()).setAllowedModes(allowedModes);
									System.out.println("    Keeping Link: "+linkToEdit.getId().toString());
									removedLinks.remove(linkToEdit.getId());
								}	
								nodes.clear();
							}

							/*Distance of the stop from the node on the map*/
							double stopDistanceFromNodeOnMap = totalLinkLengthOnMap * (stopOffset/linkToEdit.getLength());
							System.out.println("Stop Distance From Last Node On Map: "+stopDistanceFromNodeOnMap);

							Coord newStopXY = convertDistanceToCoordinates(linkToEdit,stopDistanceFromNodeOnMap);
							TransitStopFacility stopFacility = createNewStop(newStopXY.getX(), newStopXY.getY(), links.get(links.size()-1).getId());
							lastStopFacility = stopFacility;
							
							linksWithStops.put(linkToEdit.getId(), stopFacility.getId());
							stopOffsets.put(stopFacility.getId(), stopOffset);

							arrival = arrival + difference;
							departure = departure + difference;					

							transitRouteStops.add(transitScheduleFactory.createTransitRouteStop(stopFacility,arrival,departure));

							System.out.println("Stop Nr. "+links.get(links.size()-1).getId()+"     X: "+transitSchedule.getFacilities().get(links.get(links.size()-1).getId()).getCoord().getX()+"  Y: "+transitSchedule.getFacilities().get(links.get(links.size()-1).getId()).getCoord().getY());

							stopOffset =  stopOffset + stopDistance;
						}

						//stopOffset = linkToEdit.getLength() - (stopOffset - stopDistance) ;
						stopOffset = stopOffset - linkToEdit.getLength();
					}
					else{
						links.add(linkToEdit);
						network.getLinks().get(linkToEdit.getId()).setAllowedModes(allowedModes);
						if(removedLinks.containsKey(linkToEdit.getId())){
							removedLinks.remove(linkToEdit.getId());
						}
						stopOffset = stopOffset - linkToEdit.getLength();
						nodes.clear();
					}

					System.out.println("RestLinkLength: "+(linkToEdit.getLength()-(stopDistance-stopOffset)));
					System.out.println("Offset due to previous stop location: "+stopOffset);

				}
			}
		}

		List<Id<Link>> newRouteLinkIds = new ArrayList<Id<Link>>();
		for(Link link:links){
			newRouteLinkIds.add(link.getId());		
		}


		for(Integer s=0;s<newRouteLinkIds.size();s++){
			System.out.print(newRouteLinkIds.get(s)+",");
		}
		System.out.println();

		/*Create transitRoute*/
		NetworkRoute networkRoute = new LinkNetworkRouteImpl(newRouteLinkIds.get(0), newRouteLinkIds.subList(1, links.size()-1), newRouteLinkIds.get(links.size()-1));				
		String routeName = transitRouteStops.get(0).getStopFacility().getLinkId().toString()+"to"+transitRouteStops.get(transitRouteStops.size()-1).getStopFacility().getLinkId().toString();
		TransitRoute transitRoute = transitScheduleFactory.createTransitRoute(Id.create(routeName, TransitRoute.class), networkRoute, transitRouteStops, "bus");
		transitRoute.setTransportMode(mode);
		transitLine.addRoute(transitRoute);

		int id = 1;
		for(Double time = startTime; time<endTime; time=time+frequency){
			Departure vehicleDeparture = transitScheduleFactory.createDeparture(Id.create(id, Departure.class), time);
			vehicleDeparture.setVehicleId(Id.create(mode+"_"+transitLineName+"_"+id, Vehicle.class));
			transitRoute.addDeparture(vehicleDeparture);
			id++;
		}

	}

	private Coord convertDistanceToCoordinates(Link link, double distance) {
		double x_diff = (link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX());
		double y_diff = (link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY());
		double thetarad = Math.atan2(y_diff, x_diff);
		//System.out.println("Theta: "+thetarad);
		double x = distance * Math.cos(thetarad) + sideOffset*Math.sin(thetarad);
		double y = distance * Math.sin(thetarad) + sideOffset*Math.cos(thetarad);

		Coord pointXY = new Coord(link.getFromNode().getCoord().getX() + x, link.getFromNode().getCoord().getY() + y);
		//System.out.println("      x: "+(link.getFromNode().getCoord().getX() + x)+" y: "+(link.getFromNode().getCoord().getY() + y));
		return pointXY;
	}

	private TransitStopFacility createNewStop(double x, double y,Id linkId) {
		Coord stopXY = new Coord(x, y);
		TransitStopFacility transitStopFacility = transitScheduleFactory.createTransitStopFacility(linkId, stopXY, false);
		transitStopFacility.setLinkId(linkId);
		transitStopFacility.setName("stopOn"+linkId.toString());
		transitSchedule.addStopFacility(transitStopFacility);
		return transitStopFacility;
	}

	public ArrayList<String[]> readCSV(String filePath) throws IOException{
		BufferedReader CSVFile = new BufferedReader(new FileReader(filePath));
		String dataRow = CSVFile.readLine(); 
		ArrayList<String[]> currentRoute = new ArrayList<String[]>();		
		while (dataRow != null){
			currentRoute.add(dataRow.substring(0, dataRow.length()).split(","));
			dataRow = CSVFile.readLine(); 
		}

		return currentRoute;
	}

	private void prepareConfig() {
		Config config = this.sc.getConfig();
		config.transit().setUseTransit(true);
		config.scenario().setUseVehicles(true);
	}
}
