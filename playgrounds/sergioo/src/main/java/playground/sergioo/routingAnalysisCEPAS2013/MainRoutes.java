package playground.sergioo.routingAnalysisCEPAS2013;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;
import playground.sergioo.typesPopulation2013.population.MatsimPopulationReader;
import playground.sergioo.visualizer2D2012.Layer;
import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainter;

public class MainRoutes {

	static class Trip implements Comparable<Trip> {
		final float travelTime;
		final float distance;
		final Id<ActivityFacility> origin;
		final Id<ActivityFacility> destination;
		final Time startTime;
		final double startLat;
		final double startLon;
		final double endLat;
		final double endLon;
		
		public Trip(float travelTime, float distance, String origin, String destination, Time startTime, double startLat, double startLon, double endLat, double endLon) {
			super();
			this.travelTime = travelTime;
			this.distance = distance;
			this.origin = Id.create(origin, ActivityFacility.class);
			this.destination = Id.create(destination, ActivityFacility.class);
			this.startTime = startTime;
			this.startLat = startLat;
			this.startLon = startLon;
			this.endLat = endLat;
			this.endLon = endLon;
		}
		@Override
		public int compareTo(Trip o) {
			return startTime.compareTo(o.startTime);
		}
		public double getDistance() {
			return MainRoutes.getDistance(startLat, startLon, endLat, endLon);
		}
	}
	static class Journey {
		PriorityQueue<Trip> trips = new PriorityQueue<Trip>();
	}
	
	static class NumComparator implements Comparator<Entry<String, Map<Journey, Integer>>> {

		@Override
		public int compare(Entry<String, Map<Journey, Integer>> o1, Entry<String, Map<Journey, Integer>> o2) {
			if(o1.getValue().size()>o2.getValue().size())
				return -1;
			if(o1.getValue().size()==o2.getValue().size())
				return 0;
			return 1;
		}
		
	}
	
