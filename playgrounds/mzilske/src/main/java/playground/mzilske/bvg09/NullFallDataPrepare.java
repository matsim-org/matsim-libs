/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.mzilske.bvg09;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.transitSchedule.TransitRouteImpl;
import org.matsim.transitSchedule.TransitScheduleWriterV1;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetworkReader;
import org.matsim.visum.VisumNetwork.LineRouteItem;
import org.matsim.visum.VisumNetwork.TransitLineRoute;


public class NullFallDataPrepare {

	private static final Logger log = Logger.getLogger(NullFallDataPrepare.class);

	private static String OutPath = "e:/_out/nullfall_berlin_brandenburg/";
	private static String InVisumNetFile = "d:/Berlin/BVG/berlin-bvg09/urdaten/nullfall2009-05-25.net";
//	private static List<String> transitLineFilter = Arrays.asList("U-8","B-M44","B-344");
	private static List<String> transitLineFilter = null;

	// OUTPUT FILES
	private static String OutNetworkFile = OutPath + "intermediateNetwork.xml";
	private static String OutTransitScheduleFile = OutPath + "intermediateTransitSchedule.xml";
	private static String OutVehicleFile = OutPath + "intermediateVehicles.xml";
	private final ScenarioImpl scenario;
	private final Config config;
	private Map<Node, Link> initialLinks = new HashMap<Node, Link>();
	private VisumNetwork vNetwork;

	private final Pattern taktPattern = Pattern.compile("([0-9]*)s");



	public NullFallDataPrepare() {
		this.scenario = new ScenarioImpl();
		this.config = this.scenario.getConfig();
	}

	private void prepareConfig() {
		this.config.scenario().setUseTransit(true);
		this.config.scenario().setUseVehicles(true);
		this.config.network().setInputFile(OutNetworkFile);
		this.config.network().setOutputFile(OutNetworkFile);
	}

	private void convertNetwork() {
		final NetworkLayer network = scenario.getNetwork();
		StreamingVisumNetworkReader streamingVisumNetworkReader = new StreamingVisumNetworkReader();
		
		VisumNetworkRowHandler nodeRowHandler = new VisumNetworkRowHandler() {
			
			@Override
			public void handleRow(Map<String, String> row) {
				Id id = new IdImpl(row.get("NR"));
				Coord coord = new CoordImpl(Double.parseDouble(row.get("XKOORD").replace(',', '.')), Double.parseDouble(row.get("YKOORD").replace(',', '.')));
				network.createAndAddNode(id, coord);
			}
			
		};
		streamingVisumNetworkReader.addRowHandler("KNOTEN", nodeRowHandler);
		
		VisumNetworkRowHandler edgeRowHandler = new VisumNetworkRowHandler() {
		
			@Override
			public void handleRow(Map<String, String> row) {	
				String nr = row.get("NR");
				IdImpl id = new IdImpl(nr);
				IdImpl fromNodeId = new IdImpl(row.get("VONKNOTNR"));
				IdImpl toNodeId = new IdImpl(row.get("NACHKNOTNR"));
				Link lastEdge = network.getLinks().get(id);
				if (lastEdge != null) {
					if (lastEdge.getFromNode().getId().equals(toNodeId) && lastEdge.getToNode().getId().equals(fromNodeId)) {
						id = new IdImpl(nr + 'R');
					} else {
						throw new RuntimeException("Duplicate edge.");
					}
				}
				double length = Double.parseDouble(row.get("LAENGE").replace(',', '.'));
				String edgeTypeIdString = row.get("TYPNR");
				if (!edgeTypeIdString.isEmpty()) {
					IdImpl edgeTypeId = new IdImpl(edgeTypeIdString);
				}
				
//				ArrayList<Float> tValues = new ArrayList<Float>();
//				for (String letter : Arrays.asList("B", "F", "P", "R", "S", "T", "U", "V")) {
//					String key = "T-OEVSYS(" + letter + ")";
//					String value = row.get(key);
//					if (value != null) {
//						float t = Float.parseFloat(value.substring(0, value.length() - 1));
//						if (t != 0.0) {
//							tValues.add(t);
//						}
//					}
//				}
//				float freespeed;
//				if (tValues.size() == 1) {
//					freespeed = tValues.get(0);
//				} else {
					// Mehr als einer dieser Werte gesetzt.
//					throw new RuntimeException(tValues.toString());
//				}
				// capacity of 2000 isn't enough for areas with heavy pt traffic (zoo, spandau, alex)
				network.createAndAddLink(id, network.getNodes().get(fromNodeId), network.getNodes().get(toNodeId), length * 1000, FreeSpeedCalculator.calculateFreeSpeedForEdge(row), 99999, 1);			
			}
			
		};
		streamingVisumNetworkReader.addRowHandler("STRECKE", edgeRowHandler);
		streamingVisumNetworkReader.read(InVisumNetFile);
	}

