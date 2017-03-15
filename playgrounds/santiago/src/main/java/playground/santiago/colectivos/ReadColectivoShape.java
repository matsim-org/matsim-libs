package playground.santiago.colectivos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import tutorial.programming.example21tutorialTUBclass.transit.TransitScheduleModifier;



public class ReadColectivoShape {
    private static Logger log = Logger.getLogger(ReadColectivoShape.class);
    private LeastCostPathCalculator lcp;
	private TransitSchedule transitSchedule;
	
	


	public static void main(String[] args) {
		new ReadColectivoShape().run();
		
	}
	
	private void run(){
		int numberLines=0;
		int countHigh =0;
		int countLow = 0;
		double longestDistance=0.0;
		double shortestDistance=100000.0;
		double totalDistance = 0.0;
		double intervall = 0.0;
		String shortestLine= "";
		String longestLine="";
		
		List<String> shortLinks = new ArrayList<>();
		Map<String,Double> distanceOfLines = new HashMap <String,Double>();
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile("C:/Users/Felix/Documents/Bachelor/Santiago de Chile/santiago/scenario/inputForMATSim/network/network_merged_cl.xml.gz");

//		Filtering car network
		NetworkFilterManager m = new NetworkFilterManager(network);
		m.addLinkFilter(new NetworkLinkFilter() {
			
			@Override
			public boolean judgeLink(Link l) {
				if (l.getAllowedModes().contains(TransportMode.car)) return true;
				else return false;
			}
		});
		
		Network network2 = m.applyFilters();
//		new NetworkWriter(network2).write("C:/Users/Felix/Documents/Bachelor/Santiago de Chile/network_car.xml.gz");
		
		//creating scenario		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.transitSchedule = scenario.getTransitSchedule();
		
		DijkstraFactory df = new DijkstraFactory();
		FreespeedTravelTimeAndDisutility fs =new FreespeedTravelTimeAndDisutility(-6, 7,-100);
		
		lcp =  df.createPathCalculator(network2, fs,fs);
		

//		reading Files
		Map<String,Geometry> lines = readShapeFileAndExtractGeometry("C:/Users/Felix/Documents/Bachelor/Santiago de Chile/Shape-Files/Rutas_Licitadas.shp");
//		Map<String,String> fleet = readExcelFileAndExtractFleet("C:/Users/Felix/Documents/Bachelor/Santiago de Chile/Shape-Files/Resumen_Geocodificacion.csv");
		Map<String,String> interval = readCSVFileAndExtractInterval("C:/Users/Felix/Documents/Bachelor/Santiago de Chile/v1/santiago/outputWithCollectivo(FC_SC1)/Traveltimes3(AddedEmptyLines).csv");
			System.out.println(interval);
		List xLinks = new ArrayList<>();
	
		
		
		for (Entry<String,Geometry> entry : lines.entrySet())
		{
			
			numberLines++;
			String linienName = entry.getKey();
//			if (linienName.equals("20") || linienName.equals("294")){
//				continue;
//			}
			
			for (Entry<String,String> entries : interval.entrySet()){
				
				if (linienName.equals(entries.getKey())){
					System.out.println(entries.getKey());
					System.out.println(entries.getValue());
					intervall = Double.parseDouble(entries.getValue());
				}
			}
			System.out.println(intervall);
			MultiLineString mls = (MultiLineString) entry.getValue();
			
			Coord start = new Coord (mls.getCoordinates()[0].x,mls.getCoordinates()[0].y);
				Node startNode = NetworkUtils.getNearestNode(network2, start);
			int l = mls.getCoordinates().length;
			Coord end = new Coord (mls.getCoordinates()[l-1].x, mls.getCoordinates()[l-1].y);
				Node endNode = NetworkUtils.getNearestNode(network2, end);
			
			double midCoordinatefinderA = 0;
			double midCoordinatefinderB = 0;
			int counter=0;
			int everyXlinks=8;
			double distance = 0;
			boolean containsDouble=true;
			List<Link> route = new ArrayList<>();
			List<Link> test = new ArrayList<>();
			
			
			Node lastNode = startNode;
			if (linienName.equals("179")|| linienName.equals("197")){
			everyXlinks=4;
			}
			if (linienName.equals("198")|| linienName.equals("218")){
				everyXlinks=9;
			}
			if (linienName.equals("20")|| linienName.equals("232")||linienName.equals("294")){
				everyXlinks=1;
			}
			
			
			while (containsDouble==true)//avoids that links are used more than once (loops)
			{
				distance = 0;
				everyXlinks++;
				test.clear();
				route.clear();
				counter = 0;
				midCoordinatefinderA=0;
				midCoordinatefinderB=0;
				
				for (Coordinate coord : mls.getCoordinates())
				{
					
					counter++;
					
					if (counter % everyXlinks == 0){
						
						//finding mid-coordinate, get nearest links and route to closest links, adds it to the colectivo route				
						if (midCoordinatefinderA > 0)
						{
							Coord midCoord = new Coord ((coord.x + midCoordinatefinderA )/2 , (coord.y + midCoordinatefinderB)/2);
	
							List<Link> closestLinks = getNearestNLinks(network2, midCoord, 5);
			
							List<Link> routeToClosestLink = findRouteToNextLink(midCoord, lastNode, closestLinks, network2);
							route.addAll(routeToClosestLink);
							int n = route.size()-1;
							if (n>0){
								lastNode = route.get(n).getToNode();
							}
						}
				
						midCoordinatefinderA = coord.x;
						midCoordinatefinderB = coord.y;
						
					}		
				}
			// adds route to last link
			List<Link> closestLinks = getNearestNLinks(network2, end, 2);
			List<Link> routeToClosestLink = findRouteToNextLink(end, lastNode, closestLinks, network2);
			
		
			route.addAll(routeToClosestLink);
			route.remove(0); //first link goes wrong way
				for (Link lllink : route)
				{
											
					if (test.contains(lllink))
					{
						containsDouble=true;
						break;
					}
						
					else
					{
						containsDouble=false;
					}
					
					test.add(lllink);		
				}	
				
				System.out.println("Linie: "+entry.getKey());
					
				//get distance of each route
				for (Link llink : route)
				{
					distance = distance + llink.getLength();
				}			
			}
			
			totalDistance= totalDistance + distance;
			//if distance < 1km line is not added
//			if (distance<1000.0){
//				shortLinks.add(linienName);
//				countLow++;
//				continue;
//			}
			countHigh++;
			addTransitRoute(linienName, route, intervall);
			
			//get longest and shortest distance
			if (distance > longestDistance){
				longestDistance = distance;
				longestLine = entry.getKey();
			}
			if (distance < shortestDistance){
				shortestDistance = distance;
				shortestLine = entry.getKey();
			}
			distanceOfLines.put(linienName, distance);
			xLinks.add(everyXlinks);
		}
		setFaresAndWriteFile(distanceOfLines, shortestDistance, longestDistance);
		
		//create vehicles, get the old vehicles- and schedule-file and add new colectivo vehicles and schedule
		createVehicles(scenario.getTransitSchedule(),scenario.getTransitVehicles());
					
		new VehicleReaderV1(scenario.getTransitVehicles()).readFile("C:/Users/Felix/Documents/Bachelor/Santiago de Chile/santiago/scenario/inputForMATSim/transit/old/transitvehicles.xml"); 
 		new TransitScheduleReader(scenario).readFile(				"C:/Users/Felix/Documents/Bachelor/Santiago de Chile/santiago/scenario/inputForMATSim/transit/old/transitschedule_simplified.xml");
 				
 		new TransitScheduleWriter(transitSchedule).writeFile(		 "C:/Users/Felix/Documents/Bachelor/Santiago de Chile/v3/scheduleFinal/schedule_all.xml");
 		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile("C:/Users/Felix/Documents/Bachelor/Santiago de Chile/v3/scheduleFinal/vehicles_all.xml");
// 		try{
// 		File file = new File(										 "C:/Users/Felix/Documents/Bachelor/Santiago de Chile/v3/scheduleTest/colectivo_statistics.xml");
// 		BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
//		bw.write("shortest distance:        "+shortestDistance);
//		bw.newLine();
//		bw.write("shortest line:            "+shortestLine);
//		bw.newLine();
//		bw.write("longest distance :        "+longestDistance);
//		bw.newLine();
//		bw.write("longest line:             "+longestLine);
//		bw.newLine();
//		bw.write("#valid lines (over 1km):  "+countHigh);
//		bw.newLine();
//		bw.write("#invalid lines (under 1km):"+countLow);
//		bw.newLine();
//		bw.write("total number of lines:     "+numberLines);
//		bw.newLine();
//		bw.write("total distance:            "+totalDistance);
//		bw.newLine();
//		
//		
//		for (int a=1; a<374; a++){
//			bw.write("|| Line:"+a+" everyXLink:"+xLinks.get(a));
//			}
//		bw.close();
// 		}
// 		catch (IOException e) {
//			e.printStackTrace();
//		}
 		System.out.println("shortest distance:        "+shortestDistance);
		System.out.println("shortest line:            "+shortestLine);
		System.out.println("longest distance :        "+longestDistance);
		System.out.println("longest line:             "+longestLine);
		System.out.println("#valid lines (over 1km):  "+countHigh);
		System.out.println("#invalid lines (under 1km):"+countLow);
		System.out.println("total number of lines:     "+numberLines);
		System.out.println("total distance:            "+totalDistance);
		for (String a : shortLinks){
			System.out.println("short link:"+a);
		}
		
		
	}			
	
	
////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////