	private static Map<String, Journey> allJourneys = new HashMap<String, Journey>();
	private static Set<String> stops = new HashSet<String>();
	private static Map<String, Map<Journey, Integer>> journeys = new HashMap<String, Map<Journey, Integer>>();
	static CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_UTM48N, TransformationFactory.WGS84);
	
	public static void process(DataBaseAdmin dba) throws SQLException, NoConnectionException {
		ResultSet allTrips = dba.executeQuery("SELECT JOURNEY_ID,BOARDING_STOP_STN,ALIGHTING_STOP_STN,Ride_Start_Time,Ride_Distance,Ride_Time,start_lat,start_lon,end_lat,end_lon FROM v2_trips12042011 ORDER BY JOURNEY_ID ASC");
		String prevJourney = "";
		Journey journey = null;
		while(allTrips.next()) {
			String currentJourney = allTrips.getString(1);
			if(!prevJourney.equals(currentJourney)) {
				if(journey!=null) {
					//allJourneys.put(prevJourney, journey);
					String key1 = journey.trips.peek().origin.toString(), key2 = getLast(journey.trips).destination.toString() ;
					if(!key1.contains("STN") && !key2.contains("STN") && !repeated(journey, key1, key2) && goodDistance(journey)) {
						Map<Journey, Integer> journeysT = journeys.get(key1+"-"+key2);
						if(journeysT==null)
							journeysT = new HashMap<Journey, Integer>();
						Journey journey2 = differentJourney(journeysT, journey); 
						if(journey2 == null)
							journeysT.put(journey, 1);
						else
							journeysT.put(journey2,journeysT.get(journey2)+1);
						journeys.put(key1+"-"+key2, journeysT);
					}
				}
				journey = new Journey();	
				prevJourney = currentJourney;
			}
			if(!allTrips.getString(2).startsWith("STN"))
				stops.add(allTrips.getString(2));
			if(!allTrips.getString(3).startsWith("STN"))
				stops.add(allTrips.getString(3));
			journey.trips.add(new Trip(allTrips.getFloat(6), allTrips.getFloat(5), allTrips.getString(2), allTrips.getString(3), allTrips.getTime(4),
					allTrips.getDouble(7), allTrips.getDouble(8), allTrips.getDouble(9), allTrips.getDouble(10)));
		}
		allTrips.close();
	}
	public static double getDistance(double startLat, double startLon, double endLat, double endLon) {
		return Math.hypot(endLat-startLat, endLon-startLon);
	}
	static boolean goodDistance(Journey journey) {
		List<Trip> listTrips = new ArrayList<Trip>(journey.trips);
		Trip first = listTrips.get(0), last = listTrips.get(listTrips.size()-1);
		double sum=0;
		for(Trip trip:journey.trips)
			sum += trip.getDistance();
		return sum<4*getDistance(first.startLat, first.startLon, last.endLat, last.endLon);
	}
	static boolean repeated(Journey journey, String key1, String key2) {
		int kone = 0, ktwo = 0;
		for(Trip trip:journey.trips)
			if(trip.origin.toString().equals(key1) || trip.destination.toString().equals(key1))
				kone++;
			else if(trip.origin.toString().equals(key2) || trip.destination.toString().equals(key2))
				ktwo++;
		return kone>1 || ktwo>1;
	}
	static Journey differentJourney(Map<Journey, Integer> journeys, Journey journey) {
		for(Journey oneJourney:journeys.keySet())
			if(same(oneJourney, journey))
				return oneJourney;
		return null;
	}
	private static boolean same(Journey oneJourney, Journey journey) {
		if(oneJourney.trips.size()==journey.trips.size()) {
			Iterator<Trip> one = oneJourney.trips.iterator(), two = journey.trips.iterator(); 
			while(one.hasNext())
				if(!one.next().origin.equals(two.next().origin))
					return false;
			return true;
		}
		return false;
	}
	public static int getNumJourneys() {
		return allJourneys.size();
	}
	static Trip getLast(PriorityQueue<Trip> trips) {
		Iterator<Trip> it = trips.iterator();
		Trip res = null;
		while(it.hasNext())
			res = it.next();
		return res;
	}

	protected static float getDistance(Network network, TransitSchedule transitSchedule, String[] parts) {
		Id<Link> fromLinkId = transitSchedule.getFacilities().get(Id.create(parts[1], ActivityFacility.class)).getLinkId();
		Id<Link> toLinkId = transitSchedule.getFacilities().get(Id.create(parts[4], ActivityFacility.class)).getLinkId();
		return (float)RouteUtils.calcDistanceExcludingStartEndLink(transitSchedule.getTransitLines().get(Id.create(parts[2], TransitLine.class)).getRoutes().get(Id.create(parts[3], TransitRoute.class)).getRoute().getSubRoute(fromLinkId, toLinkId), network);
	}
	
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		final int index = 1;
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/CEPASDataBase.properties"));
		process(dba);
		System.out.println(getNumJourneys());
		final PriorityQueue<Entry<String, Map<Journey, Integer>>> numSorted = new PriorityQueue<Entry<String, Map<Journey, Integer>>>(journeys.size(), new NumComparator());
		for(Entry<String, Map<Journey, Integer>> numTrip:journeys.entrySet())
			numSorted.add(numTrip);
		final List<Entry<String, Map<Journey, Integer>>> list = new ArrayList<Map.Entry<String,Map<Journey,Integer>>>(numSorted);
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig()), scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(args[0]);
		for(Node node:scenario.getNetwork().getNodes().values())
			((NodeImpl)node).setCoord(transformation.transform(node.getCoord()));
		new MatsimNetworkReader(scenario2.getNetwork()).readFile(args[1]);
		for(Node node:scenario2.getNetwork().getNodes().values())
			((NodeImpl)node).setCoord(transformation.transform(node.getCoord()));
		LayersPanel panel = new LayersPanel() {
			{
				addLayer(new Layer(new NetworkPainter(scenario2.getNetwork())));
				addLayer(new Layer(new RoutesPainter(list.get(index).getValue())));
				double xMin = Double.MAX_VALUE, yMin = Double.MAX_VALUE, xMax = -Double.MAX_VALUE, yMax = -Double.MAX_VALUE;
				for(Journey journey:list.get(index).getValue().keySet())
					for(Trip trip:journey.trips) {
						double x=trip.startLon, y=trip.startLat;
						if(x<xMin)
							xMin = x;
						if(y<yMin)
							yMin = y;
						if(x>xMax)
							xMax = x;
						if(y>yMax)
							yMax = y;
						x=trip.endLon; y=trip.endLat;
						if(x<xMin)
							xMin = x;
						if(y<yMin)
							yMin = y;
						if(x>xMax)
							xMax = x;
						if(y>yMax)
							yMax = y;
					}
				Collection<double[]> bounds = new ArrayList<double[]>();
				double border = (xMax-xMin)/20;
				bounds.add(new double[]{xMin-border, yMax+border});
				bounds.add(new double[]{xMax+border, yMin-border});
				calculateBoundaries(bounds);
			}
		};
		RoutesWindow window = new RoutesWindow(panel);
		window.setVisible(true);
		scenario.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile(args[2]);
		RoutesPopulation routesPopulation = new RoutesPopulation(scenario, list.get(index).getKey());
		((PopulationImpl)scenario.getPopulation()).setIsStreaming(true);
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(routesPopulation);
		new MatsimPopulationReader(scenario).readFile(args[3]);
		final Map<Journey, Integer> journeysPlan = routesPopulation.getJourneyPlan();
		LayersPanel panel2 = new LayersPanel() {
			{
				addLayer(new Layer(new NetworkPainter(scenario2.getNetwork())));
				addLayer(new Layer(new RoutesPainter(journeysPlan)));
				double xMin = Double.MAX_VALUE, yMin = Double.MAX_VALUE, xMax = -Double.MAX_VALUE, yMax = -Double.MAX_VALUE;
				for(Journey journey:list.get(index).getValue().keySet())
					for(Trip trip:journey.trips) {
						double x=trip.startLon, y=trip.startLat;
						if(x<xMin)
							xMin = x;
						if(y<yMin)
							yMin = y;
						if(x>xMax)
							xMax = x;
						if(y>yMax)
							yMax = y;
						x=trip.endLon; y=trip.endLat;
						if(x<xMin)
							xMin = x;
						if(y<yMin)
							yMin = y;
						if(x>xMax)
							xMax = x;
						if(y>yMax)
							yMax = y;
					}
				Collection<double[]> bounds = new ArrayList<double[]>();
				double border = (xMax-xMin)/20;
				bounds.add(new double[]{xMin-border, yMax+border});
				bounds.add(new double[]{xMax+border, yMin-border});
				calculateBoundaries(bounds);
			}
		};
		RoutesWindow window2 = new RoutesWindow(panel2);
		window2.setVisible(true);
		routesPopulation = new RoutesPopulation(scenario, list.get(index).getKey());
		((PopulationImpl)scenario2.getPopulation()).setIsStreaming(true);
		((PopulationImpl)scenario2.getPopulation()).addAlgorithm(routesPopulation);
		new MatsimPopulationReader(scenario2).readFile(args[4]);
		final Map<Journey, Integer> journeysPlan2 = routesPopulation.getJourneyPlan();
		LayersPanel panel3 = new LayersPanel() {
			{
				addLayer(new Layer(new NetworkPainter(scenario2.getNetwork())));
				addLayer(new Layer(new RoutesPainter(journeysPlan2)));
				double xMin = Double.MAX_VALUE, yMin = Double.MAX_VALUE, xMax = -Double.MAX_VALUE, yMax = -Double.MAX_VALUE;
				for(Journey journey:list.get(index).getValue().keySet())
					for(Trip trip:journey.trips) {
						double x=trip.startLon, y=trip.startLat;
						if(x<xMin)
							xMin = x;
						if(y<yMin)
							yMin = y;
						if(x>xMax)
							xMax = x;
						if(y>yMax)
							yMax = y;
						x=trip.endLon; y=trip.endLat;
						if(x<xMin)
							xMin = x;
						if(y<yMin)
							yMin = y;
						if(x>xMax)
							xMax = x;
						if(y>yMax)
							yMax = y;
					}
				Collection<double[]> bounds = new ArrayList<double[]>();
				double border = (xMax-xMin)/20;
				bounds.add(new double[]{xMin-border, yMax+border});
				bounds.add(new double[]{xMax+border, yMin-border});
				calculateBoundaries(bounds);
			}
		};
		RoutesWindow window3 = new RoutesWindow(panel3);
		window3.setVisible(true);
		/*OriginDestinationMeasurement bestOD = null;
		int moreJourneys = 0;
		for(String stopA:stops)
			for(String stopB:stops)
				if(!stopA.equals(stopB)) {
					OriginDestinationMeasurement od = new OriginDestinationMeasurement(Id.create(stopA, TransitStopFacility.class), Id.create(stopB, TransitStopFacility.class));
					int num = od.count(allJourneys);
					if(num>moreJourneys) {
						moreJourneys = num;
						bestOD = od;
						System.out.println(stopA+"-"+stopB+": "+moreJourneys);
					}
				}
		System.out.println(bestOD);*/
	}
}
