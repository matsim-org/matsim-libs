/* *********************************************************************** *
 * project: org.matsim.*
 * UCSBptNetworkParser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.ucsb.transit;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkWriteAsTable;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author balmermi
 *
 */
public class UCSBptNetworkParser {

	private final static Logger log = Logger.getLogger(UCSBptNetworkParser.class);
	
	private static final String ILIN_NAME = "ilin";
	private static final String DIST_NAME = "dist";
	private static final String NODEFLAG_NAME = "att";
	private static final String DESC_NAME = "loc";
	private static final String PARENT_ILIN_NAME = "parent";
	private static final String ROUTE_NAME = "rte";
	private static final String MODE_NAME = "mode";

	
	static class IlinTripDistBlock {
		int ilin = -1;
		int trip = -1;
		int dist = -1;
		List<Integer> depTimes = new ArrayList<Integer>();
	}

	
	public void parse(String ptTimesFile, ObjectAttributes nodeObjectAttributes, Scenario scenario) {
		TransitSchedule transitSchedule = scenario.getTransitSchedule();
		
		// create Stop Facilities
		for (Node node : scenario.getNetwork().getNodes().values()) {
			if (!node.getId().toString().startsWith("pn-")) {
				TransitStopFacility transitStopFacility =
						transitSchedule.getFactory().createTransitStopFacility(Id.create("stop-"+node.getId().toString(), TransitStopFacility.class), new CoordImpl(node.getCoord()), false);
				transitSchedule.addStopFacility(transitStopFacility);
				transitStopFacility.setName((String)nodeObjectAttributes.getAttribute(node.getId().toString(),DESC_NAME));

				if (node.getInLinks().size() != 1) { throw new RuntimeException("node id="+node.getId()+": number of inLinks="+node.getInLinks().size()+" should not happen!"); }
				transitStopFacility.setLinkId(node.getInLinks().values().iterator().next().getId());
			}
		}
		
		// create schedule
		int lineCnt = 0;
		try {
			BufferedReader br = IOUtils.getBufferedReader(ptTimesFile);
			
			// header
			String curr_line = br.readLine(); lineCnt++;
			String[] heads = curr_line.split("\t", -1);
			Map<String,Integer> column = new LinkedHashMap<String, Integer>(heads.length);
			for (int i=0; i<heads.length; i++) { column.put(heads[i],i); }
			log.info("columns of input file: "+ptTimesFile+" ...");
			for (String head : column.keySet()) { log.info(column.get(head)+":"+head); }
			log.info("done. (columns of input file)");

			// data
			int prevIlin = -1;
			int prevTrip = -1;
			int prevDist = -1;
			IlinTripDistBlock ilinTripDistBlock = null;
			List<IlinTripDistBlock> ilinTripDistBlocks = null;
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				lineCnt++;
				
				int ilin = new Integer(entries[column.get(ILIN_NAME)]).intValue();
				int trip = new Integer(entries[column.get("trip")]).intValue();
				int dist = new Integer(entries[column.get(DIST_NAME)]).intValue();
				int depTime = new Integer(entries[column.get("timexsec")]).intValue();
				
				if (ilin != prevIlin) {
					if (ilinTripDistBlocks != null) {
						log.info(lineCnt+" (ilin != prevIlin): adding new line");
						ilinTripDistBlocks.add(ilinTripDistBlock);
						checkIlinTripDistBlocks(ilinTripDistBlocks);
						handleIlinTripDistBlocks(transitSchedule,ilinTripDistBlocks);
					}
					ilinTripDistBlocks = new ArrayList<IlinTripDistBlock>();
					ilinTripDistBlock = new IlinTripDistBlock();
					ilinTripDistBlock.ilin = ilin;
					ilinTripDistBlock.trip = trip;
					ilinTripDistBlock.dist = dist;
				} else if (trip != prevTrip) {
					log.info(lineCnt+" (trip != prevTrip): adding new line route");
					ilinTripDistBlocks.add(ilinTripDistBlock);
					checkIlinTripDistBlocks(ilinTripDistBlocks);
					handleIlinTripDistBlocks(transitSchedule,ilinTripDistBlocks);
					ilinTripDistBlocks = new ArrayList<IlinTripDistBlock>();

					ilinTripDistBlock = new IlinTripDistBlock();
					ilinTripDistBlock.ilin = ilin;
					ilinTripDistBlock.trip = trip;
					ilinTripDistBlock.dist = dist;
				} else if (dist < prevDist) {
					log.info(lineCnt+" (dist < prevDist): adding new line route");
					ilinTripDistBlocks.add(ilinTripDistBlock);
					checkIlinTripDistBlocks(ilinTripDistBlocks);
					handleIlinTripDistBlocks(transitSchedule,ilinTripDistBlocks);
					ilinTripDistBlocks = new ArrayList<IlinTripDistBlock>();

					ilinTripDistBlock = new IlinTripDistBlock();
					ilinTripDistBlock.ilin = ilin;
					ilinTripDistBlock.trip = trip;
					ilinTripDistBlock.dist = dist;
				} else if (dist != prevDist) {
					ilinTripDistBlocks.add(ilinTripDistBlock);
						
					ilinTripDistBlock = new IlinTripDistBlock();
					ilinTripDistBlock.ilin = ilin;
					ilinTripDistBlock.trip = trip;
					ilinTripDistBlock.dist = dist;
				}
				ilinTripDistBlock.depTimes.add(depTime);
				prevDist = dist;
				prevTrip = trip;
				prevIlin = ilin;
			}
			log.info(lineCnt+" (last one): adding new line");
			ilinTripDistBlocks.add(ilinTripDistBlock);
			checkIlinTripDistBlocks(ilinTripDistBlocks);
			handleIlinTripDistBlocks(transitSchedule,ilinTripDistBlocks);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		log.info(lineCnt+" lines parsed");
	}
	
