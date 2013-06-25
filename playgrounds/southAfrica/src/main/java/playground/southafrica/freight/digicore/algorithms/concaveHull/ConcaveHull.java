/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.southafrica.freight.digicore.algorithms.concaveHull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdge;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdgeSubdivision;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdgeTriangle;
import com.vividsolutions.jts.triangulate.quadedge.Vertex;
import com.vividsolutions.jts.util.UniqueCoordinateArrayFilter;

/**
 * Class to generate the concave hull from a set of given points.
 * 
 * @author jwjoubert
 */
public class ConcaveHull {
	private final static Logger LOG = Logger.getLogger(ConcaveHull.class);
	private GeometryFactory geomFactory;
	private GeometryCollection filteredPoints;
	private double threshold;
	
	private HashMap<LineSegment, Integer> segments = new HashMap<LineSegment, Integer>();
	private HashMap<Integer, Edge> edges = new HashMap<Integer, Edge>();
	private HashMap<Integer, Triangle> triangles = new HashMap<Integer, Triangle>();

	private TreeMap<Integer, Edge> consideredEdges = new TreeMap<Integer, Edge>();
	private HashMap<Integer, Edge> ignoredEdges = new HashMap<Integer, Edge>();
	
	private Map<Coordinate,Integer> coordinates = new HashMap<Coordinate, Integer>();
	private Map<Integer, Node> vertices = new HashMap<Integer, Node>();


	
	public ConcaveHull(GeometryCollection points, double threshold) {
		this.geomFactory = points.getFactory();

		/* Ensure a unique set of input points. */
		UniqueCoordinateArrayFilter filter = new UniqueCoordinateArrayFilter();
		points.apply(filter);
		Coordinate[] ca = filter.getCoordinates();
		
		/* Convert the filtered points into GeometryCollection. */
		Geometry[] ga = new Geometry[ca.length];
		for(int i = 0; i < ca.length; i++){
			ga[i] = geomFactory.createPoint(ca[i]);
		}
		this.filteredPoints = new GeometryCollection(ga, geomFactory);
		
		this.threshold = threshold;
	}
	
	
	/**
	 * Returns a {@link Geometry} that represents the concave hull of the input
	 * geometry according to the threshold.
	 * The returned geometry contains the minimal number of points needed to
	 * represent the concave hull.
	 *
	 * @return if the concave hull contains 3 or more points, a {@link Polygon};
	 * 2 points, a {@link LineString};
	 * 1 point, a {@link Point};
	 * 0 points, an empty {@link GeometryCollection}.
	 */
	public Geometry getConcaveHull() {

		if (this.filteredPoints.getNumGeometries() == 0) {
			return this.geomFactory.createGeometryCollection(null);
		}
		if (this.filteredPoints.getNumGeometries() == 1) {
			return this.filteredPoints.getGeometryN(0);
		}
		if (this.filteredPoints.getNumGeometries() == 2) {
			return this.geomFactory.createLineString(this.filteredPoints.getCoordinates());
		}

		return concaveHull();
	}
	
	
	private Geometry concaveHull(){
		/* Construct the Delaunay Triangulation. */
		DelaunayTriangulationBuilder dtb = new DelaunayTriangulationBuilder();
		dtb.setSites(this.filteredPoints);
		
		QuadEdgeSubdivision qes = dtb.getSubdivision();
		
		/*TODO Sort out the following warnings...*/
		Collection<QuadEdge> quadEdges = qes.getEdges();
		
		List<QuadEdgeTriangle> qeTriangles = QuadEdgeTriangle.createOn(qes);
		Collection<Vertex> qeVertices = qes.getVertices(false);
		
		/* Create index maps for the nodes/vertices. */
		int nodeId = 0;
		for (Vertex v : qeVertices) {
			this.coordinates.put(v.getCoordinate(), nodeId);
			this.vertices.put(nodeId, new Node(nodeId, v.getCoordinate()));
			nodeId++;
		}

		/* Identify the list of boundary edges. To do that:
		 * - Find all edges that are `frame-border' QuadEdges;
		 * - Find all edges that are `frame' QuadEdges;
		 * - Those QuadEdges that are frame-border, but NOT frame; must
		 *   therefore be `border' QuadEdges.
		 *   TODO find out what frame and border edges really are?!
		 */
		List<QuadEdge> qeFrameBorder = new ArrayList<QuadEdge>();
		List<QuadEdge> qeFrame = new ArrayList<QuadEdge>();
		List<QuadEdge> qeBorder = new ArrayList<QuadEdge>();

		for (QuadEdge qe : quadEdges) {
			if (qes.isFrameBorderEdge(qe)) {
				qeFrameBorder.add(qe);
			} 
			if (qes.isFrameEdge(qe)) {
				qeFrame.add(qe);
			}
		}
		
		/* Now identify the border edges. */ 
		for (QuadEdge q : qeFrameBorder){
			if (! qeFrame.contains(q)) {
				qeBorder.add(q);
			}
		}
		
		/* Remove all the QuadEdges that were artificially added as a result
		 * of the Delaunay Triangulation process. */
		for (QuadEdge q : qeFrame) {
			qes.delete(q);
			//FIXME We add the removal from quadEdges AND QuadEdgeSubdivision
//			quadEdges.remove(q);
		}
		
		/* Sort the boundary list on edge length in descending order. */
		HashMap<QuadEdge, Double> qeLengths = new HashMap<QuadEdge, Double>();
		for (QuadEdge qe : quadEdges) {
			qeLengths.put(qe, qe.toLineSegment().getLength());
		}
				
		DoubleComparator dc = new DoubleComparator(qeLengths);
		TreeMap<QuadEdge, Double> qeSorted = new TreeMap<QuadEdge, Double>(dc);
		qeSorted.putAll(qeLengths);

		/* Initialise the vertex boundary function. Set all vertices's 
		 * boundary attribute to `false'. 
		 * 
		 * This was done by default when `Node's were created. */
		
		/* Create the dual-directional map for all edges. For each boundary edge, 
		 * set the associated nodes' boundary attribute to `true'. */
		int edgeId = 0;
		for(QuadEdge qe : qeSorted.keySet()){
			LineSegment ls = qe.toLineSegment();
			ls.normalize();
			
			/* Get the nodes associated with the coordinates. */
			Integer idOrigin = this.coordinates.get(ls.p0);
			Integer idDestination = this.coordinates.get(ls.p1);
			Node nodeOrigin = this.vertices.get(idOrigin);
			Node nodeDestination = this.vertices.get(idDestination);
		
			Edge edge;
			if (qeBorder.contains(qe)) {
				nodeOrigin.setBorder(true);
				nodeDestination.setBorder(true);
				edge = new Edge(edgeId, ls, nodeOrigin, nodeDestination, true);
				if (ls.getLength() < this.threshold) {
					this.ignoredEdges.put(edgeId, edge);
				} else {
					this.consideredEdges.put(edgeId, edge);
				}
			} else {
				edge = new Edge(edgeId, ls, nodeOrigin, nodeDestination, false);
			}
			this.edges.put(edgeId, edge);
			this.segments.put(ls, edgeId);
			edgeId++;
		}

		/* Link the edges to their respective triangles. */
		int triangleId = 0;
		for (QuadEdgeTriangle qet : qeTriangles) {
			LineSegment lsA = qet.getEdge(0).toLineSegment();
			LineSegment lsB = qet.getEdge(1).toLineSegment();
			LineSegment lsC = qet.getEdge(2).toLineSegment();
			lsA.normalize();
			lsB.normalize();
			lsC.normalize();
			
			Edge edgeA = this.edges.get(this.segments.get(lsA));
			Edge edgeB = this.edges.get(this.segments.get(lsB));
			Edge edgeC = this.edges.get(this.segments.get(lsC));

			Triangle triangle = new Triangle(triangleId, qet.isBorder() ? true : false);
			triangle.addEdge(edgeA);
			triangle.addEdge(edgeB);
			triangle.addEdge(edgeC);

			edgeA.addTriangle(triangle);
			edgeB.addTriangle(triangle);
			edgeC.addTriangle(triangle);

			this.triangles.put(triangleId, triangle);
			triangleId++;
		}

		/* For each edge, check if it belongs to more than one triangle. If so,
		 * link the triangles as neighbours. */
		for (Edge edge : this.edges.values()) {
			if (edge.getTriangles().size() != 1) {
				Triangle tA = edge.getTriangles().get(0);
				Triangle tB = edge.getTriangles().get(1);
				tA.addNeighbour(tB);
				tB.addNeighbour(tA);
			}
		}
		
		/* Iteratively remove boundary edges, add the new resulting boundary 
		 * edges, and sort on edge length until no more boundary edges can be
		 * removed. */
		int index = 0;
		while(index != -1){
			index = -1;
			
			/* Identify the longest edge. */
			Edge e = null;
			if(!this.consideredEdges.isEmpty()){
				e = this.consideredEdges.firstEntry().getValue();
				index = e.getId();
			}
			
			if(e != null){
				Triangle triangle = e.getTriangles().get(0);
				List<Triangle> neighbours = triangle.getNeighbours();
				
				/* First test for irregular triangle. If a border edge belongs
				 * to a triangle that only has ONE neighbour, removing that
				 * border edge will result in an irregular triangle. 
				 * TODO Check if this test is really necessary... it seems that
				 * the second test will cover both possibilities for irregular
				 * triangles. */
				if(neighbours.size() == 1){
					/* It is irregular. Even though it's long enough to be
					 * considered for removal, it will be flagged and ignored */
					this.consideredEdges.remove(e.getId());
					this.ignoredEdges.put(e.getId(), e);
				} else{
					Edge e0 = triangle.getEdges().get(0);
					Edge e1 = triangle.getEdges().get(1);
					
					/* Second test for irregular triangle. If a triangle has
					 * ALL THREE nodes on the boundary, none of its edges can
					 * be removed while remaining regular. */
					if(e0.getOriginNode().isBorder() &&
					   e0.getDestinationNode().isBorder() &&
					   e1.getOriginNode().isBorder() &&
					   e1.getDestinationNode().isBorder()){
						
						/* It is irregular. Even though it's long enough to be
						 * considered for removal, it will be flagged and ignored */
						this.consideredEdges.remove(e.getId());
						this.ignoredEdges.put(e.getId(), e);
					} else{
						/* It seems it is fine to remove this edge. */
						
						/* Border triangles seems to always have only two
						 * neighbours. */
						Triangle tA = neighbours.get(0);
						Triangle tB = neighbours.get(1);
						tA.setBorder(true); // FIXME not necessarily useful. Consider removing all triangle-related border attributes.
						tB.setBorder(true); // FIXME not necessarily useful. See above
						this.triangles.remove(triangle.getId());
						tA.removeNeighbour(triangle);
						tB.removeNeighbour(triangle);
						
						/* Get the other two edges associated with the triangle, 
						 * as they will become the new border edges. */
						List<Edge> triangleEdges = triangle.getEdges();
						triangleEdges.remove(e);
						
						/* Remove the current edge. */
						this.edges.remove(e.getId());
						this.consideredEdges.remove(e.getId());
						
						/* Add the two new border edges, considering their 
						 * length as well. */
						Edge eA = triangleEdges.get(0);
						eA.setBorder(true);
						if(eA.getGeometry().getLength() > this.threshold){
							this.consideredEdges.put(eA.getId(), eA);
						} else{
							this.ignoredEdges.put(eA.getId(), eA);
						}
						eA.removeTriangle(triangle);

						Edge eB = triangleEdges.get(1);
						eB.setBorder(true);
						if(eB.getGeometry().getLength() > this.threshold){
							this.consideredEdges.put(eB.getId(), eB);
						} else{
							this.ignoredEdges.put(eB.getId(), eB);
						}
						eB.removeTriangle(triangle);
					}
				}
			}
		}
		
		/* Assemble the final concave Hull. Start by adding all the border
		 * edges. */
		List<LineString> borderEdges = new ArrayList<LineString>();
		for(Edge e : this.consideredEdges.values()){
			borderEdges.add( e.getGeometry().toGeometry(this.geomFactory) );
		}
		for(Edge e : this.ignoredEdges.values()){
			borderEdges.add( e.getGeometry().toGeometry(this.geomFactory) );
		}
		
		/* Merge the line strings. */
		LineMerger lineMerger = new LineMerger();
		lineMerger.add(borderEdges);
		LineString merge = (LineString) lineMerger.getMergedLineStrings().iterator().next();
		
		if (merge.isRing()) {
			LinearRing lr = new LinearRing(merge.getCoordinateSequence(), this.geomFactory);
			Polygon concaveHull = new Polygon(lr, null, this.geomFactory);
			return concaveHull;
		} else{
			LOG.warn("Could not create hull as the line segments do not form a closed ring.");
			return null;
		}
	}

	
	/**
	 * Just returns the number of (filtered) input points. This method is most
	 * likely only going to be used for test purposes.
	 * @return
	 */
	public Object getInputPoints() {
		return this.filteredPoints.getNumGeometries();
	}
	
	
	/** Reads a flat file containing only the coordinates of points. The first
	 * column is assumed to be the longitude, and the second the latitude. 
	 * 
	 * NOTE: It is assumed that the coordinate reference system is projected, as 
	 * distance calculations are done.
	 * 
	 * @param fileName
	 * @return
	 */
	public static List<Coordinate> getClusterCoords(String fileName) {  
		LOG.info("Reading coordinate list from " + fileName);
		List<Coordinate> coordinateList = new ArrayList<Coordinate>();
		
		try {
			BufferedReader br = IOUtils.getBufferedReader(fileName);
			br.readLine();
						
			String lines;
			while ((lines = br.readLine()) != null) {
				String[] inputString = lines.split(",");
				double x = Double.parseDouble(inputString[0]);
				double y = Double.parseDouble(inputString[1]);
				Coordinate coord = new Coordinate(x, y);
				coordinateList.add(coord); 
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return coordinateList;
	}

	
	public static void toCsvFile(String outputFolder, Geometry concaveHull, double thresholdParameter) {  
		
		Coordinate[] boundaryArray = concaveHull.getCoordinates();
		List<Tuple<Coordinate, Coordinate>> boundaryList = 
				new ArrayList<Tuple<Coordinate, Coordinate>>();
		for (int i = 0; i < boundaryArray.length-1; i++) {
			Coordinate coordinateA = boundaryArray[i];
			Coordinate coordinateB = boundaryArray[i+1];
			Tuple<Coordinate, Coordinate> edgeTuple = 
					new Tuple<Coordinate, Coordinate>(coordinateA, coordinateB);
			boundaryList.add(edgeTuple);
		} 
		
		try {
			BufferedWriter output = new BufferedWriter(
					new FileWriter(new File(
							String.format("%sThreshold_%.0f.csv", outputFolder, thresholdParameter))));
			try {
				output.write("firstX, firstY, secondX, secondY");
				output.newLine();
				
					for (Tuple<Coordinate, Coordinate> edge : boundaryList) {
						String firstX = Double.toString(edge.getFirst().x);
						String firstY = Double.toString(edge.getFirst().y);
						String secondX = Double.toString(edge.getSecond().x);
						String secondY = Double.toString(edge.getSecond().y);
						output.write(firstX);
						output.write(",");
						output.write(firstY);
						output.write(",");
						output.write(secondX);
						output.write(",");
						output.write(secondY);
						output.newLine();
					}
			} finally {
				output.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) {
		Header.printHeader(ConcaveHull.class.toString(), args);
		
		List<Coordinate> coordinates = ConcaveHull.getClusterCoords(args[0]);
		
		GeometryFactory gf = new GeometryFactory();
		Geometry[] points = new Geometry[coordinates.size()];
		for(int i = 0; i < coordinates.size(); i++){
			points[i] = gf.createPoint(coordinates.get(i));
		}
		GeometryCollection gc = new GeometryCollection(points, gf);
		
		ConcaveHull ch = new ConcaveHull(gc, Double.parseDouble(args[2]));
		Geometry g = ch.getConcaveHull();

		ConcaveHull.toCsvFile(args[1], g, Double.parseDouble(args[2]));
		
		Header.printFooter();
	}
	
}
