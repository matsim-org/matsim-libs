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
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.sergioo.VISUM.VisumFile2MatsimNetwork;


public class GTFS2MATSimTransitScheduleFileWriter extends MatsimXmlWriter implements MatsimWriter{

	//Constants
	/**
	 * Visum network file
	 */
	private static final String VISUM_FILE = "C:/Users/sergioo/Documents/2011/Work/FCL/Operations/Data/Navteq/Network.net";
	/**
	 * Table beginnings
	 */
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
	 * The calendar sevices
	 */
	private Map<String, Service> services;
	/**
	 * The shapes
	 */
	private Map<String, Shape> shapes;
	/**
	 * The routes
	 */
	private SortedMap<String, Route> routes;
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
	 * 
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
	 * 
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
	
	@Override
	/**
	 * 
	 * @param filename
	 */
	public void write(String filename) {
		try {
			timeFormat = new SimpleDateFormat("HH:mm:ss");
			BufferedReader reader = null;
			String line = null;
			this.openFile(filename);
			this.writeXmlHead();
			//Stops
			this.writeStartTag("transitStops", new ArrayList<Tuple<String,String>>());
			for(File root:roots) {
				File fileStops = new File(root.getPath()+"/"+GTFSDefinitions.STOPS.fileName);
				reader = new BufferedReader(new FileReader(fileStops));
				int[] indices = GTFSDefinitions.STOPS.getIndices(reader.readLine());
				line = reader.readLine();
				while(line!=null) {
					String[] parts = line.split(",");
					List<Tuple<String, String>> params = new ArrayList<Tuple<String,String>>();
					params.add(new Tuple<String, String>("id", parts[indices[0]]));
					params.add(new Tuple<String, String>("x", parts[indices[1]]));
					params.add(new Tuple<String, String>("y", parts[indices[2]]));
					String id = ((NetworkImpl)network).getNearestLink(new CoordImpl(Double.parseDouble(parts[indices[1]]), Double.parseDouble(parts[indices[2]]))).getId().toString();
					params.add(new Tuple<String, String>("linkRefId", id));
					params.add(new Tuple<String, String>("name", parts[indices[3]]));
					params.add(new Tuple<String, String>("isBlocking", "true"));
					this.writeStartTag("stopFacilities", params,true);
					line = reader.readLine();
				}
				reader.close();
			}
			writeEndTag("transitStops");
			for(File root:roots) {
				//Files load
				services = new HashMap<String, Service>();
				shapes = new HashMap<String, Shape>(); 
				routes = new TreeMap<String, Route>();
				for(GTFSDefinitions gtfs:GTFSDefinitions.values()) {
					if(!gtfs.equals(GTFSDefinitions.STOPS)) {
						File file = new File(root.getPath()+"/"+gtfs.fileName);
						reader = new BufferedReader(new FileReader(file));
						int[] indices = gtfs.getIndices(reader.readLine());
						line = reader.readLine();
						while(line!=null) {
							String[] parts = line.split(",");
							Method m = GTFS2MATSimTransitScheduleFileWriter.class.getMethod(gtfs.getFunction(), new Class[] {String[].class,int[].class});
							m.invoke(this, new Object[]{parts,indices});
							line = reader.readLine();
						}
						reader.close();
					}
				}
				//Public Transport Schedule
				for(String routeKey:routes.keySet()) {
					Route route = routes.get(routeKey);
					List<Tuple<String,String>> routeAtts = new ArrayList<Tuple<String,String>>();
					routeAtts.add(new Tuple<String, String>("id", routeKey));
					this.writeStartTag("transitLine", routeAtts);
					for(String tripKey:route.getTrips().keySet()) {
						Trip trip = route.getTrips().get(tripKey);
						boolean isService=false;
						for(String serviceId:serviceIds)
							if(trip.getService().equals(services.get(serviceId)))
								isService = true;
						if(isService) {
							List<Tuple<String,String>> tripAtts = new ArrayList<Tuple<String,String>>();
							tripAtts.add(new Tuple<String, String>("id", tripKey));
							this.writeStartTag("transitRoute", tripAtts);
							//Mode
							this.writeStartTag("transportMode", new ArrayList<Tuple<String,String>>());
							this.writeContent(Route.ROUTE_TYPES[route.getRouteType()], true);
							this.writeEndTag("transportMode");
							//Route profile
							this.writeStartTag("routeProfile", new ArrayList<Tuple<String,String>>());
							Date startTime = trip.getStopTimes().get(trip.getStopTimes().firstKey()).getArrivalTime();
							for(Integer stopTimeKey:trip.getStopTimes().keySet()) {
								StopTime stopTime = trip.getStopTimes().get(stopTimeKey);
								List<Tuple<String,String>> stopTimeAtts = new ArrayList<Tuple<String,String>>();
								stopTimeAtts.add(new Tuple<String, String>("refId", stopTime.getStopId()));
								if(!stopTimeKey.equals(trip.getStopTimes().firstKey())) {
									long difference = stopTime.getArrivalTime().getTime()-startTime.getTime();
									try {
										stopTimeAtts.add(new Tuple<String, String>("arrivalOffSet", timeFormat.format(new Date(timeFormat.parse("00:00:00").getTime()+difference))));
									} catch (ParseException e) {
										e.printStackTrace();
									}
								}
								if(!stopTimeKey.equals(trip.getStopTimes().lastKey())) {
									long difference = stopTime.getDepartureTime().getTime()-startTime.getTime();
									try {
										stopTimeAtts.add(new Tuple<String, String>("departureOffSet", timeFormat.format(new Date(timeFormat.parse("00:00:00").getTime()+difference))));
									} catch (ParseException e) {
										e.printStackTrace();
									}
								}
								this.writeStartTag("stop", stopTimeAtts, true);
							}
							this.writeEndTag("routeProfile");
							//Route
							this.writeStartTag("route", new ArrayList<Tuple<String,String>>());
							Method m = GTFS2MATSimTransitScheduleFileWriter.class.getMethod("write"+Route.ROUTE_TYPES[route.getRouteType()]+"Route", new Class[] {Trip.class});
							m.invoke(this, new Object[]{trip});
							this.writeEndTag("route");
							//Departures
							int id = 1;
							this.writeStartTag("departures", new ArrayList<Tuple<String,String>>());
							for(Frequency frequency:trip.getFrequencies()) {
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
								departureAtts.add(new Tuple<String, String>("departureTime", timeFormat.format(trip.getStopTimes().get(trip.getStopTimes().firstKey()).getDepartureTime())));
								this.writeStartTag("departure", departureAtts, true);
							}
							this.writeEndTag("departures");
							this.writeEndTag("transitRoute");
						}
					}
					this.writeEndTag("transitLine");
				}
	
			}
			this.close();
		} catch (IOException e) {
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
	}
	/**
	 * Methods for processing one line of each file
	 * @param parts
	 * @param indices
	 */
	public void processCalendar(String[] parts, int[] indices) {
		boolean[] days = new boolean[7];
		for(int d=0; d<days.length; d++)
			days[d]=parts[d+indices[1]].equals("1");
		services.put(parts[indices[0]], new Service(days, parts[indices[2]], parts[indices[3]]));
	}
	public void processCalendarDate(String[] parts, int[] indices) {
		Service actual = services.get(parts[indices[0]]);
		if(parts[indices[2]].equals("2"))
			actual.addException(parts[indices[1]]);
		else
			actual.addAddition(parts[indices[1]]);
	}
	public void processShape(String[] parts, int[] indices) {
		Shape actual = shapes.get(parts[indices[0]]);
		if(actual==null) {
			actual = new Shape();
			shapes.put(parts[indices[0]], actual);
		}
		actual.addPoint(new CoordImpl(Double.parseDouble(parts[indices[1]]), Double.parseDouble(parts[indices[2]])),Integer.parseInt(parts[indices[3]]));
	}
	public void processRoute(String[] parts, int[] indices) {
		routes.put(parts[indices[0]], new Route(parts[indices[1]], Integer.parseInt(parts[indices[2]])));
	}
	public void processTrip(String[] parts, int[] indices) {
		Route route = routes.get(parts[indices[0]]);
		if(parts.length==5) {
			route.putTrip(parts[indices[1]], new Trip(services.get(parts[indices[2]]), shapes.get(parts[indices[3]])));
		}
		else
			route.putTrip(parts[indices[1]], new Trip(services.get(parts[indices[2]]), null));
	}
	public void processStopTime(String[] parts, int[] indices) {
		for(Route actualRoute:routes.values()) {
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
	public void processFrequency(String[] parts, int[] indices) {
		for(Route actualRoute:routes.values()) {
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
	 * Methods for writing the links of a trip
	 */
	public void writeBusRoute() {
		
	}
	//Main method
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		//fixGTFSTrainSingapore2();
		VisumFile2MatsimNetwork v2m = new VisumFile2MatsimNetwork();
		v2m.createNetworkFromVISUMFile(new File(VISUM_FILE));
		Network network = v2m.getNetwork();
		new NetworkCleaner().run(network);
		GTFS2MATSimTransitScheduleFileWriter g2m = new GTFS2MATSimTransitScheduleFileWriter(new File[]{new File("./data/gtfs/buses"),new File("./data/gtfs/trains")}, network, new String[]{"weekday","weeksatday","daily"});
		g2m.write("./data/gtfs/test2.xml");
	}

}
