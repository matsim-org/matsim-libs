/* *********************************************************************** *
 * project: org.matsim.*
 * SomeBits.java
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

/**
 * 
 */
package playground.southafrica.projects.mapMatching;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.CalcBoundingBox;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.Vehicle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import playground.southafrica.projects.digicore.DigicoreUtils;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Some bits and pieces of code to get the map matching algorithm off the 
 * ground.
 * 
 * @author jwjoubert
 */
public class SomeBits {
	final private static Logger LOG = Logger.getLogger(SomeBits.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(SomeBits.class.toString(), args);
		
//		Map<String, Integer> map = new TreeMap<String, Integer>();
//		File file = new File(args[0]);
//		String id = file.getName().substring(0, file.getName().indexOf("."));
//		if(!map.containsKey(id)){
//			map.put(id, map.size()+1);
//		}
//		int newId = map.get(id);
//		
//		splitTrips(file, String.valueOf(newId));
		
//		/* Read in the network. */ 
//		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new MatsimNetworkReader(sc.getNetwork()).parse(args[0]);
//		
//		QuadTree<Link> qt = buildQuadTreeFromNetwork(sc.getNetwork());
//		findRouteBetweenRandomPoints(sc.getNetwork());
		
		/* Coordinates. */
		Coord c1 = CoordUtils.createCoord(1.0, 1.0);
		Coord c2 = CoordUtils.createCoord(11.0, 1.0);
		/* MATSim version. */
		Coord a = CoordUtils.createCoord(0.0, 0.0);
		Coord b = CoordUtils.createCoord(10.0, 0.0);
		Network net = NetworkUtils.createNetwork();
		NetworkFactory nf = net.getFactory();
		Node na = nf.createNode(Id.createNodeId("a"), a);
		Node nb = nf.createNode(Id.createNodeId("b"), b);
		net.addNode(na);
		net.addNode(nb);
		Link link = nf.createLink(Id.createLinkId("ab"), na, nb);
		net.addLink(link);
		checkCoords(c1, link);
		checkCoords(c2, link);
		
		Header.printFooter();
	}
	
	public static Coord checkCoords(Coord c, Link link){
		
		/* Vividsolutions version. */
		GeometryFactory gf = new GeometryFactory();
		Coordinate ca = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
		Coordinate cb = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
		
		LineSegment seg = new LineSegment(ca, cb);
		
		Coordinate cu = new Coordinate(c.getX(), c.getY());
		Coordinate cp = seg.project(cu);
		
//		LOG.info(String.format("Distance: %.2f (%.1f,%.1f)", seg.distancePerpendicular(cu), cp.x, cp.y));
		
		Coord rc = CoordUtils.createCoord(cp.x, cp.y);
		return rc;
	}
	

	public static void splitTrips(File file, String newId){
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "EPSG:3857");
		String id = file.getName().substring(0, file.getName().indexOf("."));
		File folder = file.getParentFile();
		String folderName = folder.getAbsolutePath() + (folder.getAbsolutePath().endsWith("/") ? "" : "/") + newId + "/";
		File newFolder = new File(folderName);
		if(newFolder.exists() && newFolder.isDirectory()){
			LOG.warn("The output folder exists and will be deleted: " + folderName);
			FileUtils.delete(newFolder);
		}
		newFolder.mkdirs();
		
