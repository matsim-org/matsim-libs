package playground.sergioo.GTFS;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkFactoryImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.sergioo.GTFS.Route.RouteTypes;
import playground.sergioo.PathEditor.kernel.RoutesPathsGenerator;


public class GTFS2MATSimTransitScheduleFileWriter extends MatsimXmlWriter implements MatsimWriter {
	
	//Attributes
	/**
	 * The folder root of the GTFS files
	 */
	private File[] roots;
	/**
	 * The network where the public transport will be performed
	 */
	private Network network;
	/**
	 * The types of dates that will be represented by the new file
	 */
	private String[] serviceIds;
	/**
	 * The stops
	 */
	private Map<String, Stop>[] stops;
	/**
	 * The calendar sevices
	 */
	private Map<String, Service>[] services;
	/**
	 * The shapes
	 */
	private Map<String, Shape>[] shapes;
	/**
	 * The routes
	 */
	private SortedMap<String, Route>[] routes;
	/**
	 * The time format 
	 */
	private SimpleDateFormat timeFormat;
	
	//Methods
	/**
	 * 
	 * @param root
	 * @param network
	 */
	public GTFS2MATSimTransitScheduleFileWriter(File[] roots, Network network, String[] serviceIds) {
		super();
		this.roots = roots;
		this.network = network;
		this.serviceIds = serviceIds;
	}