	private void convertSchedule() {
		log.info("converting visum data to TransitSchedule.");
		Visum2TransitSchedule converter = new Visum2TransitSchedule(vNetwork, this.scenario.getTransitSchedule(), this.scenario.getVehicles());
		converter.setTransitLineFilter(transitLineFilter);
		// configure how transport modes must be converted
		// the ones for Berlin
		converter.registerTransportMode("B", TransportMode.bus); // Bus Berlin
		converter.registerTransportMode("F", TransportMode.walk); // Fuss
//		converter.registerTransportMode("K", TransportMode.bus);
//		converter.registerTransportMode("L", TransportMode.other);
		converter.registerTransportMode("P", TransportMode.bus); // Bus Umland
		converter.registerTransportMode("R", TransportMode.train); // Regionalbahn
		converter.registerTransportMode("S", TransportMode.train); // S-Bahn
		converter.registerTransportMode("T", TransportMode.tram); // Tram Berlin
		converter.registerTransportMode("U", TransportMode.train); // U-Bahn
		converter.registerTransportMode("V", TransportMode.tram); // Tram Umland
//		converter.registerTransportMode("W", TransportMode.bus);
//		converter.registerTransportMode("Z", TransportMode.train);
		converter.convert();
	}

	private void convertRoutes() {
		Iterator<TransitLine> transitLineI = this.scenario.getTransitSchedule().getTransitLines().values().iterator();
		int linesSkipped = 0;
		while (transitLineI.hasNext()) {
			TransitLine transitLine = transitLineI.next();
			try {
				convertLine(transitLine);
			} catch (RuntimeException e) {
				e.printStackTrace();
				transitLineI.remove();
				++linesSkipped;
			}
		}
		log.info("Skipped " + linesSkipped + " lines.");
	}

	private void convertLine(TransitLine transitLine) {
		for (TransitRoute transitRouteI: transitLine.getRoutes().values()) {
			VisumNetwork.TransitLineRoute transitLineRoute = getTransitLineRoute(transitLine, transitRouteI);
			List<VisumNetwork.LineRouteItem> lineRouteItems = getLineRouteItems(transitLine, transitRouteI);
			List<Id> linkIds = new ArrayList<Id>();
			Iterator<VisumNetwork.LineRouteItem> i = lineRouteItems.iterator();
			VisumNetwork.LineRouteItem initialLineRouteItem = i.next();
			Link initialLink = createOrFindInitialLink(initialLineRouteItem);
			linkIds.add(initialLink.getId());
			VisumNetwork.LineRouteItem previousLineRouteItem = initialLineRouteItem;
			while (i.hasNext()) {
				VisumNetwork.LineRouteItem nextLineRouteItem = i.next();
				Link link = findLink(previousLineRouteItem, nextLineRouteItem);
				// check if stop was on link instead of node
				if(link == null){
					log.warn("No link found. Skipping to next node. Please check line " + transitLine.getId() + " manually.");
					continue;					
				} else {
					linkIds.add(link.getId());
					previousLineRouteItem = nextLineRouteItem;
				}
			}
			NetworkRoute linkNetworkRoute = RouteUtils.createNetworkRoute(linkIds, this.scenario.getNetwork());
			transitRouteI.setRoute(linkNetworkRoute);
			if (transitLineRoute.takt != null) {
				int taktInSeconds = parseTaktToSeconds(transitLineRoute.takt);
				Collection<Departure> departures = createDepartures(transitLine, transitRouteI, taktInSeconds);
				for (Departure departure : departures) {
					transitRouteI.addDeparture(departure);
				}
			}
		}
	}

	private int parseTaktToSeconds(String takt) {
		Matcher m = taktPattern.matcher(takt);
		if (!m.matches()) {
			throw new RuntimeException();
		}
		return Integer.parseInt(m.group(1));
	}

