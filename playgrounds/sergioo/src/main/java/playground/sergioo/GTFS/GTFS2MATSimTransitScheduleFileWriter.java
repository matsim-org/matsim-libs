package playground.sergioo.GTFS;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
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
	 * 
	 * @param root
	 * @param network
	 */
	public GTFS2MATSimTransitScheduleFileWriter(File root, Network network) {
		super();
		this.root = root;
		this.network = network;
	}

	/**
	 * 
	 * @throws IOException
	 */
	public static void fixGTFSSingapore() throws IOException {
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
	
	@Override
	public void write(String filename) {
		try {
			//Stops
			File fileStops = new File(root.getPath()+"/stops.txt");
			BufferedReader reader = new BufferedReader(new FileReader(fileStops));
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
			//TODO
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
					newShape.addPoint(new Location(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
					shapes.put(parts[0], newShape);
				}
				else
					actual.addPoint(new Location(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
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
					Trip trip = actualRoute.getTrip(parts[0]);
					if(trip!=null)
						trip.putStopTime(Integer.parseInt(parts[4]), new StopTime(parts[1],parts[2],parts[3]));
				}
			}
			reader.close();
			//Public Transport Schedule
			
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
		VisumNetwork vNetwork = null;
		//new VisumNetworkReader(vNetwork).read(VISUM_FILE);
		GTFS2MATSimTransitScheduleFileWriter a = new GTFS2MATSimTransitScheduleFileWriter(new File("./data/gtfs/buses"),null);
		a.write("./data/test1.xml");
	}

}