	/**
	 * Changes the calendar references in the trips file
	 * @throws IOException
	 */
	public static void fixGTFSBusSingapore() throws IOException {
		File oldFile=new File("C:/Users/sergioo/Desktop/Desktop/buses/trips2.txt");
		File newFile=new File("C:/Users/sergioo/Desktop/Desktop/buses/trips.txt");
		BufferedReader reader = new BufferedReader(new FileReader(oldFile));
		PrintWriter writer = new PrintWriter(newFile);
		String line = reader.readLine();
		writer.println(line);
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			if(parts[1].endsWith("saturday"))
				parts[1]="saturday";
			else if(parts[1].endsWith("sunday"))
				parts[1]="sunday";
			else if(parts[1].endsWith("weekday"))
				parts[1]="weekday";
			writer.print(parts[0]);
			int i=1;
			for(;i<parts.length;i++)
				writer.print(","+parts[i]);
			for(;i<5;i++)
				writer.print(",");
			writer.println();
			line=reader.readLine();
		}
		writer.close();
		reader.close();
	}
	/**
	 * Erases the E and S routes
	 * @throws IOException
	 */
	public static void fixGTFSBusSingapore2() throws IOException {
		Collection<String> trips = new HashSet<String>();
		File oldFile=new File("./data/gtfs/buses/trips2.txt");
		File newFile=new File("./data/gtfs/buses/trips.txt");
		BufferedReader reader = new BufferedReader(new FileReader(oldFile));
		PrintWriter writer = new PrintWriter(newFile);
		String line = reader.readLine();
		writer.println(line);
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			if(!parts[2].endsWith("-S") && !parts[2].endsWith("-E")) {
				trips.add(parts[2]);
				writer.println(line);
			}
			else if(parts[2].endsWith("-E") && !trips.contains(parts[2].substring(0, parts[2].lastIndexOf('-')))) {
				String tripId = parts[2].substring(0, parts[2].lastIndexOf('-'));
				trips.add(tripId);
				writer.print(parts[0]+","+parts[1]+","+tripId);
				int i=3;
				for(;i<parts.length;i++)
					writer.print(","+parts[i]);
				for(;i<5;i++)
					writer.print(",");
				writer.println();
			}
			line=reader.readLine();
		}
		writer.close();
		reader.close();
		Map<String,String> startDepartures = new HashMap<String,String>();
		Map<String,String> endDepartures = new HashMap<String,String>();
		oldFile=new File("./data/gtfs/buses/stop_times2.txt");
		newFile=new File("./data/gtfs/buses/stop_times.txt");
		reader = new BufferedReader(new FileReader(oldFile));
		writer = new PrintWriter(newFile);
		line = reader.readLine();
		writer.println(line);
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			if(parts[0].endsWith("-S")) {
				String tripId = parts[0].substring(0, parts[0].lastIndexOf('-'));
				if(startDepartures.get(tripId)==null)
					startDepartures.put(tripId, parts[2]);
				writer.print(tripId);
				for(int i=1;i<parts.length;i++)
					writer.print(","+parts[i]);
				writer.println();
			}
			if(parts[0].endsWith("-E")) {
				String tripId = parts[0].substring(0, parts[0].lastIndexOf('-'));
				if(endDepartures.get(tripId)==null) {
					String[] parts3 = parts[2].split(":");
					int hour = Integer.parseInt(parts3[0]);
					if(hour<12) {
						hour+=24;
						endDepartures.put(tripId, Integer.toString(hour)+":"+parts3[1]+":"+parts3[2]);
					}
					else
						endDepartures.put(tripId, parts[2]);
				}
			}
			line=reader.readLine();
		}
		writer.close();
		reader.close();
		oldFile=new File("./data/gtfs/buses/frequencies2.txt");
		newFile=new File("./data/gtfs/buses/frequencies.txt");
		reader = new BufferedReader(new FileReader(oldFile));
		writer = new PrintWriter(newFile);
		line = reader.readLine();
		writer.println(line);
		String previous = line;
		line = reader.readLine();
		String[] parts = line.split(",");
		String tripId = parts[0];
		previous = parts[0]+","+startDepartures.get(parts[0]);
		for(int i=2;i<parts.length;i++)
			previous += ","+parts[i];
		line=reader.readLine();
		while(line!=null) {
			parts = line.split(",");
			if(!parts[0].equals(tripId)) {
				String[] parts2 = previous.split(",");
				for(int i=0;i<2;i++)
					writer.print(parts2[i]+",");
				writer.println(endDepartures.get(parts2[0])+","+parts2[3]);
				tripId = parts[0];
				previous = parts[0]+","+startDepartures.get(parts[0]);
				for(int i=2;i<parts.length;i++)
					previous += ","+parts[i];
			}
			else {
				writer.println(previous);
				previous = line;
			}
			line=reader.readLine();
		}
		String[] parts2 = previous.split(",");
		for(int i=0;i<2;i++)
			writer.print(parts2[i]+",");
		writer.println(endDepartures.get(parts2[0])+","+parts2[3]);
		writer.close();
		reader.close();
	}
	/**
	 * Changes the calendar references in the trips file
	 * @throws IOException
	 */
	public static void fixGTFSTrainSingapore() throws IOException {
		File oldFile=new File("C:/Users/sergioo/Documents/2011/Work/FCL/Operations/Data/GoogleTransitFeed/ProcessedData/Trains/trips2.txt");
		File newFile=new File("C:/Users/sergioo/Documents/2011/Work/FCL/Operations/Data/GoogleTransitFeed/ProcessedData/Trains/trips.txt");
		BufferedReader reader = new BufferedReader(new FileReader(oldFile));
		PrintWriter writer = new PrintWriter(newFile);
		String line = reader.readLine();
		writer.println(line);
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			if(parts[1].endsWith("weeksatday"))
				parts[1]="weeksatday";
			else if(parts[1].endsWith("sunday"))
				parts[1]="sunday";
			else if(parts[1].endsWith("weekday"))
				parts[1]="weekday";
			else if(parts[1].contains("daily"))
				parts[1]="daily";
			else
				System.out.println("Error");
			writer.print(parts[0]);
			int i=1;
			for(;i<parts.length;i++)
				writer.print(","+parts[i]);
			for(;i<5;i++)
				writer.print(",");
			writer.println();
			line=reader.readLine();
		}
		writer.close();
		reader.close();
	}
	/**
	 * Erases the E and S routes
	 * @throws IOException
	 */
	public static void fixGTFSTrainSingapore2() throws IOException {
		SortedMap<String,TripAux> trips = new TreeMap<String,TripAux>();
		File oldFile=new File("./data/gtfs/trains/trips2.txt");
		File newFile=new File("./data/gtfs/trains/trips.txt");
		BufferedReader reader = new BufferedReader(new FileReader(oldFile));
		PrintWriter writer = new PrintWriter(newFile);
		String line = reader.readLine();
		writer.println(line);
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			if(!parts[2].contains("_first") && !parts[2].contains("_last")) {
				TripAux tripAux = trips.get(parts[2]);
				if(tripAux==null) {
					trips.put(parts[2],new TripAux());
					tripAux = trips.get(parts[2]);
				}
				tripAux.setLine(line);
			}
			else if(parts[2].contains("_first")) {
				String tripId=parts[2].replaceAll("_first", "");
				TripAux tripAux = trips.get(tripId);
				if(tripAux==null) {
					tripAux = trips.put(parts[2],new TripAux());
					tripAux = trips.get(parts[2]);
					tripAux.setLine(line);
				}
				tripAux.addFirst(parts[2]);
			}
			else {
				String tripId=parts[2].replaceAll("_last", "");
				TripAux tripAux = trips.get(tripId);
				if(tripAux==null) {
					tripAux = trips.put(parts[2],new TripAux());
					tripAux = trips.get(parts[2]);
					tripAux.setLine(line);
				}
				tripAux.addLast(parts[2]);
			}
			line=reader.readLine();
		}
		for(Entry<String, TripAux> trip:trips.entrySet()) {
			writer.println(trip.getValue().getLine());
			System.out.println(trip.getKey()+" "+trip.getValue().getFirsts().size()+" "+trip.getValue().getLasts().size());
		}
		writer.close();
		reader.close();
		
		Map<String,String> startDepartures = new HashMap<String,String>();
		Map<String,String> endDepartures = new HashMap<String,String>();
		oldFile=new File("./data/gtfs/trains/stop_times2.txt");
		newFile=new File("./data/gtfs/trains/stop_times.txt");
		reader = new BufferedReader(new FileReader(oldFile));
		writer = new PrintWriter(newFile);
		line = reader.readLine();
		writer.println(line);
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			if(parts[0].contains("_first")) {
				String tripId = parts[0].replaceAll("_first", "");
				if(trips.containsKey(tripId)) {
					if(startDepartures.get(tripId)==null)
						startDepartures.put(tripId, parts[2]);
					writer.print(tripId);
					for(int i=1;i<parts.length;i++)
						writer.print(","+parts[i]);
					writer.println();
				}
			}
			else if(parts[0].contains("_last")) {
				String tripId = parts[0].replaceAll("_last", "");;
				if(trips.containsKey(tripId)) {
					if(endDepartures.get(tripId)==null)
						endDepartures.put(tripId, parts[2]);
				}
				else if(trips.containsKey(parts[0]))
					writer.println(line);
			}
			else {
				if(trips.get(parts[0]).getFirsts().size()==0)
					writer.println(line);
			}
			line=reader.readLine();
		}
		writer.close();
		reader.close();
		oldFile=new File("./data/gtfs/trains/frequencies2.txt");
		newFile=new File("./data/gtfs/trains/frequencies.txt");
		reader = new BufferedReader(new FileReader(oldFile));
		writer = new PrintWriter(newFile);
		line = reader.readLine();
		writer.println(line);
		String previous = line;
		line = reader.readLine();
		String[] parts = line.split(",");
		String tripId = parts[0];
		previous = parts[0]+","+startDepartures.get(parts[0]);
		for(int i=2;i<parts.length;i++)
			previous += ","+parts[i];
		line=reader.readLine();
		while(line!=null) {
			parts = line.split(",");
			if(!parts[0].equals(tripId)) {
				String[] parts2 = previous.split(",");
				if(endDepartures.get(parts2[0])!=null) {
					for(int i=0;i<2;i++)
						writer.print(parts2[i]+",");
					writer.println(endDepartures.get(parts2[0])+","+parts2[3]);
				}
				else
					writer.println(previous);
				tripId = parts[0];
				previous = parts[0]+","+startDepartures.get(parts[0]);
				for(int i=2;i<parts.length;i++)
					previous += ","+parts[i];
			}
			else {
				writer.println(previous);
				previous = line;
			}
			line=reader.readLine();
		}
		String[] parts2 = previous.split(",");
		if(endDepartures.get(parts2[0])!=null) {
			for(int i=0;i<2;i++)
				writer.print(parts2[i]+",");
			writer.println(endDepartures.get(parts2[0])+","+parts2[3]);
		}
		else
			writer.println(previous);
		writer.close();
		reader.close();
	}
	/**
	 * Loads all the GTFS information form the roots field
	 */
	private void loadGTFSFiles() {
		try {
			timeFormat = new SimpleDateFormat("HH:mm:ss");
			BufferedReader reader = null;
			String line = null;
			//Files load
			int size = roots.length;
			stops = new Map[size];
			services = new Map[size];
			shapes = new Map[size]; 
			routes = new SortedMap[size];
			int r=0;
			for(File root:roots) {
				stops[r]=new HashMap<String, Stop>();
				services[r]=new HashMap<String, Service>();
				shapes[r]=new HashMap<String, Shape>();
				routes[r]=new TreeMap<String, Route>();
				for(GTFSDefinitions gtfs:GTFSDefinitions.values()) {
					File file = new File(root.getPath()+"/"+gtfs.fileName);
					reader = new BufferedReader(new FileReader(file));
					int[] indices = gtfs.getIndices(reader.readLine());
					line = reader.readLine();
					while(line!=null) {
						String[] parts = line.split(",");
						Method m = GTFS2MATSimTransitScheduleFileWriter.class.getMethod(gtfs.getFunction(), new Class[] {String[].class,int[].class,int.class});
						m.invoke(this, new Object[]{parts,indices,r});
						line = reader.readLine();
					}
					reader.close();
				}
				r++;
			}
		} catch(IOException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		/*try {
			calculateShapePoints();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}*/
	}
	/*private void calculateShapePoints() throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(new File("./data/shapesDistance"));
		for(Route route:routes[0].values())
			for(Entry<String,Trip> tripE:route.getTrips().entrySet()) {
				Shape shape = tripE.getValue().getShape();
				if(shape!=null) {
					double avg = 0;
					SortedMap<Integer, Coord> ps = shape.getPoints();
					for(int i=1; i<ps.size(); i++)
						avg+=CoordUtils.calcDistance(ps.get(i), ps.get(i+1))*(6371000*Math.PI)/180;
					avg/=ps.size();
					pw.println(tripE.getKey()+" "+avg);
				}
			}
		pw.close();
	}*/

	/**
	 * Methods for processing one line of each file
	 * @param parts
	 * @param indices
	 */
	public void processStop(String[] parts, int[] indices, int r) {
		stops[r].put(parts[indices[0]],new Stop(new CoordImpl(Double.parseDouble(parts[indices[1]]),Double.parseDouble(parts[indices[2]])),parts[indices[3]],true));
	}
	public void processCalendar(String[] parts, int[] indices, int r) {
		boolean[] days = new boolean[7];
		for(int d=0; d<days.length; d++)
			days[d]=parts[d+indices[1]].equals("1");
		services[r].put(parts[indices[0]], new Service(days, parts[indices[2]], parts[indices[3]]));
	}
	public void processCalendarDate(String[] parts, int[] indices, int r) {
		Service actual = services[r].get(parts[indices[0]]);
		if(parts[indices[2]].equals("2"))
			actual.addException(parts[indices[1]]);
		else
			actual.addAddition(parts[indices[1]]);
	}
	public void processShape(String[] parts, int[] indices, int r) {
		Shape actual = shapes[r].get(parts[indices[0]]);
		if(actual==null) {
			actual = new Shape(parts[indices[0]]);
			shapes[r].put(parts[indices[0]], actual);
		}
		actual.addPoint(new CoordImpl(Double.parseDouble(parts[indices[1]]), Double.parseDouble(parts[indices[2]])),Integer.parseInt(parts[indices[3]]));
	}
	public void processRoute(String[] parts, int[] indices, int r) {
		routes[r].put(parts[indices[0]], new Route(parts[indices[1]], RouteTypes.values()[Integer.parseInt(parts[indices[2]])]));
	}
	public void processTrip(String[] parts, int[] indices, int r) {
		Route route = routes[r].get(parts[indices[0]]);
		if(parts.length==5) {
			route.putTrip(parts[indices[1]], new Trip(services[r].get(parts[indices[2]]), shapes[r].get(parts[indices[3]]),route));
		}
		else
			route.putTrip(parts[indices[1]], new Trip(services[r].get(parts[indices[2]]), null, route));
	}
	public void processStopTime(String[] parts, int[] indices, int r) {
		for(Route actualRoute:routes[r].values()) {
			Trip trip = actualRoute.getTrips().get(parts[indices[0]]);
			if(trip!=null) {
				try {
					trip.putStopTime(Integer.parseInt(parts[indices[1]]), new StopTime(timeFormat.parse(parts[indices[2]]),timeFormat.parse(parts[indices[3]]),parts[indices[4]]));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
	}
	public void processFrequency(String[] parts, int[] indices, int r) {
		for(Route actualRoute:routes[r].values()) {
			Trip trip = actualRoute.getTrips().get(parts[indices[0]]);
			if(trip!=null) {
				try {
					trip.addFrequency(new Frequency(timeFormat.parse(parts[indices[1]]), timeFormat.parse(parts[indices[2]]), Integer.parseInt(parts[indices[3]])));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
	}
	/**
	 * From the loaded information calculates all the necessary information for MATSim
	 * @throws IOException 
	 */
	private void calculateUnknownInformation() throws IOException {
		//New Stops according to the modes
		for(int r=0; r<roots.length; r++)
			for(Route route:routes[r].values())
				for(Trip trip:route.getTrips().values())
					for(Entry<Integer,StopTime> stopTime:trip.getStopTimes().entrySet())
						if(stops[r].get(stopTime.getValue().getStopId()).getRouteType()==null)
							stops[r].get(stopTime.getValue().getStopId()).setRouteType(route.getRouteType());
						else if(!stops[r].get(stopTime.getValue().getStopId()).getRouteType().equals(route.getRouteType())) {
							Stop brotherStop = stops[r].get(stopTime.getValue().getStopId());
							Stop newStop = new Stop(brotherStop.getPoint(), brotherStop.getName(), brotherStop.isBlocks());
							newStop.setRouteType(route.getRouteType());
							String newStopId = stopTime.getValue().getStopId()+"_"+route.getRouteType().name;
							stops[r].put(newStopId, newStop);
							stopTime.getValue().setStopId(newStopId);
						}
						else if(stops[r].get(stopTime.getValue().getStopId()).getRouteType().equals(route.getRouteType()) && !route.getRouteType().wayType.equals(Route.WayTypes.ROAD)) {
							Stop brotherStop = stops[r].get(stopTime.getValue().getStopId());
							Stop newStop = new Stop(brotherStop.getPoint(), brotherStop.getName(), brotherStop.isBlocks());
							newStop.setRouteType(route.getRouteType());
							String newStopId = stopTime.getValue().getStopId()+"_"+stopTime.getKey();
							stops[r].put(newStopId, newStop);
							stopTime.getValue().setStopId(newStopId);
						}
		//Links
		for(int r=0; r<roots.length; r++) {
			if(r==0) {
				RoutesPathsGenerator routesPathsGenerator = new RoutesPathsGenerator(network, routes[r], stops[r]);
				routesPathsGenerator.run();
			}
			for(Route route:routes[r].values())
				if(!route.getRouteType().wayType.equals(Route.WayTypes.ROAD))
					for(Trip trip:route.getTrips().values())
						addNewLinksSequence(trip, true, route.getRouteType(), r);
		}
	}
	/**
	 * Methods for write a new or calculated sequence of links for a trip
	 * @param trip
	 * @param withShape
	 */
	private void addNewLinksSequence(Trip trip, boolean withShape, RouteTypes routeType, int r) {
		Shape shape = trip.getShape();
		double length;
		double freeSpeed=100;
		double capacity=100;
		double nOfLanes=1;
		if(withShape && shape!=null) {
			SortedMap<Integer, StopTime> stopTimes = trip.getStopTimes();
			Stop stop = stops[r].get(stopTimes.get(stopTimes.firstKey()).getStopId());
			double nearest = CoordUtils.calcDistance(shape.getPoints().get(1),stop.getPoint());
			int nearestIndex = 1;
			for(int n=2; n<shape.getPoints().size(); n++) {
				double distance = CoordUtils.calcDistance(shape.getPoints().get(n),stop.getPoint());
				if(distance<nearest) {
					nearest = distance;
					nearestIndex = n;
				}
			}
			if(nearestIndex>1)
				nearestIndex--;
			IdImpl id = new IdImpl(shape.getId()+"_"+nearestIndex);
			NodeImpl node = (NodeImpl) network.getNodes().get(id);
			if(node==null) {
				node = new NodeImpl(id);
				node.setCoord(shape.getPoints().get(nearestIndex));
				network.addNode(node);
			}
			Node previous = node;
			for(int p=nearestIndex+1; p<shape.getPoints().size(); p++) {
				id = new IdImpl(shape.getId()+"_"+p);
				node = (NodeImpl) network.getNodes().get(id);
				if(node==null) {
					node = new NodeImpl(id);
					node.setCoord(shape.getPoints().get(p));
					network.addNode(node);
				}
				id = new IdImpl((p-1)+"_"+shape.getId()+"_"+p);
				Link link = network.getLinks().get(id);
				if(link==null) {
					length = CoordUtils.calcDistance(previous.getCoord(), node.getCoord());
					link = new LinkFactoryImpl().createLink(id, previous, node, network, length, freeSpeed, capacity, nOfLanes);
					Set<String> modes = new HashSet<String>();
					modes.add(routeType.name);
					link.setAllowedModes(modes);
					network.addLink(link);
				}
				trip.addLink(link);
				previous = node;
			}
		}
		else {
			String id = trip.getStopTimes().get(1).getStopId();
			Stop stop = stops[r].get(id);
			NodeImpl node = (NodeImpl) network.getNodes().get(new IdImpl(id));
			if(node==null) {
				node = new NodeImpl(new IdImpl(id));
				node.setCoord(stop.getPoint());
				network.addNode(node);
			}
			Node previous = node;
			for(int p=2; p<trip.getStopTimes().size(); p++) {
				id = trip.getStopTimes().get(p).getStopId();
				stop = stops[r].get(id);
				node = (NodeImpl) network.getNodes().get(new IdImpl(id));
				if(node==null) {
					node = new NodeImpl(new IdImpl(id));
					node.setCoord(stop.getPoint());
					network.addNode(node);
				}
				id = trip.getStopTimes().get(p-1).getStopId()+"_"+id;
				Link link = network.getLinks().get(new IdImpl(id));
				if(link==null) {
					length = CoordUtils.calcDistance(previous.getCoord(), node.getCoord());
					link = new LinkFactoryImpl().createLink(new IdImpl(id), previous, node, network, length, freeSpeed, capacity, nOfLanes);
					Set<String> modes = new HashSet<String>();
					modes.add(routeType.name);
					link.setAllowedModes(modes);
					network.addLink(link);
				}
				trip.addLink(link);
				previous = node;
			}
		}
	}
	@Override
	/**
	 * Writes the MATSim public transport file
	 * @param filename
	 */
	public void write(String filename) {
		try {
			loadGTFSFiles();
			calculateUnknownInformation();
			//Public Transport Schedule
			//Stops
			this.openFile(filename);
			this.writeXmlHead();
			this.writeStartTag("transitStops", new ArrayList<Tuple<String,String>>());
			for(int r=0; r<roots.length; r++)
				for(Entry<String, Stop> stop: stops[r].entrySet()) {
					List<Tuple<String, String>> params = new ArrayList<Tuple<String,String>>();
					params.add(new Tuple<String, String>("id", stop.getKey()));
					params.add(new Tuple<String, String>("x", Double.toString(stop.getValue().getPoint().getX())));
					params.add(new Tuple<String, String>("y", Double.toString(stop.getValue().getPoint().getX())));
					params.add(new Tuple<String, String>("linkRefId", stop.getValue().getLinkId()));
					params.add(new Tuple<String, String>("name", stop.getValue().getName()));
					params.add(new Tuple<String, String>("isBlocking", Boolean.toString(stop.getValue().isBlocks())));
					this.writeStartTag("stopFacilities", params,true);
				}
			writeEndTag("transitStops");
			//Lines
			for(int r=0; r<roots.length; r++)
				for(Entry<String,Route> route:routes[r].entrySet()) {
					List<Tuple<String,String>> routeAtts = new ArrayList<Tuple<String,String>>();
					routeAtts.add(new Tuple<String, String>("id", route.getKey()));
					this.writeStartTag("transitLine", routeAtts);
					for(Entry<String,Trip> trip:route.getValue().getTrips().entrySet()) {
						boolean isService=false;
						for(String serviceId:serviceIds)
							if(trip.getValue().getService().equals(services[r].get(serviceId)))
								isService = true;
						if(isService) {
							List<Tuple<String,String>> tripAtts = new ArrayList<Tuple<String,String>>();
							tripAtts.add(new Tuple<String, String>("id", trip.getKey()));
							this.writeStartTag("transitRoute", tripAtts);
							//Mode
							this.writeStartTag("transportMode", new ArrayList<Tuple<String,String>>());
							this.writeContent(route.getValue().getRouteType().name, true);
							this.writeEndTag("transportMode");
							//Route profile
							this.writeStartTag("routeProfile", new ArrayList<Tuple<String,String>>());
							Date startTime = trip.getValue().getStopTimes().get(trip.getValue().getStopTimes().firstKey()).getArrivalTime();
							for(Integer stopTimeKey:trip.getValue().getStopTimes().keySet()) {
								StopTime stopTime = trip.getValue().getStopTimes().get(stopTimeKey);
								List<Tuple<String,String>> stopTimeAtts = new ArrayList<Tuple<String,String>>();
								stopTimeAtts.add(new Tuple<String, String>("refId", stopTime.getStopId()));
								if(!stopTimeKey.equals(trip.getValue().getStopTimes().firstKey())) {
									long difference = stopTime.getArrivalTime().getTime()-startTime.getTime();
									stopTimeAtts.add(new Tuple<String, String>("arrivalOffSet", timeFormat.format(new Date(timeFormat.parse("00:00:00").getTime()+difference))));
								}
								if(!stopTimeKey.equals(trip.getValue().getStopTimes().lastKey())) {
									long difference = stopTime.getDepartureTime().getTime()-startTime.getTime();
									stopTimeAtts.add(new Tuple<String, String>("departureOffSet", timeFormat.format(new Date(timeFormat.parse("00:00:00").getTime()+difference))));
								}
								this.writeStartTag("stop", stopTimeAtts, true);
							}
							this.writeEndTag("routeProfile");
							//Route
							this.writeStartTag("route", new ArrayList<Tuple<String,String>>());
							for(Link link:trip.getValue().getLinks()) {
								List<Tuple<String,String>> linkAtts = new ArrayList<Tuple<String,String>>();
								linkAtts.add(new Tuple<String, String>("refId", link.getId().toString()));
								this.writeStartTag("link", linkAtts, true);
							}
							this.writeEndTag("route");
							//Departures
							int id = 1;
							this.writeStartTag("departures", new ArrayList<Tuple<String,String>>());
							for(Frequency frequency:trip.getValue().getFrequencies()) {
								for(Date actualTime = frequency.getStartTime(); actualTime.before(frequency.getEndTime()); actualTime.setTime(actualTime.getTime()+frequency.getSecondsPerDeparture()*1000)) {
									List<Tuple<String,String>> departureAtts = new ArrayList<Tuple<String,String>>();
									departureAtts.add(new Tuple<String, String>("id", Integer.toString(id)));
									departureAtts.add(new Tuple<String, String>("departureTime", timeFormat.format(actualTime)));
									this.writeStartTag("departure", departureAtts, true);
									id++;
								}
							}
							if(id==1) {
								List<Tuple<String,String>> departureAtts = new ArrayList<Tuple<String,String>>();
								departureAtts.add(new Tuple<String, String>("id", Integer.toString(id)));
								departureAtts.add(new Tuple<String, String>("departureTime", timeFormat.format(trip.getValue().getStopTimes().get(trip.getValue().getStopTimes().firstKey()).getDepartureTime())));
								this.writeStartTag("departure", departureAtts, true);
							}
							this.writeEndTag("departures");
							this.writeEndTag("transitRoute");
						}
					}
					this.writeEndTag("transitLine");
				}
			this.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	//Main method
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario);
		matsimNetworkReader.readFile("./data/networks/singapore.xml");
		Network network = scenario.getNetwork();
		GTFS2MATSimTransitScheduleFileWriter g2m = new GTFS2MATSimTransitScheduleFileWriter(new File[]{new File("./data/gtfs/buses"),new File("./data/gtfs/trains")}, network, new String[]{"weekday","weeksatday","daily"});
		g2m.write("./data/gtfs/test2.xml");
	}

}