	private final void checkIlinTripDistBlocks(List<IlinTripDistBlock> ilinTripDistBlocks) {
		if (ilinTripDistBlocks == null) { throw new RuntimeException("ilinTripDistBlocks == null!"); }
		if (ilinTripDistBlocks.isEmpty()) { throw new RuntimeException("ilinTripDistBlocks is empty!"); }
		
		int maxTimeEntries = ilinTripDistBlocks.get(0).depTimes.size();
		boolean isBalanced = true;
		for (IlinTripDistBlock block : ilinTripDistBlocks) {
			if (block.depTimes.size() != maxTimeEntries) { isBalanced = false; }
			if (block.depTimes.size() > maxTimeEntries) { maxTimeEntries = block.depTimes.size(); }
		}
		
		if (!isBalanced) {
			log.warn("Unbalanced IlinTripDistBlock:");
			for (int i=0; i<ilinTripDistBlocks.size(); i++) {
				String blockString = "i="+i+": ilin="+ilinTripDistBlocks.get(i).ilin+"; trip="+ilinTripDistBlocks.get(i).trip+"; dist="+ilinTripDistBlocks.get(i).dist;
				for (Integer depTime : ilinTripDistBlocks.get(i).depTimes) { blockString = blockString + "; "+depTime; }
				log.warn(blockString);
			}
		}
	}
	
