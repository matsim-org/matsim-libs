package playground.sergioo.GTFS2PTSchedule.PathEditor.kernel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;

import playground.sergioo.GTFS2PTSchedule.PathEditor.gui.Window;
import playground.sergioo.GTFS2PTSchedule.Route;
import playground.sergioo.GTFS2PTSchedule.Stop;
import playground.sergioo.GTFS2PTSchedule.StopTime;
import playground.sergioo.GTFS2PTSchedule.Trip;

public class RoutesPathsGenerator {
	
	//Constants
	/**
	 * Pre-processed information files
	 */
	private final static String[] PREFILES = {"fixedStops.txt","bases.txt","finishedTrips.txt"};
	public static final File NEW_NETWORK_NODES_FILE = new File("./data/paths/newNetworkNodes2.txt");
	public static final File NEW_NETWORK_LINKS_FILE = new File("./data/paths/newNetworkLinks2.txt");
	
	//Attributes
	/**
	 * The network where the public transport will be performed
	 */
	private Network network;
	/**
	 * The routes reference
	 */
	private Map<String, Route> routes;
	/**
	 * The stops reference
	 */
	private Map<String, Stop> stops;
	/**
	 * Paths that are bases for calculate others
	 */
	private Map<String, String[]> bases;
	/**
	 * Trips with established paths
	 */
	private Map<String, String[]> finishedTrips;

