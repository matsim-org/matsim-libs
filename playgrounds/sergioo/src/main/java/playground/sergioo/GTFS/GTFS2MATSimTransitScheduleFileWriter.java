package playground.sergioo.GTFS;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkFactoryImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.sergioo.GTFS.Route.RouteTypes;
import playground.sergioo.GTFS.auxiliar.LinkStops;
import playground.sergioo.PathEditor.gui.Window;
import playground.sergioo.PathEditor.kernel.RoutesPathsGenerator;
import util.geometry.Line2D;
import util.geometry.Point2D;

public class GTFS2MATSimTransitScheduleFileWriter extends MatsimXmlWriter implements MatsimWriter {
	
	//Constants
	/**
	 * Maximum distance allowed between an stop and the end of the corresponding link
	 */
	private static final double MAX_DISTANCE_STOP_LINK = 50*180/(6371000*Math.PI);
	private static final double DEFAULT_FREE_SPEED = 20;
	private static final double DEFAULT_CAPCITY = 500;
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
	 * @param root
	 * @param network
	 */
	public GTFS2MATSimTransitScheduleFileWriter(File[] roots, Network network, String[] serviceIds) {
		super();
		this.roots = roots;
		this.network = network;
		updateNetwork();
		this.serviceIds = serviceIds;
	}