	private void setFaresAndWriteFile (Map<String, Double> distanceOfLines, Double shortestDistance, Double longestDistance){
		
		for (Entry<String,Double> entry : distanceOfLines.entrySet()){
			double fare=0;
			String routeName = entry.getKey();
			Double distance = entry.getValue();
			
		try {

			
			fare= 200.0 + 2800.0 * (distance-shortestDistance) / (longestDistance-shortestDistance);
			fare = (int) Math.round(fare);
			File file = new File("C:/Users/Felix/Documents/Bachelor/Santiago de Chile/v3/scheduleTest/Fares.txt");
			
			// if file doesnt exists, then create it
//			if (!file.exists()) {
				file.createNewFile();
//			}

//			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
			bw.write(routeName+" ; "+fare);
			bw.newLine();
			bw.close();


		} catch (IOException e) {
			e.printStackTrace();
		}
		}
	}	
	
private Map<String, String> readShapeFileAndExtractFare(String filename) {
	Map<String,String> lines = new TreeMap<>();	
	for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {
				String lineNo = ft.getAttribute("SERVICIO").toString();
				String fare = String.valueOf(ft.getAttribute("TARIFA_D_N").toString());

				lines.put(lineNo, fare);
			

		 
	}	
	return lines;
	
	}

private void addTransitRoute(String routeName, List<Link> route, Double intervall) {
	
	NetworkRoute nr = new LinkNetworkRouteImpl(route.get(0).getId(),route.get(route.size()-1).getId());
	List<Id<Link>> linkIds = new ArrayList<>();
	for (int i = 1; i<route.size()-1;i++){
		linkIds.add(route.get(i).getId());
	}
		
	List<TransitRouteStop> stops = new ArrayList<>();
	
	for (int j=0; j< route.size();j++){
		TransitStopFacility stopf1 = transitSchedule.getFactory().createTransitStopFacility(Id.create(routeName+ "co_parada" +j, TransitStopFacility.class), route.get(j).getCoord(), false); 
		TransitRouteStop stop1 = transitSchedule.getFactory().createTransitRouteStop(stopf1, j*30, j*30);
		stops.add(stop1);
		this.transitSchedule.addStopFacility(stopf1);
		stopf1.setLinkId(route.get(j).getId());
	}
	
	nr.setLinkIds(route.get(0).getId(), linkIds, route.get(route.size()-1).getId());
	TransitRoute transitRoute = transitSchedule.getFactory().createTransitRoute(Id.create("co"+routeName, TransitRoute.class), nr, stops, "colectivo");
	
	TransitLine transitLine = transitSchedule.getFactory().createTransitLine(Id.create("co"+routeName, TransitLine.class));
	transitLine.addRoute(transitRoute);

	this.transitSchedule.addTransitLine(transitLine);
		if (intervall > 3.0){
		for (int k = 0; 18000.0+k*60*intervall < 79200.0 ; k++) {
		
			Departure dep = transitSchedule.getFactory().createDeparture(Id.create("dep"+k, Departure.class), 18000.0+k*60*intervall);
			transitRoute.addDeparture(dep);
		}
		}
		//generating different schedule for peak times
		else{
			int count=0;
			for (int k = 0; 18000.0+k*1.24*60*intervall < 25140.0 ; k++) {
				count++;
				Departure dep = transitSchedule.getFactory().createDeparture(Id.create("dep"+count, Departure.class), 18000.0+k*1.24*60*intervall);
				transitRoute.addDeparture(dep);
				
			}
			for (int k = 0; 25200.0+k*60*intervall < 32340.0 ; k++) {
				count++;
				Departure dep = transitSchedule.getFactory().createDeparture(Id.create("dep"+count, Departure.class), 25200.0+k*60*intervall);
				transitRoute.addDeparture(dep);
				
			}
			for (int k = 0; 32400.0+k*1.24*60*intervall < 64740.0 ; k++) {
				count++;
				Departure dep = transitSchedule.getFactory().createDeparture(Id.create("dep"+count, Departure.class), 32400.0+k*1.24*60*intervall);
				transitRoute.addDeparture(dep);
			}
			for (int k = 0; 64800.0+k*60*intervall < 71940.0 ; k++) {
				count++;
				Departure dep = transitSchedule.getFactory().createDeparture(Id.create("dep"+count, Departure.class), 64800.0+k*60*intervall);
				transitRoute.addDeparture(dep);
			}
			for (int k = 0; 72000.0+k*1.24*60*intervall < 79140.0 ; k++) {
				count++;
				Departure dep = transitSchedule.getFactory().createDeparture(Id.create("dep"+count, Departure.class), 72000.0+k*1.24*60*intervall);
				transitRoute.addDeparture(dep);
			}
			
			
		}
	}


private void createVehicles (TransitSchedule schedule, Vehicles vehicles) {

	VehiclesFactory vb = vehicles.getFactory();
	VehicleType vehicleType = vb.createVehicleType(Id.create("colectivo", VehicleType.class));
	VehicleCapacity capacity = new VehicleCapacityImpl();
	capacity.setSeats(Integer.valueOf(4));
	capacity.setStandingRoom(Integer.valueOf(0));
	vehicleType.setLength(4.0);
	vehicleType.setCapacity(capacity);
	vehicles.addVehicleType(vehicleType);
	
	long vehId = 1;
		for (TransitLine line : transitSchedule.getTransitLines().values()){ // this.schedule.getTransitLines().values()) {
			vehId=1;
			for (TransitRoute route : line.getRoutes().values()) {
	//			System.out.println("Line:      " + line);
	//			System.out.println("Route:     " + route);
				
				for (Departure departure : route.getDepartures().values()) {
						
	//				System.out.println("Departure: "+ departure );
					Vehicle veh = vb.createVehicle(Id.createVehicleId(route.getId() + "_" + vehId++), vehicleType);
	
					vehicles.addVehicle(veh);
					departure.setVehicleId(veh.getId());
					}
				}
			}
		}
		
	
	private List<Link> findRouteToNextLink(Coord lastCoord, Node lastNode, List<Link> closestLinks, Network network) {
		double closestDistance = Double.MAX_VALUE;
		List<Link> bestRoute = new ArrayList<>(); 
		for (Link l: closestLinks){
			List<Link> route = calcLinksRoute(lastNode.getId(), l.getId(), network); 
			double dist = calcRouteDistance(route);
			if (route.size()>0){
			if (dist<closestDistance){
				bestRoute = route;
				closestDistance = dist;
			} else if (dist == closestDistance){
				if (CoordUtils.calcEuclideanDistance(lastCoord, route.get(route.size()-1).getCoord())<CoordUtils.calcEuclideanDistance(lastCoord, bestRoute.get(route.size()-1).getCoord())){
					bestRoute = route;
					closestDistance = dist;	
				}
			}}
		}
		
		return bestRoute;
	}
	
