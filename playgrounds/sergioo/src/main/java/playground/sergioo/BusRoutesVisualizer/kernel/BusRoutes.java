package playground.sergioo.BusRoutesVisualizer.kernel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.sergioo.BusRoutesVisualizer.gui.Window;
import playground.sergioo.GTFS2PTSchedule.GTFSDefinitions;
import playground.sergioo.GTFS2PTSchedule.Stop;

public class BusRoutes {
	
	//Constants
	/**
	 * Pre-processed information files
	 */
	private final static File[] PREFILES = {new File("./data/paths/fixedStops.txt"),new File("./data/paths/bases.txt"),new File("./data/paths/finishedTrips.txt")};
	public static final File NEW_NETWORK_NODES_FILE = new File("./data/paths/newNetworkNodes.txt");
	public static final File NEW_NETWORK_LINKS_FILE = new File("./data/paths/newNetworkLinks.txt");
	
	//Attributes
	/**
	 * The network where the public transport will be performed
	 */
	private final Network network;
	/**
	 * Trips with established paths
	 */
	private final Map<String, String[]> finishedTrips;
	/**
	 * The stops reference
	 */
	private final Map<String, Stop> stops;
	
	//Methods
	/**
	 * @param network
	 * @param stops
	 * @throws IOException 
	 */
	public BusRoutes(Network network) throws IOException {
		super();
		this.network = network;
		stops = new HashMap<String, Stop>();
		File file = new File("./data/gtfs/buses"+"/"+GTFSDefinitions.STOPS.fileName);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		int[] indices = GTFSDefinitions.STOPS.getIndices(reader.readLine());
		String line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			processStop(parts, indices);
			line = reader.readLine();
		}
		reader.close();
		finishedTrips = new HashMap<String, String[]>();
		reader = new BufferedReader(new FileReader(PREFILES[2]));
		line = reader.readLine();
		while(line!=null) {
			String[] links = reader.readLine().split(";");
			finishedTrips.put(line, links);
			line = reader.readLine();
		}
		reader.close();
		reader = new BufferedReader(new FileReader(PREFILES[0]));
		line = reader.readLine();
		while(line!=null) {
			Stop stop = stops.get(line);
			stop.setLinkId(reader.readLine());
			stop.setFixedLinkId();
			line = reader.readLine();
		}
		reader.close();
	}
	/**
	 * Adds an stop
	 * @param parts
	 * @param indices
	 */
	public void processStop(String[] parts, int[] indices) {
		stops.put(parts[indices[0]],new Stop(new CoordImpl(Double.parseDouble(parts[indices[1]]),Double.parseDouble(parts[indices[2]])),parts[indices[3]],true));
	}
	/**
	 * Creates and shows the routes tree
	 * @param code
	 * @param numTransfers
	 */
	private void visualizeTree(String code, int numTransfers) {
		RouteTree routeTree = new RouteTree(network, code, numTransfers, finishedTrips, stops);
		Window window = new Window("Bus Stop: "+code+" ("+numTransfers+" transfers"+")", routeTree);
		window.setVisible(true);
	}
	/**
	 * 
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario);
		matsimNetworkReader.readFile("./data/networks/singapore_initial.xml");
		Network network = scenario.getNetwork();
		BusRoutes busRoutes = new BusRoutes(network);
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Write the code of the desired stop");
		String code = reader.readLine();
		System.out.println("Write the number of transfers (0..n)");
		int numTransfers = Integer.parseInt(reader.readLine());
		busRoutes.visualizeTree(code,numTransfers);
	}
	
}