		LOG.info("Splitting trips for " + id );
		LOG.info("  Writing trips to " + folderName);
		BufferedReader br = IOUtils.getBufferedReader(file.getAbsolutePath());
		int trips = 0;
		BufferedWriter bw = IOUtils.getAppendingBufferedWriter(folderName + newId + "_" + String.format("%03d.csv", trips));
		try{
			String line = br.readLine();
			String[] sa = line.split(",");
			long previousTime = Long.parseLong(sa[2]);
			
			while((line = br.readLine()) != null){
				sa = line.split(",");
				long time = Long.parseLong(sa[2]);
				double diff = ((double)(time - previousTime)) / 1000.0;
				double lon = Double.parseDouble(sa[3]);
				double lat = Double.parseDouble(sa[4]);
				Coord c = ct.transform(CoordUtils.createCoord(lon, lat));
				
				/* Get a more usable date format. */
				Calendar cal = DigicoreUtils.getCalendarSince1996(time);
				double MatsimTime = Time.parseTime(DigicoreUtils.getTimeOfDayFromCalendar(cal));
				String cleanLine = String.format("%.2f,%.2f,%.3f\n", c.getX(), c.getY(), MatsimTime);
				if(diff <= Time.parseTime("00:01:00")){
					bw.write(cleanLine);
				} else{
					bw.close();
					trips++;
					bw = IOUtils.getAppendingBufferedWriter(folderName + newId + "_" + String.format("%03d.csv", trips));
					bw.write(cleanLine);
//					LOG.info("  diff: " + ((double)diff)/1000.0 + "sec");
				}
				previousTime = time;
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + file.getAbsolutePath());
		} finally{
			try {
				br.close();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + file.getAbsolutePath());
			}
		}
		
		
		LOG.info("Done splitting");
		LOG.info("Trips recorded: " + trips);
	}

	
	
	/**
	 * Build a {@link QuadTree} from a network. Each link is added, using
	 * the link's centroid, i.e. midpoint as location in the {@link QuadTree}.
	 * @param network
	 * @return
	 */
	public static QuadTree<Link> buildQuadTreeFromNetwork(Network network){
		LOG.info("Building QuadTree of network links...");
		
		/* Find the extent of the QuadTree. */
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for(Link l : network.getLinks().values()){
			Coord c = l.getCoord();
			minX = Math.min(minX, c.getX());
			minY = Math.min(minY, c.getY());
			maxX = Math.max(maxX, c.getX());
			maxY = Math.max(maxY, c.getY());
		}
		QuadTree<Link> linkQT = new QuadTree<Link>(minX, minY, maxX, maxY);
		
		/* Add each link to the QuadTree. */
		for(Link l : network.getLinks().values()){
			Coord c = l.getCoord();
			linkQT.put(c.getX(), c.getY(), l);
		}
		LOG.info("Done building QuadTree.");
		LOG.info(" ->  Number of links in network: " + network.getLinks().size());
		LOG.info(" -> Number of links in QuadTree: " + linkQT.size());
		return linkQT;
	}
	
	
	public static void findRouteBetweenRandomPoints(Network network){
		LOG.info("Find a route between two arbitrary points...");
		CalcBoundingBox cbb = new CalcBoundingBox();
		cbb.run(network);
		double x1 = cbb.getMinX() + Math.random()*(cbb.getMaxX() - cbb.getMinX());
		double y1 = cbb.getMinX() + Math.random()*(cbb.getMaxY() - cbb.getMinY());
		Coord c1 = CoordUtils.createCoord(x1, y1);
		Node n1 = NetworkUtils.getNearestLink(network, c1).getFromNode();
		
		double x2 = cbb.getMinX() + Math.random()*(cbb.getMaxX() - cbb.getMinX());
		double y2 = cbb.getMinX() + Math.random()*(cbb.getMaxY() - cbb.getMinY());
		Coord c2 = CoordUtils.createCoord(x2, y2);
		Node n2 = NetworkUtils.getNearestLink(network, c2).getToNode();

		/* Route between two nodes. */
		DijkstraFactory fdf = new DijkstraFactory();
		TravelDisutility travelCosts = new TravelDisutility() {
			
			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				return link.getLength();
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return getLinkTravelDisutility(link, Time.UNDEFINED_TIME, null, null);
			}
		};
		TravelTime travelTimes = new FreeSpeedTravelTime();
		LeastCostPathCalculator pc = fdf.createPathCalculator(network, travelCosts, travelTimes );
		
		Path path = pc.calcLeastCostPath(n1, n2, Time.UNDEFINED_TIME, null, null);
		
		List<Link> links = path.links;
		LOG.info("Number of links in path: " + links.size());
		LOG.info(String.format("  From (%.0f,%.0f)", n1.getCoord().getX(), n1.getCoord().getY()));
		for(Link l : links){
			LOG.info("     - " + l.getId().toString());
		}
		LOG.info(String.format("  From (%.0f,%.0f)", n2.getCoord().getX(), n2.getCoord().getY()));
		
		LOG.info("Done finding route.");
		
		
		/* Build conceptual graph from candidate links. */
		Link candidateLink1 = network.getLinks().get(Id.createLinkId("35"));
		Link candidateLink2 = network.getLinks().get(Id.createLinkId("4"));
		
		Network	graph = NetworkUtils.createNetwork();
		NetworkFactory nf = graph.getFactory();
		Id<Node> node1Id = Id.createNodeId(candidateLink1.getId().toString());
		Node nodeForLink1 = null;
		if(!graph.getNodes().containsKey(node1Id)){
			nodeForLink1 = nf.createNode(node1Id, candidateLink1.getCoord());
			graph.addNode(nodeForLink1);
		} else{
			nodeForLink1 = graph.getNodes().get(node1Id);
		}
		
		Id<Node> node2Id = Id.createNodeId(candidateLink2.getId().toString());
		Node nodeForLink2 = null;
		if(!graph.getNodes().containsKey(node2Id)){
			nodeForLink2 = nf.createNode(node2Id, candidateLink2.getCoord());
			graph.addNode(nodeForLink2);
		} else{
			nodeForLink2 = graph.getNodes().get(node2Id);
		}
		
		Id<Link> linkId = Id.createLinkId(nodeForLink1.getId().toString() + "_" + nodeForLink2.getId().toString());
		Link graphLink = nf.createLink(linkId, nodeForLink1, nodeForLink2);
		graphLink.setLength(calculateWeight());
		graph.addLink(graphLink);
		
		/* There is this thing called 'custom attributes'. */
		ObjectAttributes linkAttributes = new ObjectAttributes();
		int someValue = 1;
		linkAttributes.putAttribute(graphLink.getId().toString(), "someAttribute", someValue);
		String attributeFile = "";
		new ObjectAttributesXmlWriter(linkAttributes).writeFile(attributeFile);
		
		/* Writing a network. */
		String filename ="";
		new NetworkWriter(graph).write(filename);
		
	}
	
	private static double calculateWeight(){
		double d = 0.0;
		return d;
	}
	
	
}