	private Collection<Departure> createDepartures(TransitLine transitLine, TransitRoute transitRoute, int taktSeconds) {
		List<Departure> departures = new ArrayList<Departure>();
		int n=0;
		for (int i=0; i < 86400; i += taktSeconds) {
			Departure departure = scenario.getTransitSchedule().getFactory().createDeparture(new IdImpl(transitRoute.getId().toString()+"-"+String.format("%05d",n++)), i);
			departures.add(departure);
		}
		return departures;
	}

	private TransitLineRoute getTransitLineRoute(TransitLine transitLine,
			TransitRoute transitRoute) {
		TransitRouteImpl transitRouteImpl = (TransitRouteImpl) transitRoute;
		for (TransitLineRoute transitLineRoute : vNetwork.lineRoutes.values()) {
			if (transitLineRoute.lineName.equals(transitLine.getId())) {
				if (transitLineRoute.id.toString().equals(transitRouteImpl.getLineRouteName())) {
					if (transitLineRoute.DCode.toString().equals(transitRouteImpl.getDirection())) {
						return transitLineRoute;
					}
				}
			}
		}
		throw new RuntimeException();
	}

	private List<LineRouteItem> getLineRouteItems(TransitLine transitLine, TransitRoute transitRouteI) {
		TransitRouteImpl transitRoute = (TransitRouteImpl) transitRouteI;
		List<LineRouteItem> lineRouteItems = new ArrayList<LineRouteItem>();
		for (LineRouteItem lineRouteItem : vNetwork.lineRouteItems.values()) {
			if (lineRouteItem.lineName.equals(transitLine.getId().toString()) && lineRouteItem.lineRouteName.equals(transitRoute.getLineRouteName()) && lineRouteItem.DCode.equals(transitRoute.getDirection())) {
				lineRouteItems.add(lineRouteItem);
			}
		}
		Collections.sort(lineRouteItems, new Comparator<LineRouteItem>() {
			public int compare(LineRouteItem o1, LineRouteItem o2) {
				return new Integer(o1.index).compareTo(new Integer(o2.index));
			}
		});
		return lineRouteItems;
	}

	private Link findLink(LineRouteItem previousLineRouteItem,
			LineRouteItem nextLineRouteItem) {
		NetworkLayer network = scenario.getNetwork();
		return findLink(network.getNodes().get(previousLineRouteItem.nodeId), network.getNodes().get(nextLineRouteItem.nodeId));
	}

	private Link findLink(Node prevNode, Node node) {
		Link foundLink = null;
		for (Link link : prevNode.getOutLinks().values()) {
			if (link.getToNode().equals(node)) {
				foundLink = link;
				break;
			}
		}
		if (foundLink == null) {
//			throw new RuntimeException();
			log.warn("There was no link found from " + prevNode + " to " + node + " Could be a stop situated on a link instead on a node.");
		}
		return foundLink;
	}

	private Link createOrFindInitialLink(LineRouteItem initialLineRouteItem) {
		NetworkLayer network = scenario.getNetwork();
		Node node = network.getNodes().get(initialLineRouteItem.nodeId);
		Link initialLink = initialLinks.get(node);
		if (initialLink == null) {
			Id id = new IdImpl("initial_" + node.getId());
			initialLink = network.createAndAddLink(id, node, node, 10, 99, 99999, 1);
			initialLinks.put(node, initialLink);
		}
		return initialLink;
	}

	private void readVisumNetwork()  {
		vNetwork = new VisumNetwork();
		log.info("reading visum network.");
		try {
			new VisumNetworkReader(vNetwork).read(InVisumNetFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeNetworkAndScheduleAndVehicles() throws IOException,
			FileNotFoundException {
		NetworkLayer network = scenario.getNetwork();
		log.info("writing network to file.");
		new NetworkWriter(network).write(OutNetworkFile);
		log.info("writing TransitSchedule to file.");
		new TransitScheduleWriterV1(this.scenario.getTransitSchedule()).write(OutTransitScheduleFile);
		log.info("writing vehicles to file.");
		new VehicleWriterV1(this.scenario.getVehicles()).writeFile(OutVehicleFile);
		try {
			new TransitScheduleWriter(this.scenario.getTransitSchedule()).writeFile(OutTransitScheduleFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		convertVisumNetwork();
	}

	private static void convertVisumNetwork() {
		NullFallDataPrepare app = new NullFallDataPrepare();
		app.prepareConfig();
		app.readVisumNetwork();
		app.convertNetwork();
		app.convertSchedule();
		app.convertRoutes();
		try {
			app.writeNetworkAndScheduleAndVehicles();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("done.");
	}

}