	private final void handleIlinTripDistBlocks(TransitSchedule transitSchedule, List<IlinTripDistBlock> ilinTripDistBlocks) {
		int ilin = ilinTripDistBlocks.get(0).ilin;
		TransitScheduleFactory factory = transitSchedule.getFactory();
		TransitLine transitLine = transitSchedule.getTransitLines().get(Id.create(ilin, TransitLine.class));
		if (transitLine == null) { transitLine = factory.createTransitLine(Id.create(ilin, TransitLine.class)); transitSchedule.addTransitLine(transitLine); }

		List<Integer> dists = new ArrayList<Integer>();
		List<Integer> times = new ArrayList<Integer>();
		for (IlinTripDistBlock block : ilinTripDistBlocks) {
			dists.add(block.dist);
			times.add(block.depTimes.get(0));
		}
		
		Id<TransitRoute> transitRouteId = Id.create(ilin+".1", TransitRoute.class);
		TransitRoute transitRoute = transitLine.getRoutes().get(transitRouteId);
		if (transitRoute == null) {
			
			List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
			NetworkRoute networkRoute = new LinkNetworkRouteImpl(null,null);
			Id<Link> routeStartLinkId = null;
			List<Id<Link>> routeLinkIds = new ArrayList<Id<Link>>();
			Id<Link> routeEndLinkId = null;
			for (int i=0; i<dists.size(); i++) {
				int dist = dists.get(i);
				int time = times.get(i);

				Id<TransitStopFacility> facilityId = Id.create("stop-"+ilin+"-"+dist, TransitStopFacility.class);
				TransitStopFacility facility = transitSchedule.getFacilities().get(facilityId);
				if (facility == null) { log.warn("facility with id="+facilityId+" not found!"); }
				TransitRouteStop transitRouteStop = factory.createTransitRouteStop(facility,time-times.get(0),time-times.get(0));
				transitRouteStop.setAwaitDepartureTime(true);
				stops.add(transitRouteStop);
				
				if (i == 0) { routeStartLinkId = facility.getLinkId(); }
				else if (i == dists.size()-1) { routeEndLinkId = facility.getLinkId(); }
				else { routeLinkIds.add(facility.getLinkId()); }
				
			}
			networkRoute.setLinkIds(routeStartLinkId,routeLinkIds,routeEndLinkId);
			transitRoute = factory.createTransitRoute(transitRouteId,networkRoute,stops,TransportMode.pt);
			transitLine.addRoute(transitRoute);
			
			for (int i=0; i<ilinTripDistBlocks.get(0).depTimes.size(); i++) {
				Departure departure = factory.createDeparture(Id.create(transitRouteId.toString()+"."+i, Departure.class), ilinTripDistBlocks.get(0).depTimes.get(i));
				transitRoute.addDeparture(departure);
			}
		}
	}
	