	private Map<String,String> readExcelFileAndExtractFleet(String filename){
		Map<String,String> fleet = new TreeMap<>();
		
        String line;
        String cvsSplitBy = ";";

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] flotte = line.split(cvsSplitBy);
                fleet.put(flotte[10], flotte[4]);
                
            }
//            fleet.put(lineNo, flotte);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fleet;
	
	}
	
	private Map<String,String> readCSVFileAndExtractInterval(String filename){
		Map<String,String> interval = new TreeMap<>();
		
        String line;
        String cvsSplitBy = ";";

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            while ((line = br.readLine()) != null) {

                
                String [] interv = line.split(cvsSplitBy);
                interval.put(interv[0], interv[7]);
                
            }
//            fleet.put(lineNo, flotte);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return interval;
	
	}
	
	
	
	
	
	private Map<String,Geometry> readShapeFileAndExtractGeometry(String filename){
		
		Map<String,Geometry> lines = new TreeMap<>();	
		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {
			
				GeometryFactory geometryFactory= new GeometryFactory();
				WKTReader wktReader = new WKTReader(geometryFactory);

				try {
					Geometry geo = wktReader.read((ft.getAttribute("the_geom")).toString());
					String lineNo = ft.getAttribute("Name").toString();
					lines.put(lineNo, geo);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			 
		}	
		return lines;
	}

	/**
     * Finds the (approx.) nearest link to a given point on the map.<br />
     * It searches first for the nearest node, and then for the nearest link
     * originating or ending at that node.
     *
     * @param coord
     *          the coordinate for which the closest link should be found
     * @return the link found closest to coord
     */
    public static List<Link> getNearestNLinks(Network network, final Coord coord, int n) {
        if (!(network instanceof Network)) {
            throw new IllegalArgumentException("Only NetworkImpl can be queried like this.");
        }
        
        List<Link> nearestLinks = new ArrayList<>();
        Map<Id<Link>,Double> distances = new HashMap<>();
        for (Link l : network.getLinks().values()){
        	double distance = CoordUtils.calcEuclideanDistance(l.getCoord(), coord);
        	distances.put(l.getId(), distance);
        }
        Map<Id<Link>,Double> sortedDistances = sortByValue(distances);
        for (Entry<Id<Link>,Double> entry : sortedDistances.entrySet()){
        	if (nearestLinks.size()<=n){
        		nearestLinks.add(network.getLinks().get(entry.getKey()));
        	} else break;
        }
        
        return nearestLinks;
    }
    
    
    public static <K, V extends Comparable<? super V>> Map<K, V> 
    sortByValue( Map<K, V> map )
{
    List<Map.Entry<K, V>> list =
        new LinkedList<>( map.entrySet() );
    Collections.sort( list, new Comparator<Map.Entry<K,V>>()
    {
        @Override
        public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
        {
            return ( o1.getValue() ).compareTo( o2.getValue() );
        }
    } );

    Map<K, V> result = new LinkedHashMap<>();
    for (Map.Entry<K, V> entry : list)
    {
        result.put( entry.getKey(), entry.getValue() );
    }
    return result;
}
    

	private List<Link> calcLinksRoute(Id<Node> fromNodeId, Id<Link> tolink, Network network ) {
		
		List<Link> route = null;
		log.info("from: "+fromNodeId+" to: "+tolink);
		try{
		Node fromNode = network.getNodes().get(fromNodeId);
		Node toNode = network.getLinks().get(tolink).getToNode();
//		log.info("calculating from "+fromNode+" to "+toNode);
		
		
		route = lcp.calcLeastCostPath(fromNode, toNode, 6.0, null, null).links;
		}
		catch (NullPointerException e){
			log.error("could not get toNode for "+fromNodeId + " or fromNode for "+tolink );
			e.printStackTrace();
			
			
		}
		return route;
		
	}
	private	double calcRouteDistance(List<Link> links){
		double dist = 0.0;
		for (Link l:links){
			dist+=l.getLength();
		}
		return dist;
	}



	
}