	private void updateNetwork() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(RoutesPathsGenerator.NEW_NETWORK_NODES_FILE));
			String line = reader.readLine();
			while(line!=null) {
				Id id = new IdImpl(line);
				network.addNode(network.getFactory().createNode(id, new CoordImpl(Double.parseDouble(reader.readLine()), Double.parseDouble(reader.readLine()))));
				line = reader.readLine();
			}
			reader.close();
			reader = new BufferedReader(new FileReader(RoutesPathsGenerator.NEW_NETWORK_LINKS_FILE));
			line = reader.readLine();
			while(line!=null) {
				Link link = new LinkFactoryImpl().createLink(new IdImpl(line), network.getNodes().get(new IdImpl(reader.readLine())), network.getNodes().get(new IdImpl(reader.readLine())), network, -1, DEFAULT_FREE_SPEED, DEFAULT_CAPCITY, 1);
				Set<String> modes = new HashSet<String>();
				modes.add("Car");
				modes.add("Bus");
				link.setAllowedModes(modes);
				network.addLink(link);
				line = reader.readLine();
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		//Path
		boolean shape = false;
		if(shape)
			generateRepeatedMRTStops();
		for(int r=0; r<roots.length; r++) {
			if(r==0) {
				RoutesPathsGenerator routesPathsGenerator = new RoutesPathsGenerator(network, routes[r], stops[r]);
				routesPathsGenerator.run();
			}
			for(Entry<String,Route> route:routes[r].entrySet())
				if(!route.getValue().getRouteType().wayType.equals(Route.WayTypes.ROAD))
					for(Entry<String,Trip> trip:route.getValue().getTrips().entrySet())
						if(shape && trip.getValue().getShape()!= null)
							addNewLinksSequenceShape(trip, route.getValue().getRouteType(), route.getKey(), r);
						else
							addNewLinksSequence(trip.getValue(), route.getValue().getRouteType(), route.getKey(), r);
		}
		//Splitting of stop-links
		splitBusStopLinks(MAX_DISTANCE_STOP_LINK);
		/*for(Route route:routes[0].values())
			for(Entry<String,Trip> trip:route.getTrips().entrySet()) {
				Window window = new Window(trip.getKey(),network,trip.getValue(),stops[0]);
				window.setVisible(true);
				while(window.isVisible())
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
			}*/
		//Coordinates system of the network
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
		for(Node node:network.getNodes().values())
			((NodeImpl)node).setCoord(ct.transform(node.getCoord()));
		for(Link link:network.getLinks().values())
			if(((LinkImpl)link).getOrigId()!=null)
				((LinkImpl)link).setLength(((LinkImpl)link).getLength()*1000);
			else
				((LinkImpl)link).setLength(CoordUtils.calcDistance(link.getFromNode().getCoord(),link.getToNode().getCoord()));
	}
	private void generateRepeatedMRTStops() {
		for(Entry<String,Route> routeE:routes[1].entrySet()) {
			for(Trip trip:routeE.getValue().getTrips().values()) {
				String pStopTime = "";
				for(StopTime stopTime:trip.getStopTimes().values()) {
					Stop stop = stops[1].get(stopTime.getStopId());
					String[] parts = stopTime.getStopId().split("/");
					int iPart=-1;
					for(iPart++;iPart<parts.length && !parts[iPart].contains(routeE.getKey());iPart++);
					if(pStopTime.equals("") && iPart<parts.length) {
						if(parts[iPart].equals("EW23"))
							pStopTime = "EW24";
						else if(parts[iPart].equals("EW4"))
							pStopTime = "EW5";
						else if(parts[iPart].equals("EW24"))
							pStopTime = "EW23";
						else if(parts[iPart].equals("NS7"))
							pStopTime = "NS5";
						else if(parts[iPart].equals("NS13"))
							pStopTime = "NS11";
						else if(parts[iPart].equals("NS21"))
							pStopTime = "NS22";
						else if(parts[iPart].equals("NS16"))
							pStopTime = "NS17";
						else if(parts[iPart].equals("CC12"))
							pStopTime = "CC11";
						
					}
					String newId = pStopTime+"_to_"+(iPart<parts.length?parts[iPart]:stopTime.getStopId());
					stops[1].put(newId,stop);
					stopTime.setStopId(newId);
					pStopTime = (iPart<parts.length?parts[iPart]:stopTime.getStopId());
				}
			}
		}
		Object[] stopIds = stops[1].keySet().toArray();
		for(Object stopId:stopIds)
			if(!((String)stopId).contains("_to_"))
				stops[1].remove(stopId);
		System.out.println();
	}
	/**
	 * Methods for write a new or calculated sequence of links for a trip
	 * @param trip
	 * @param withShape
	 */
	private void addNewLinksSequence(Trip trip, RouteTypes routeType, String routeKey, int r) {
		double length;
		double freeSpeed=100;
		double capacity=100;
		double nOfLanes=1;
		StopTime stopTime = trip.getStopTimes().get(1);
		String id = stopTime.getStopId();
		Stop stop = stops[r].get(id);
		NodeImpl node = (NodeImpl) network.getNodes().get(new IdImpl(id));
		if(node==null) {
			node = new NodeImpl(new IdImpl(id));
			node.setCoord(stop.getPoint());
			network.addNode(node);
		}
		String id2 = trip.getStopTimes().get(2).getStopId();
		Stop stop2 = stops[r].get(id2);
		NodeImpl node2 = (NodeImpl) network.getNodes().get(new IdImpl(id2));
		if(node2==null) {
			node2 = new NodeImpl(new IdImpl(id2));
			node2.setCoord(stop2.getPoint());
			network.addNode(node2);
		}
		id2 = trip.getStopTimes().get(2).getStopId()+"_"+trip.getStopTimes().get(1).getStopId();
		Link link = network.getLinks().get(new IdImpl(id2));
		if(link==null) {
			length = CoordUtils.calcDistance(node2.getCoord(), node.getCoord());
			link = new LinkFactoryImpl().createLink(new IdImpl(id2), node2, node, network, length, freeSpeed, capacity, nOfLanes);
			Set<String> modes = new HashSet<String>();
			modes.add(routeType.name);
			link.setAllowedModes(modes);
			network.addLink(link);
		}
		trip.addLink(link);
		String[] parts = id.split("/");
		if(parts.length>1) {
			for(String part:parts)
				if(part.startsWith(routeKey)) {
					Stop nStop = stops[r].get(part);
					Stop nStopR = stops[r].get(part+"_r");
					if(nStop==null && nStopR==null) {
						nStop = new Stop(stop.getPoint(), stop.getName(), stop.isBlocks());
						nStop.setLinkId(id2);
						nStop.setFixedLinkId();
						stops[r].put(part,nStop);
						stopTime.setStopId(part);
					}
					else if(nStopR==null && !nStop.getLinkId().equals(id2)) {
							nStopR = new Stop(stop.getPoint(), stop.getName(), stop.isBlocks());
							nStopR.setLinkId(id2);
							nStopR.setFixedLinkId();
							stops[r].put(part+"_r",nStopR);
							stopTime.setStopId(part+"_r");
					}
					else
						if(id2.equals(nStop.getLinkId()))
							stopTime.setStopId(part);
						else
							stopTime.setStopId(part+"_r");
				}
		}
		else
			if(!stop.setLinkId(id2) && !stop.getLinkId().equals(id2)) {
				Stop nStopR = new Stop(stop.getPoint(), stop.getName(), stop.isBlocks());
				nStopR.setLinkId(id2);
				nStopR.setFixedLinkId();
				stops[r].put(id+"_r",nStopR);
				stopTime.setStopId(id+"_r");
			}
			else
				stop.setFixedLinkId();
		Node previous = node;
		String prevId = id; 
		for(int p=2; p<=trip.getStopTimes().lastKey(); p++)
			if(trip.getStopTimes().get(p)!=null) {
				stopTime = trip.getStopTimes().get(p);
				id = stopTime.getStopId();
				stop = stops[r].get(id);
				node = (NodeImpl) network.getNodes().get(new IdImpl(id));
				if(node==null) {
					node = new NodeImpl(new IdImpl(id));
					node.setCoord(stop.getPoint());
					network.addNode(node);
				}
				id2 = prevId+"_"+id;
				link = network.getLinks().get(new IdImpl(id2));
				if(link==null) {
					length = CoordUtils.calcDistance(previous.getCoord(), node.getCoord());
					link = new LinkFactoryImpl().createLink(new IdImpl(id2), previous, node, network, length, freeSpeed, capacity, nOfLanes);
					Set<String> modes = new HashSet<String>();
					modes.add(routeType.name);
					link.setAllowedModes(modes);
					network.addLink(link);
				}
				trip.addLink(link);
				parts = id.split("/");
				if(parts.length>1) {
					for(String part:parts)
						if(part.startsWith(routeKey)) {
							Stop nStop = stops[r].get(part);
							Stop nStopR = stops[r].get(part+"_r");
							if(nStop==null && nStopR==null) {
								nStop = new Stop(stop.getPoint(), stop.getName(), stop.isBlocks());
								nStop.setLinkId(id2);
								nStop.setFixedLinkId();
								stops[r].put(part,nStop);
								stopTime.setStopId(part);
							}
							else if(nStopR==null && !nStop.getLinkId().equals(id2)) {
								nStopR = new Stop(stop.getPoint(), stop.getName(), stop.isBlocks());
								nStopR.setLinkId(id2);
								nStopR.setFixedLinkId();
								stops[r].put(part+"_r",nStopR);
								stopTime.setStopId(part+"_r");
							}
							else
								if(id2.equals(nStop.getLinkId()))
									stopTime.setStopId(part);
								else
									stopTime.setStopId(part+"_r");
						}
				}
				else
					if(!stop.setLinkId(id2) && !stop.getLinkId().equals(id2)) {
						Stop nStopR = new Stop(stop.getPoint(), stop.getName(), stop.isBlocks());
						nStopR.setLinkId(id2);
						nStopR.setFixedLinkId();
						stops[r].put(id+"_r",nStopR);
						stopTime.setStopId(id+"_r");
					}
					else
						stop.setFixedLinkId();
				previous = node;
				prevId = id;
			}
		/*Window window = new Window("ssss",network,trip,stops[r]);
		window.setVisible(true);
		while(window.isVisible())
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
	}
	private void addNewLinksSequenceShape(Entry<String,Trip> tripE, RouteTypes routeType, String routeKey, int r) {
		Shape shape = tripE.getValue().getShape();
		Iterator<StopTime> iStopTime=tripE.getValue().getStopTimes().values().iterator();
		String stopId = iStopTime.next().getStopId();
		double nearest = CoordUtils.calcDistance(shape.getPoints().get(1),stops[r].get(stopId).getPoint());
		int nearI = 1;
		for(int n=2; n<shape.getPoints().size(); n++) {
			double distance = CoordUtils.calcDistance(shape.getPoints().get(n),stops[r].get(stopId).getPoint());
			if(distance<nearest) {
				nearest = distance;
				nearI = n;
			}
		}
		Point2D pPoint = nearI==1?null:new Point2D(shape.getPoints().get(nearI-1).getX(), shape.getPoints().get(nearI-1).getY());
		Point2D point = new Point2D(shape.getPoints().get(nearI).getX(), shape.getPoints().get(nearI).getY());
		Point2D nPoint = new Point2D(shape.getPoints().get(nearI+1).getX(), shape.getPoints().get(nearI+1).getY());
		Point2D sPoint = new Point2D(stops[r].get(stopId).getPoint().getX(), stops[r].get(stopId).getPoint().getY());
		Line2D line = pPoint==null?null:new Line2D(pPoint, point);
		Line2D lineN = new Line2D(point, nPoint);
		Node previous = null, node = null;
		Link link = null;
		if(line!=null && line.isNearestInside(sPoint))
			nearI--;
		else if(!lineN.isNearestInside(sPoint)) {
			Iterator<StopTime> iStopTime2=tripE.getValue().getStopTimes().values().iterator();
			iStopTime2.next();
			String stop2Id = iStopTime2.next().getStopId();
			node = addNode(stop2Id, stops[1].get(stop2Id).getPoint());
			for(previous = node; !(line!=null && line.isNearestInside(sPoint)) && !lineN.isNearestInside(sPoint);previous = node) {
				node = addNode(stopId,stops[1].get(stopId).getPoint());
				String idS = stopId;
				link = addLink(idS, previous, node, routeType);
				tripE.getValue().addLink(link);
				stops[r].get(stopId).setLinkId(idS);
				if(iStopTime.hasNext())
					stopId = iStopTime.next().getStopId();
				nearest = CoordUtils.calcDistance(shape.getPoints().get(1),stops[r].get(stopId).getPoint());
				nearI = 1;
				for(int n=2; n<shape.getPoints().size(); n++) {
					double distance = CoordUtils.calcDistance(shape.getPoints().get(n),stops[r].get(stopId).getPoint());
					if(distance<nearest) {
						nearest = distance;
						nearI = n;
					}
				}
				pPoint = nearI==1?null:new Point2D(shape.getPoints().get(nearI-1).getX(), shape.getPoints().get(nearI-1).getY());
				point = new Point2D(shape.getPoints().get(nearI).getX(), shape.getPoints().get(nearI).getY());
				nPoint = new Point2D(shape.getPoints().get(nearI+1).getX(), shape.getPoints().get(nearI+1).getY());
				sPoint = new Point2D(stops[r].get(stopId).getPoint().getX(), stops[r].get(stopId).getPoint().getY());
				line = pPoint==null?null:new Line2D(pPoint, point);
				lineN = new Line2D(point, nPoint);
			}
			if(line!=null && line.isNearestInside(sPoint))
				nearI--;
		}
		node = addNode(shape.getId()+"_"+nearI, shape.getPoints().get(nearI));
		if(previous != null) {
			link = addLink("con"+tripE.getKey(), previous, node, routeType);
			tripE.getValue().addLink(link);
		}
		previous = node;
		boolean end=false;
		for(int p=nearI+1; !end; p++) {
			if(p<=shape.getPoints().size()) {
				if(!shape.getPoints().get(p).equals(node.getCoord())) {
					node = addNode(shape.getId()+"_"+p, shape.getPoints().get(p));
					String idS = nearI+"_"+shape.getId()+"_"+p;
					link =	addLink(idS,previous,node,routeType);
					nearI = p;
					tripE.getValue().addLink(link);
					pPoint = new Point2D(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
					point = new Point2D(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
					sPoint = new Point2D(stops[r].get(stopId).getPoint().getX(), stops[r].get(stopId).getPoint().getY());
					line = new Line2D(pPoint, point);
					if(line.isNearestInside(sPoint)) {
						stops[r].get(stopId).setLinkId(idS);
						if(iStopTime.hasNext())
							stopId = iStopTime.next().getStopId();
						else
							end = true;
					}
				}
			}
			else {
				node = addNode(stopId,stops[1].get(stopId).getPoint());
				String idS = stopId;
				link = addLink(idS, previous, node, routeType);
				tripE.getValue().addLink(link);
				stops[r].get(stopId).setLinkId(idS);
				if(iStopTime.hasNext()) {
					stopId = iStopTime.next().getStopId();
				}
				else
					end = true;
			}
			previous = node;
		}
		Window window = new Window(tripE.getKey(),network,tripE.getValue(),stops[r]);
		window.setVisible(true);
		while(window.isVisible())
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}
	
	private Link addLink(String idS, Node previous, Node node, RouteTypes routeType) {
		double freeSpeed=100;
		double capacity=100;
		double nOfLanes=1;
		Id id = new IdImpl(idS);
		Link link = network.getLinks().get(id);
		if(link==null) {
			link = new LinkFactoryImpl().createLink(id, previous, node, network, 0, freeSpeed, capacity, nOfLanes);
			Set<String> modes = new HashSet<String>();
			modes.add(routeType.name);
			link.setAllowedModes(modes);
			network.addLink(link);
		}
		return link;
	}

	private Node addNode(String idS, Coord coord) {
		Id id = new IdImpl(idS);
		NodeImpl node = (NodeImpl) network.getNodes().get(id);
		if(node==null) {
			node = new NodeImpl(id);
			node.setCoord(coord);
			network.addNode(node);
		}
		return node;
	}
	/**
	 * Modifies the network avoiding big distances between stops and the end 
	 * @param maxDistanceStopLink
	 */
	private void splitBusStopLinks(double maxDistanceStopLink) {
		Map<String,LinkStops> linksStops = new HashMap<String,LinkStops>();
		for(Stop stop:stops[0].values()) {
			if(stop.getLinkId()!=null) {
				LinkStops linkStops = linksStops.get(stop.getLinkId());
				if(linkStops == null) {
					linkStops = new LinkStops(network.getLinks().get(new IdImpl(stop.getLinkId())));
					linksStops.put(stop.getLinkId(), linkStops);
				}
				linkStops.addStop(stop);
			}
		}
		for(LinkStops linkStops:linksStops.values()) {
			for(int i=0; i<linkStops.getNumStops()-1; i++)
				try {
					changeBusTrips(linkStops.getLink(),linkStops.split(i,network), false);
				} catch (Exception e) {
					e.printStackTrace();
				}
			if(linkStops.getLastDistance()>MAX_DISTANCE_STOP_LINK)
				try {
					changeBusTrips(linkStops.getLink(),linkStops.split(linkStops.getNumStops()-1,network), true);
				} catch (Exception e) {
					e.printStackTrace();
				}	
		}	
	}

	private void changeBusTrips(Link link, Link split, boolean last) {
		for(Route route:routes[0].values())
			for(Trip trip:route.getTrips().values())
				for(int i=0; i<trip.getLinks().size(); i++)
					if(trip.getLinks().get(i).equals(link)) {
						String firstStopId = trip.getStopTimes().get(trip.getStopTimes().firstKey()).getStopId();
						if(i>0 || (i==0 && stops[0].get(firstStopId).getLinkId().equals(split.getId().toString()))) {
							trip.getLinks().add(i, split);
							i++;
							if(last && i==trip.getLinks().size()-1)
								trip.getLinks().remove(i);
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
			//Write modified network
			NetworkWriter networkWriter =  new NetworkWriter(network);
			networkWriter.write("./data/networks/singapore2.xml");
			//Public Transport Schedule
			//Stops
			this.openFile(filename);
			this.writeXmlHead();
			this.writeDoctype("transitSchedule", "http://www.matsim.org/files/dtd/transitSchedule_v1.dtd");
			this.writeStartTag("transitSchedule", new ArrayList<Tuple<String,String>>());
			this.writeStartTag("transitStops", new ArrayList<Tuple<String,String>>());
			CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
			for(int r=0; r<roots.length; r++)
				for(Entry<String, Stop> stop: stops[r].entrySet())
					if(stop.getValue().getLinkId()!=null) {
						Coord result = ct.transform(stop.getValue().getPoint());
						List<Tuple<String, String>> params = new ArrayList<Tuple<String,String>>();
						params.add(new Tuple<String, String>("id", stop.getKey()));
						params.add(new Tuple<String, String>("x", Double.toString(result.getX())));
						params.add(new Tuple<String, String>("y", Double.toString(result.getY())));
						params.add(new Tuple<String, String>("linkRefId", stop.getValue().getLinkId()));
						params.add(new Tuple<String, String>("name", stop.getValue().getName()));
						params.add(new Tuple<String, String>("isBlocking", Boolean.toString(stop.getValue().isBlocks())));
						this.writeStartTag("stopFacility", params,true);
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
									stopTimeAtts.add(new Tuple<String, String>("arrivalOffset", timeFormat.format(new Date(timeFormat.parse("00:00:00").getTime()+difference))));
								}
								if(!stopTimeKey.equals(trip.getValue().getStopTimes().lastKey())) {
									long difference = stopTime.getDepartureTime().getTime()-startTime.getTime();
									stopTimeAtts.add(new Tuple<String, String>("departureOffset", timeFormat.format(new Date(timeFormat.parse("00:00:00").getTime()+difference))));
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
			this.writeEndTag("transitSchedule");
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
		matsimNetworkReader.readFile("./data/networks/singapore_initial.xml");
		Network network = scenario.getNetwork();
		GTFS2MATSimTransitScheduleFileWriter g2m = new GTFS2MATSimTransitScheduleFileWriter(new File[]{new File("./data/gtfs/buses"),new File("./data/gtfs/trains")}, network, new String[]{"weekday","weeksatday","daily"});
		g2m.write("./data/gtfs/test.xml");
	}

}
