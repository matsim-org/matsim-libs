package playground.sergioo.GTFS;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.visum.VisumNetwork;


public class GTFS2MATSimTransitScheduleFileWriter extends MatsimXmlWriter implements MatsimWriter{

	private static final String VISUM_FILE = "someone";
	/**
	 * The folder root of the GTFS files
	 */
	private File root;
	/**
	 * The network where the public transport will be performed
	 */
	private Network network;
	/**
	 * The types of dates that will be represented by the new file
	 */
	private String serviceId;
	
	/**
	 * 
	 * @param root
	 * @param network
	 */
	public GTFS2MATSimTransitScheduleFileWriter(File root, Network network, String serviceId) {
		super();
		this.root = root;
		this.network = network;
		this.serviceId = serviceId;
	}

	/**
	 * 
	 * @throws IOException
	 */
	public static void fixGTFSBusSingapore() throws IOException {
		File f=new File("C:/Users/sergioo/Desktop/Desktop/buses/trips.txt");
		File f2=new File("C:/Users/sergioo/Desktop/Desktop/buses/trips2.txt");
		BufferedReader reader = new BufferedReader(new FileReader(f));
		PrintWriter writer = new PrintWriter(f2);
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
	 * 
	 * @throws IOException
	 */
	public static void fixGTFSTrainSingapore() throws IOException {
		File f=new File("C:/Users/sergioo/Documents/2011/Work/FCL/Operations/Data/GoogleTransitFeed/ProcessedData/Trains/trips2.txt");
		File f2=new File("C:/Users/sergioo/Documents/2011/Work/FCL/Operations/Data/GoogleTransitFeed/ProcessedData/Trains/trips.txt");
		BufferedReader reader = new BufferedReader(new FileReader(f));
		PrintWriter writer = new PrintWriter(f2);
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
	
	@Override
	public void write(String filename) {
		try {
			//Stops
			File fileStops = new File(root.getPath()+"/stops.txt");
			BufferedReader reader = new BufferedReader(new FileReader(fileStops));
			SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
			this.openFile(filename);
			this.writeXmlHead();
			this.writeStartTag("transitStops", new ArrayList<Tuple<String,String>>());
			reader.readLine();
			String line = reader.readLine();
			while(line!=null) {
				String[] parts = line.split(",");
				List<Tuple<String, String>> params = new ArrayList<Tuple<String,String>>();
				params.add(new Tuple<String, String>("id", parts[0]));
				params.add(new Tuple<String, String>("x", parts[4]));
				params.add(new Tuple<String, String>("y", parts[3]));
				String id = "111";//((NetworkImpl)network).getNearestLink(new CoordImpl(Double.parseDouble(parts[4]), Double.parseDouble(parts[3]))).getId().toString();
				params.add(new Tuple<String, String>("linkRefId", id));
				params.add(new Tuple<String, String>("name", parts[2]));
				params.add(new Tuple<String, String>("isBlocking", "true"));
				this.writeStartTag("stopFacilities", params,true);
				line = reader.readLine();
			}
			writeEndTag("transitStops");
			reader.close();
			//Services
			HashMap<String, Service> services = new HashMap<String, Service>();
			File fileServices = new File(root.getPath()+"/calendar.txt");
			reader = new BufferedReader(new FileReader(fileServices));
			reader.readLine();
			line = reader.readLine();
			while(line!=null) {
				String[] parts = line.split(",");
				boolean[] days = new boolean[7];
				for(int d=0; d<days.length; d++)
					days[d]=parts[d+1].equals("1");
				services.put(parts[0], new Service(days, parts[8], parts[9]));
				line = reader.readLine();
			}
			reader.close();
			File fileServices2 = new File(root.getPath()+"/calendar_dates.txt");
			reader = new BufferedReader(new FileReader(fileServices2));
			reader.readLine();
			line = reader.readLine();
			while(line!=null) {
				String[] parts = line.split(",");
				Service actual = services.get(parts[0]);
				if(parts[2].equals("2"))
					actual.addException(parts[1]);
				else
					actual.addAddition(parts[1]);
				line = reader.readLine();
			}
			reader.close();
			//Shapes
			HashMap<String, Shape> shapes = new HashMap<String, Shape>(); 
			File fileShapes = new File(root.getPath()+"/shapes.txt");
			reader = new BufferedReader(new FileReader(fileShapes));
			reader.readLine();
			line = reader.readLine();
			while(line!=null) {
				String[] parts = line.split(",");
				Shape actual = shapes.get(parts[0]);
				if(actual==null) {
					Shape newShape = new Shape();
					newShape.addPoint(new CoordImpl(Double.parseDouble(parts[2]), Double.parseDouble(parts[2])));
					shapes.put(parts[0], newShape);
				}
				else
					actual.addPoint(new CoordImpl(Double.parseDouble(parts[2]), Double.parseDouble(parts[1])));
				line = reader.readLine();
			}
			reader.close();
			//Routes
			HashMap<String, Route> routes = new HashMap<String, Route>(); 
			File fileRoutes = new File(root.getPath()+"/routes.txt");
			reader = new BufferedReader(new FileReader(fileRoutes));
			reader.readLine();
			line = reader.readLine();
			while(line!=null) {
				String[] parts = line.split(",");
				routes.put(parts[0], new Route(parts[1], parts[3]));
				line = reader.readLine();
			}
			reader.close();
			//Trips
			File fileTrips = new File(root.getPath()+"/trips.txt");
			reader = new BufferedReader(new FileReader(fileTrips));
			reader.readLine();
			line = reader.readLine();
			while(line!=null) {
				String[] parts = line.split(",");
				Route route = routes.get(parts[0]);
				route.putTrip(parts[2], new Trip(services.get(parts[1]), shapes.get(parts[4])));
				line = reader.readLine();
			}
			reader.close();
			//StopTimes
			File fileStopTimes = new File(root.getPath()+"/stopTimes.txt");
			reader = new BufferedReader(new FileReader(fileStopTimes));
			reader.readLine();
			line = reader.readLine();
			while(line!=null) {
				String[] parts = line.split(",");
				for(Route actualRoute:routes.values()) {
					Trip trip = actualRoute.getTrips().get(parts[0]);
					if(trip!=null) {
						try {
							trip.putStopTime(Integer.parseInt(parts[4]), new StopTime(sdf.parse(parts[1]),sdf.parse(parts[2]),parts[3]));
						} catch (NumberFormatException e) {
							e.printStackTrace();
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
				}
				line = reader.readLine();
			}
			reader.close();
			//Frequencies
			File fileFrequencies = new File(root.getPath()+"/frequencies.txt");
			reader = new BufferedReader(new FileReader(fileFrequencies));
			reader.readLine();
			line = reader.readLine();
			while(line!=null) {
				String[] parts = line.split(",");
				for(Route actualRoute:routes.values()) {
					Trip trip = actualRoute.getTrips().get(parts[0]);
					if(trip!=null) {
						try {
							trip.addFrequency(new Frequency(sdf.parse(parts[1]), sdf.parse(parts[2]), Integer.parseInt(parts[3])));
						} catch (NumberFormatException e) {
							e.printStackTrace();
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
				}
				line = reader.readLine();
			}
			reader.close();
			//Public Transport Schedule
			for(String routeKey:routes.keySet()) {
				Route route = routes.get(routeKey);
				List<Tuple<String,String>> routeAtts = new ArrayList<Tuple<String,String>>();
				routeAtts.add(new Tuple<String, String>("id", routeKey));
				this.writeStartTag("transitLine", routeAtts);
				for(String tripKey:route.getTrips().keySet()) {
					Trip trip = route.getTrips().get(tripKey);
					if(services.get(serviceId).equals(trip.getService())) {
						List<Tuple<String,String>> tripAtts = new ArrayList<Tuple<String,String>>();
						tripAtts.add(new Tuple<String, String>("id", tripKey));
						this.writeStartTag("transitRoute", tripAtts);
						//Mode
						this.writeStartTag("transportMode", new ArrayList<Tuple<String,String>>());
						this.writeContent("bus", true);
						this.writeEndTag("transportMode");
						//Route profile
						this.writeStartTag("routeProfile", new ArrayList<Tuple<String,String>>());
						Date startTime = trip.getFrequencies().get(0).getStartTime();
						for(Integer stopTimeKey:trip.getStopTimes().keySet()) {
							StopTime stopTime = trip.getStopTimes().get(stopTimeKey);
							List<Tuple<String,String>> stopTimeAtts = new ArrayList<Tuple<String,String>>();
							stopTimeAtts.add(new Tuple<String, String>("refId", stopTime.getStopId()));
							if(!stopTimeKey.equals(1)) {
								long difference = stopTime.getArrivalTime().getTime()-startTime.getTime();
								stopTimeAtts.add(new Tuple<String, String>("arrivalOffSet", sdf.format(new Date(difference))));
							}
							if(!stopTimeKey.equals(trip.getStopTimes().size())) {
								long difference = stopTime.getDepartureTime().getTime()-startTime.getTime();
								stopTimeAtts.add(new Tuple<String, String>("departureOffSet", sdf.format(new Date(difference))));
							}
							this.writeStartTag("stop", stopTimeAtts, true);
						}
						this.writeEndTag("routeProfile");
						//Route
						this.writeStartTag("route", new ArrayList<Tuple<String,String>>());
						Shape shape = trip.getShape();
						Link actualLink = null;
						for(Coord point:shape.getPoints()) {
							Link link = ((NetworkImpl)network).getNearestLink(point);
							if(actualLink==null || !link.equals(actualLink))
								if(actualLink==null || actualLink.getToNode().equals(link.getFromNode())) {
									actualLink = link;
									List<Tuple<String,String>> linkAtts = new ArrayList<Tuple<String,String>>();
									linkAtts.add(new Tuple<String, String>("refId", link.getId().toString()));
									this.writeStartTag("link", linkAtts, true);
								}
								else
									System.out.println("paila, hay puntos en el shape que saltan mas de un link");
						}
						this.writeEndTag("route");
						//Departures
						int id = 1;
						int vid = 1;
						this.writeStartTag("departures", new ArrayList<Tuple<String,String>>());
						for(Frequency frequency:trip.getFrequencies()) {
							for(Date actualTime = frequency.getStartTime(); actualTime.before(frequency.getEndTime()); actualTime.setTime(actualTime.getTime()+frequency.getSecondsPerDeparture()*1000)) {
								List<Tuple<String,String>> departureAtts = new ArrayList<Tuple<String,String>>();
								departureAtts.add(new Tuple<String, String>("id", Integer.toString(id)));
								departureAtts.add(new Tuple<String, String>("departureTime", sdf.format(actualTime)));
								departureAtts.add(new Tuple<String, String>("vehicleRefId", Integer.toString(vid)));
								id++;
								vid++;
							}
						}
						this.writeEndTag("departures");
						this.writeEndTag("transitRoute");
					}
				}
				this.writeEndTag("transitLine");
			}
			this.close();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	//Main method
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		fixGTFSTrainSingapore();
		/*VisumNetwork vNetwork = null;
		//new VisumNetworkReader(vNetwork).read(VISUM_FILE);
		GTFS2MATSimTransitScheduleFileWriter a = new GTFS2MATSimTransitScheduleFileWriter(new File("./data/gtfs/buses"),null);
		a.write("./data/test1.xml");*/
	}

}