	//Methods
	/**
	 * @param network
	 * @param stops
	 * @throws IOException 
	 */
	public RoutesPathsGenerator(Network network, File root, Map<String, Route> routes, Map<String, Stop> stops) throws IOException {
		super();
		this.network = network;
		this.routes = routes;
		this.stops = stops;
		File  tempFolder = new File(root.getPath()+"/temp");
		if(!tempFolder.exists())
			if(!tempFolder.mkdir())
				throw new IOException();
		File fixedStopsFile = new File(tempFolder.getPath()+"/"+PREFILES[0]);
		if(!fixedStopsFile.exists())
			if(!fixedStopsFile.createNewFile())
				throw new IOException();
		BufferedReader reader = new BufferedReader(new FileReader(fixedStopsFile));
		String line = reader.readLine();
		while(line!=null) {
			Stop stop = stops.get(line);
			stop.setLinkId(reader.readLine());
			stop.setFixedLinkId();
			line = reader.readLine();
		}
		reader.close();
		bases = new HashMap<String, String[]>();
		File basesFile = new File(tempFolder.getPath()+"/"+PREFILES[1]);
		if(!basesFile.exists())
			if(!basesFile.createNewFile())
				throw new IOException();
		reader = new BufferedReader(new FileReader(basesFile));
		line = reader.readLine();
		while(line!=null) {
			String[] links = reader.readLine().split(";");
			bases.put(line, links);
			line = reader.readLine();
		}
		reader.close();
		finishedTrips = new HashMap<String, String[]>();
		File finishedTripsFile = new File(tempFolder.getPath()+"/"+PREFILES[2]);
		if(!finishedTripsFile.exists())
			if(!finishedTripsFile.createNewFile())
				throw new IOException();
		reader = new BufferedReader(new FileReader(finishedTripsFile));
		line = reader.readLine();
		while(line!=null) {
			String[] links = reader.readLine().split(";");
			finishedTrips.put(line, links);
			line = reader.readLine();
		}
		reader.close();
		
	}
	public void run() throws IOException {
		System.out.println("Total number of routes: "+routes.size());
		int i=0;
		for(Entry<String,Route> route:routes.entrySet()) {
			for(Entry<String,Trip> tripEntry:route.getValue().getTrips().entrySet())
				calculateBusLinksSequence(tripEntry, route.getValue());
			i++;
			System.out.println(i+". "+route.getKey()+" ("+route.getValue().getTrips().size()+")");
		}
	}
	/**
	 * 
	 * @param tripEntry
	 * @param withShape
	 * @param route
	 * @throws IOException
	 */
	private void calculateBusLinksSequence(Entry<String,Trip> tripEntry, Route route) throws IOException {
		List<Link> links;
		String[] linksS = finishedTrips.get(tripEntry.getKey());
		if(linksS==null) {
			String baseId = route.getShortName()+(tripEntry.getKey().contains("_1")?"_1":"_2");
			linksS = bases.get(baseId);
			boolean withBase = false;
			links = new ArrayList<Link>();
			Window window;
			if(linksS==null)
				window = new Window(tripEntry.getKey(),network,tripEntry.getValue(),stops,links,this);
			else {
				window = new Window(tripEntry.getKey(),network,tripEntry.getValue(),stops,linksS,links,this);
				withBase = true;
			}
			window.setVisible(true);
			while(links.size()==0)
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			window.setVisible(false);
			PrintWriter writer = new PrintWriter(new FileWriter(PREFILES[0],true));
			for(StopTime stopTime: tripEntry.getValue().getStopTimes().values()) {
				Stop stop = stops.get(stopTime.getStopId());
				if(!stop.isFixedLinkId()) {
					stop.setFixedLinkId();
					writer.println(stopTime.getStopId());
					writer.println(stop.getLinkId());
				}
			}
			writer.close();
			if(!withBase) {
				writer = new PrintWriter(new FileWriter(PREFILES[1],true));
				writer.println(baseId);
				String[] linksA = new String[links.size()];
				int i=0;
				for(Link link:links) {
					writer.print(link.getId()+";");
					linksA[i] = link.getId().toString();
					i++;
				}
				writer.println();
				writer.close();
				bases.put(baseId, linksA);
			}
			writer = new PrintWriter(new FileWriter(PREFILES[2],true));
			writer.println(tripEntry.getKey());
			String linksT = "";
			String[] linksA = new String[links.size()];
			int i=0;
			for(Link link:links) {
				linksT+=link.getId()+";";
				linksA[i]=link.getId().toString();
				i++;
			}
			writer.println(linksT);
			finishedTrips.put(tripEntry.getKey(), linksA);
			writer.close();
		}
		else {
			links = new ArrayList<Link>();
			for(String link:linksS)
				links.add(network.getLinks().get(new IdImpl(link)));
		}
		for(Link link:links) {
			Set<String> modes = new HashSet<String>(link.getAllowedModes());
			modes.add(route.getRouteType().name);
			link.setAllowedModes(modes);
		}
		tripEntry.getValue().setRoute(links);
	}
	/**
	 * 
	 * @param selectedStopId
	 * @throws IOException
	 */
	public void restartTripsStops(String selectedStopId) throws IOException {
		PrintWriter writer = new PrintWriter(PREFILES[0]);
		for(Entry<String,Stop> stopE: stops.entrySet()) {
			if(stopE.getValue().isFixedLinkId()) {
				writer.println(stopE.getKey());
				writer.println(stopE.getValue().getLinkId());
			}
		}
		writer.close();
		String baseId = null;
		writer = new PrintWriter(PREFILES[2]);
		for(Entry<String,String[]> fTripE:finishedTrips.entrySet()) {
			boolean isOk = true;
			for(Entry<String,Route> routeE:routes.entrySet()) {
				Trip trip = routeE.getValue().getTrips().get(fTripE.getKey());
				if(trip!=null)
					for(StopTime stopTime:trip.getStopTimes().values())
						if(stopTime.getStopId().equals(selectedStopId)) {
							isOk=false;
							baseId = routeE.getValue().getShortName()+(fTripE.getKey().contains("_1")?"_1":"_2");
						}
			}
			if(isOk) {
				writer.println(fTripE.getKey());
				String linksT = "";
				for(String link:fTripE.getValue())
					linksT+=link+";";
				writer.println(linksT);
			}
			else
				bases.remove(baseId);
		}
		writer.close();
		writer = new PrintWriter(PREFILES[1]);
		for(Entry<String,String[]> baseE:bases.entrySet()) {
			writer.println(baseE.getKey());
			String linksT = "";
			for(String link:baseE.getValue())
				linksT+=link+";";
			writer.println(linksT);
		}
		writer.close();
	}
	public Collection<Link> getAllStopLinks() {
		Set<Link> allStopLinks = new HashSet<Link>();
		for(Stop stop:stops.values())
			if(stop.isFixedLinkId())
				allStopLinks.add(network.getLinks().get(new IdImpl(stop.getLinkId())));
		return allStopLinks;
	}
	
}