	public void createNetworkFromPtStops(String ptStopsFile, Scenario scenario, ObjectAttributes nodeObjectAttributes) {
		log.info("creating ptNodes from "+ptStopsFile+" shape file...");
		Network network = scenario.getNetwork();
		int fCnt = 0;
		Set<Id<Node>> nodeIds = new HashSet<>();
		Id<Node> prevNodeId = null;
		int prevIlinId = -1;
		for (SimpleFeature f : ShapeFileReader.getAllFeatures(ptStopsFile)) {
			fCnt++;
			
			// ilin dist and nodeId
			int ilinId = Integer.parseInt(f.getAttribute(ILIN_NAME).toString().trim());
			int distId = Integer.parseInt(f.getAttribute(DIST_NAME).toString().trim());
			Id<Node> nodeId = Id.create(ilinId+"-"+distId, Node.class);
			if (!nodeIds.add(nodeId)) { throw new RuntimeException("fCnt "+fCnt+": nodeId="+nodeId+" already created before!"); }

			// node type and switch to sister route flag 
			String nodeFlag = f.getAttribute(NODEFLAG_NAME).toString().trim();
			String ptNodeType = null;
			boolean switchToSisterRoute = false; 
			if (nodeFlag.length() == 0) {
				ptNodeType = "waypoint";
			} else if (nodeFlag.length() == 1) {
				if (nodeFlag.startsWith("T")) { ptNodeType = "timePoint"; }
				else if (nodeFlag.startsWith("B")) { ptNodeType = "stopAndTimePoint"; }
				else if (nodeFlag.startsWith("S")) { ptNodeType = "stopPoint"; }
				else  { throw new RuntimeException("fCnt "+fCnt+": nodeFlag='"+nodeFlag+"' (length=1) is neither T, B nor S"); }
			} else if (nodeFlag.length() == 2) {
				if (nodeFlag.startsWith("T")) { ptNodeType = "timePoint"; }
				else if (nodeFlag.startsWith("B")) { ptNodeType = "stopAndTimePoint"; }
				else if (nodeFlag.startsWith("S")) { ptNodeType = "stopPoint"; }
				else  { throw new RuntimeException("fCnt "+fCnt+": nodeFlag='"+nodeFlag+"' (length=2) is neither T, B nor S"); }
				if (nodeFlag.substring(1).equals("S")) { switchToSisterRoute = true; }
				else { throw new RuntimeException("fCnt "+fCnt+": nodeFlag='"+nodeFlag+"' (length=2) is not S"); }
			} else {
				throw new RuntimeException("fCnt "+fCnt+": nodeFlag='"+nodeFlag+"' does not contain the correct amount of characters!");
			}

							
			// node type and switch to sister route flag 
			String description = f.getAttribute(DESC_NAME).toString().trim();


			// parent ilin id
			int parentIlinId = Integer.parseInt(f.getAttribute(PARENT_ILIN_NAME).toString().trim());

			// parent routeName -> carrier code | line number | alt number { } direction
			String routeName = f.getAttribute(ROUTE_NAME).toString().trim();
			if (routeName.length() != 7) { throw new RuntimeException("fCnt "+fCnt+": routeName="+routeName+" does not have 7 characters!"); }
			String carrierCode = routeName.substring(0,2);
			String lineNr = routeName.substring(2,5);
			String alt = routeName.substring(5,6).trim(); // either a number or empty string
			String direction = routeName.substring(6);
			if (!(direction.equals("R") || direction.equals("E") || direction.equals("W") || direction.equals("N") || direction.equals("S"))) {
				throw new RuntimeException("fCnt "+fCnt+": routeName="+routeName+" last character is not R, E, W, N nor S!");
			}
			
			// mode
			String ptModeType = f.getAttribute(MODE_NAME).toString().trim();
			if (!(ptModeType.equals("1CR") || ptModeType.equals("2LR") || ptModeType.equals("3EX") || ptModeType.equals("4RB") || ptModeType.equals("5LB") || ptModeType.equals("6TW"))) {
				throw new RuntimeException("fCnt "+fCnt+": ptType="+ptModeType+" is neither 1CR, 2LR, 3EX, 4RB, 5LB nor 6TW!");
			}
			
			// create only nodes where pt stops
			if (ptNodeType.startsWith("stop")) {
				// coordinate
				Coordinate c = new Coordinate((f.getBounds().getMinX() + f.getBounds().getMaxX())/2.0, (f.getBounds().getMinY() + f.getBounds().getMaxY())/2.0);
				
				// add node
				Node n = network.getFactory().createNode(nodeId,new CoordImpl(c.x,c.y));
				network.addNode(n);

				// add node attributes
				nodeObjectAttributes.putAttribute(nodeId.toString(),ILIN_NAME,ilinId);
				nodeObjectAttributes.putAttribute(nodeId.toString(),DIST_NAME,distId);
				nodeObjectAttributes.putAttribute(nodeId.toString(),"switchNode",switchToSisterRoute);
				nodeObjectAttributes.putAttribute(nodeId.toString(),"ptNodeType",ptNodeType);
				nodeObjectAttributes.putAttribute(nodeId.toString(),DESC_NAME,description);
				nodeObjectAttributes.putAttribute(nodeId.toString(),PARENT_ILIN_NAME,parentIlinId);
				nodeObjectAttributes.putAttribute(nodeId.toString(),"carrierCode",carrierCode);
				nodeObjectAttributes.putAttribute(nodeId.toString(),"lineNr",lineNr);
				nodeObjectAttributes.putAttribute(nodeId.toString(),"alt",alt);
				nodeObjectAttributes.putAttribute(nodeId.toString(),"direction",direction);
				nodeObjectAttributes.putAttribute(nodeId.toString(),MODE_NAME,ptModeType);

				if (prevIlinId == ilinId) { // still the same line
					// create link with Id := ilin-prevDist-dist
					Link link = network.getFactory().createLink(Id.create(prevNodeId.toString()+"-"+distId, Link.class), network.getNodes().get(prevNodeId), network.getNodes().get(nodeId));
					double length = 100.0*0.3048*(distId-(Integer)nodeObjectAttributes.getAttribute(prevNodeId.toString(),DIST_NAME)); // differences of distIds in 100 feet converted to meters
					link.setLength(length);
					link.setFreespeed(999999.0); // default
					link.setNumberOfLanes(1.0); // default
					link.setCapacity(999999.0); // default
					Set<String> allowedModes = new HashSet<String>(2); allowedModes.add(TransportMode.pt); allowedModes.add(ptModeType);
					link.setAllowedModes(allowedModes);
					network.addLink(link);
				}
				else { // a new line: insert a pseudo link for the stop facility
					Node pseudoNode = network.getFactory().createNode(Id.create("pn-"+nodeId.toString(), Node.class),new CoordImpl(c.x,c.y));
					network.addNode(pseudoNode);
					nodeObjectAttributes.putAttribute(nodeId.toString(),ILIN_NAME,ilinId);
					nodeObjectAttributes.putAttribute(nodeId.toString(),DIST_NAME,-1); // -100 feet
					nodeObjectAttributes.putAttribute(nodeId.toString(),"switchNode",switchToSisterRoute);
					nodeObjectAttributes.putAttribute(nodeId.toString(),"ptNodeType","waypoint");
					nodeObjectAttributes.putAttribute(nodeId.toString(),DESC_NAME,description);
					nodeObjectAttributes.putAttribute(nodeId.toString(),PARENT_ILIN_NAME,parentIlinId);
					nodeObjectAttributes.putAttribute(nodeId.toString(),"carrierCode",carrierCode);
					nodeObjectAttributes.putAttribute(nodeId.toString(),"lineNr",lineNr);
					nodeObjectAttributes.putAttribute(nodeId.toString(),"alt",alt);
					nodeObjectAttributes.putAttribute(nodeId.toString(),"direction",direction);
					nodeObjectAttributes.putAttribute(nodeId.toString(),MODE_NAME,ptModeType);
					Link link = network.getFactory().createLink(Id.create(pseudoNode.toString()+"-"+distId, Link.class), pseudoNode, network.getNodes().get(nodeId));
					double length = 100.0*0.3048*1.0; // 100 feet converted to meters
					link.setLength(length);
					link.setFreespeed(100.0); // fast speed for speudo link
					link.setNumberOfLanes(1.0); // default
					link.setCapacity(10000.0); // large cap for pseudo link
					Set<String> allowedModes = new HashSet<String>(2); allowedModes.add(TransportMode.pt); allowedModes.add(ptModeType);
					link.setAllowedModes(allowedModes);
					network.addLink(link);
				}
				prevIlinId = ilinId;
				prevNodeId = nodeId;
			}
		}
		log.info("done. (creating ptNodes)");

	}
	
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		ObjectAttributes nodeObjectAttributes = new ObjectAttributes();
		UCSBptNetworkParser parser = new UCSBptNetworkParser();
		parser.createNetworkFromPtStops(
				"D:/balmermi/documents/eclipse/input/raw/america/usa/losAngeles/UCSB/0000/transit/preparation/stops/stops_UTM_Zone_11N.shp",
				scenario,nodeObjectAttributes);
		parser.parse(
				"D:/balmermi/documents/eclipse/input/raw/america/usa/losAngeles/UCSB/0000/transit/preparation/times/times_tuesday_sorted.txt",
				nodeObjectAttributes, scenario);
		String base = "../../../output/";
		new NetworkWriter(scenario.getNetwork()).write(base+"network.xml.gz");
		new NetworkWriteAsTable(base).run(scenario.getNetwork());
		new ObjectAttributesXmlWriter(nodeObjectAttributes).writeFile(base+"nodeObjectAttributes.xml.gz");
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(base+"transitSchedule.xml.gz");
	}
}
