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

package org.matsim.core.network.algorithms.intersectionSimplifier.containers;

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
import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class to generate the concave hull from a set of given points. The algorithm 
 * is based on the paper by <br><br> 
 * &nbsp;&nbsp;&nbsp;&nbsp&nbsp;&nbsp;&nbsp;&nbsp Duckham et al. (2008). 
 * Efficient generation of simple polygons for characterizing the shape of a set 
 * of points in the plane. <i>Pattern Recognition</i>, <b>41</b>, 3224-3236. <br><br>
 * 
 * This code also benefits from insight gained from the code from Eric Grosso,
 * but we've adapted (and some cases simplified and improved) for specific 
 * application, originally, to the South African application using <i>Digicore</i> 
 * commercial vehicle activity chain data.
 * 
 * @author jwjoubert
 */
public class ConcaveHull {
	private final static Logger LOG = Logger.getLogger(ConcaveHull.class);
	private GeometryFactory geomFactory;
	private GeometryCollection filteredPoints;
	private double threshold;
	private boolean printIterations = false;
	
	private HashMap<LineSegment, Integer> segments = new HashMap<>();
	private HashMap<Integer, HullEdge> edges = new HashMap<>();
	private HashMap<Integer, HullTriangle> triangles = new HashMap<>();

	private TreeMap<Integer, HullEdge> consideredEdges = new TreeMap<>();
	private HashMap<Integer, HullEdge> ignoredEdges = new HashMap<>();
	
	private Map<Coordinate,Integer> coordinates = new HashMap<>();
	private Map<Integer, HullNode> vertices = new HashMap<>();


	/**
	 * Constructor for the concave hull algorithm. Be default, not output will
	 * be written for each iteration of the algorithm's execution. If you
	 * require control of the output, rather use the alternative constructor
	 * {@link #ConcaveHull(GeometryCollection, double, boolean)}
	 * @param points the {@link GeometryCollection} of input points for which 
	 * 		  the concave hull will be computed. In the South African 
	 * 		  application the will typically be {@link Point}s, and not 
	 * 		  {@link Coordinate}s as the latter is <i>not</i> a {@link Geometry}.
	 * @param threshold the edge length threshold used by the concave hull
	 *        algorithm. Only border edges with a length <i>longer</i> than the
	 *        threshold will be considered for removal.
	 */
	public ConcaveHull(GeometryCollection points, double threshold) {
		this(points, threshold, false);
	}
	
	/**
	 * Constructor for the concave hull algorithm. 
	 * @param points the {@link GeometryCollection} of input points for which 
	 * 		  the concave hull will be computed. In the South African 
	 * 		  application the will typically be {@link Point}s, and not 
	 * 		  {@link Coordinate}s as the latter is <i>not</i> a {@link Geometry}.
	 * @param threshold the edge length threshold used by the concave hull
	 *        algorithm. Only border edges with a length <i>longer</i> than the
	 *        threshold will be considered for removal.
	 * @param printIterations boolean choice indicating if output (triangles and
	 *        borders) will be written to the output folder, or not. This only
	 *        needs to be true when small-scale single instances are run, 
	 *        typically for illustration purposes.
	 */
	public ConcaveHull(GeometryCollection points, double threshold, boolean printIterations){
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
		this.printIterations = printIterations;
		
		/* Print the header for the iterations' output. */
		if(this.printIterations){
			/* File printing triangles. */
			String filename = String.format("output/concaveHull/Threshold_%.0f_triangles.csv", this.threshold);
			BufferedWriter bw = IOUtils.getBufferedWriter(filename);
			try{
				bw.write("iteration,firstX,firstY,secondX,secondY");
				bw.newLine();
			} catch (IOException e) {
				throw new RuntimeException("Could not write to BufferedWriter " + filename);
			} finally{
				try {
					bw.close();
				} catch (IOException e) {
					throw new RuntimeException("Could not close BufferedWriter " + filename);
				}
			}

			/* File printing border. */
			filename = String.format("output/concaveHull/Threshold_%.0f_border.csv", this.threshold);
			bw = IOUtils.getBufferedWriter(filename);
			try{
				bw.write("iteration,firstX,firstY,secondX,secondY");
				bw.newLine();
			} catch (IOException e) {
				throw new RuntimeException("Could not write to BufferedWriter " + filename);
			} finally{
				try {
					bw.close();
				} catch (IOException e) {
					throw new RuntimeException("Could not close BufferedWriter " + filename);
				}
			}
		}
	}
	
	
	
	/**
	 * Returns a {@link Geometry} that represents the concave hull of the input
	 * geometry according to the threshold.
	 * The returned geometry contains the minimal number of points needed to
	 * represent the concave hull. No facility identifier is required. If you
	 * require to identify a facility, for example for debugging purposes, 
	 * rather use the method {@link #getConcaveHull(String)}.
	 *
	 * @return if the concave hull contains 3 or more points, a {@link Polygon};
	 * 2 points, a {@link LineString};
	 * 1 point, a {@link Point};
	 * 0 points, an empty {@link GeometryCollection}.
	 */
	public Geometry getConcaveHull() {
		return getConcaveHull("");
	}
	
	
	/**
	 * Returns a {@link Geometry} that represents the concave hull of the input
	 * geometry according to the threshold.
	 * The returned geometry contains the minimal number of points needed to
	 * represent the concave hull.
	 * 
	 * @param facilityIdentifier a {@link String} that uniquely identifies the
	 *        group of points for which the hull is created. For example, this 
	 *        can be the facility Id, and is useful for debugging, especially 
	 *        when a Delaunay triangulation cannot be determined for a facility. 
	 *
	 * @return if the concave hull contains 3 or more points, a {@link Polygon};
	 * 2 points, a {@link LineString};
	 * 1 point, a {@link Point};
	 * 0 points, an empty {@link GeometryCollection}.
	 */
	public Geometry getConcaveHull(String facilityIdentifier) {
		
		/* Empty geometry. */
		if (this.filteredPoints.getNumGeometries() == 0) {
			return this.geomFactory.createGeometryCollection(null);
		}
		
		/* Point. */
		if (this.filteredPoints.getNumGeometries() == 1) {
			return this.filteredPoints.getGeometryN(0);
		}
		
		/* Line. */
		if (this.filteredPoints.getNumGeometries() == 2) {
			return this.geomFactory.createLineString(this.filteredPoints.getCoordinates());
		}

		/* Polygon: the concave hull. */
		return concaveHull(facilityIdentifier);
	}
	
	
	
	
	private Geometry concaveHull(String facilityIdentifier){
		/* Construct the Delaunay Triangulation. */
		DelaunayTriangulationBuilder dtb = new DelaunayTriangulationBuilder();
		dtb.setSites(this.filteredPoints);
		
		QuadEdgeSubdivision qes = dtb.getSubdivision();
		
		/*TODO Sort out the following warnings...*/
		Collection<QuadEdge> quadEdges = qes.getEdges();
		
		List<QuadEdgeTriangle> qeTriangles = QuadEdgeTriangle.createOn(qes);
		Collection<Vertex> qeVertices = qes.getVertices(false);

		/*TODO Remove after debugging. It seems that some facilities, although
		 * they have three or more points, do not have a 'valid' Delaunay
		 * triangulation. */
		if(qeTriangles.size() == 0 || qeVertices.size() == 0){
			LOG.warn("No triangulation for " + this.filteredPoints.getNumPoints() + " points!!");
			LOG.warn("   --> Unique id for the group of points: " + facilityIdentifier);
			Coordinate[] ca = this.filteredPoints.getCoordinates();
			if(ca.length == 3){
				LOG.warn("   --> Instead, a polygon (triangle) of the three points will be returned.");
				Coordinate[] caClosed = new Coordinate[4];
				caClosed[0] = ca[0];
				caClosed[1] = ca[1];
				caClosed[2] = ca[2];
				caClosed[3] = ca[0];				
				return this.geomFactory.createPolygon(this.geomFactory.createLinearRing(caClosed), null);
			} else{
				LOG.warn("   --> Returning a single point geometry as the weighted coordinates of the filtered points.");
				double xSum = 0;
				double ySum = 0;
				for(Coordinate c :this.filteredPoints.getCoordinates()){
					xSum += c.x;
					ySum += c.y;
				}
				double newX = xSum / ((double)this.filteredPoints.getCoordinates().length);
				double newY = ySum / ((double)this.filteredPoints.getCoordinates().length);

				return this.geomFactory.createPoint(new Coordinate(newX, newY));
			}
		}
		
		/* Create index maps for the nodes/vertices. */
		int nodeId = 0;
		for (Vertex v : qeVertices) {
			this.coordinates.put(v.getCoordinate(), nodeId);
			this.vertices.put(nodeId, new HullNode(nodeId, v.getCoordinate()));
			nodeId++;
		}

		/* Identify the list of boundary edges. To do that:
		 * - Find all edges that are `frame-border' QuadEdges;
		 * - Find all edges that are `frame' QuadEdges;
		 * - Those QuadEdges that are frame-border, but NOT frame; must
		 *   therefore be `border' QuadEdges.
		 *   TODO find out what frame and border edges really are?!
		 */
		List<QuadEdge> qeFrameBorder = new ArrayList<>();
		List<QuadEdge> qeFrame = new ArrayList<>();
		List<QuadEdge> qeBorder = new ArrayList<>();

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
			quadEdges.remove(q);
		}
		
		/* Sort the boundary list on edge length in descending order. */
		HashMap<QuadEdge, Double> qeLengths = new HashMap<>();
		for (QuadEdge qe : quadEdges) {
			qeLengths.put(qe, qe.toLineSegment().getLength());
		}
				
		QuadEdgeComparator dc = new QuadEdgeComparator(qeLengths);
		TreeMap<QuadEdge, Double> qeSorted = new TreeMap<>(dc);
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
			HullNode nodeOrigin = this.vertices.get(idOrigin);
			HullNode nodeDestination = this.vertices.get(idDestination);
		
			HullEdge edge;
			if (qeBorder.contains(qe)) {
				nodeOrigin.setBorder(true);
				nodeDestination.setBorder(true);
				edge = new HullEdge(edgeId, ls, nodeOrigin, nodeDestination, true);
				if (ls.getLength() < this.threshold) {
					this.ignoredEdges.put(edgeId, edge);
				} else {
					this.consideredEdges.put(edgeId, edge);
				}
			} else {
				edge = new HullEdge(edgeId, ls, nodeOrigin, nodeDestination, false);
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
			
			HullEdge edgeA = this.edges.get(this.segments.get(lsA));
			HullEdge edgeB = this.edges.get(this.segments.get(lsB));
			HullEdge edgeC = this.edges.get(this.segments.get(lsC));

			HullTriangle triangle = new HullTriangle(triangleId);
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
		for (HullEdge edge : this.edges.values()) {
			int numberOfTriangles = edge.getTriangles().size();
			if ( numberOfTriangles > 1) {
				HullTriangle tA = edge.getTriangles().get(0);
				HullTriangle tB = edge.getTriangles().get(1);
				tA.addNeighbour(tB);
				tB.addNeighbour(tA);
			}
			
			/* TODO Remove after debugging. */
			if(numberOfTriangles == 0){
				LOG.error("An edge not associated with a triangle!");
				LOG.warn("   --> Unique id for the group of points: " + facilityIdentifier);
			}
		}
		
		/* Write the first iteration's output. */
		int iteration = 0;
		if(this.printIterations){
			writeOutput(iteration++);
		}

		/* Iteratively remove boundary edges, add the new resulting boundary 
		 * edges, and sort on edge length until no more boundary edges can be
		 * removed. */
		while(!this.consideredEdges.isEmpty()){

			/* Identify the longest edge. */
			HullEdge e = this.consideredEdges.firstEntry().getValue();

			/* TODO Remove after debugging. This was necessary as some links 
			 * are not associated with triangles... I don't know how that happens! */
			if(e.getTriangles().size() == 0){
				LOG.warn("Considered edge without a triangle association!!");
				LOG.warn("   --> For now (20130703) we deal with this by simply making the link 'ignored'.");
				LOG.warn("   --> Unique id for the group of points: " + facilityIdentifier);
				this.consideredEdges.remove(e.getId());
				this.ignoredEdges.put(e.getId(), e);
				continue;
			} 

			HullTriangle triangle = e.getTriangles().get(0);
			List<HullTriangle> neighbours = triangle.getNeighbours();

			/* First test for irregular triangle. If a border edge belongs
			 * to a triangle that only has ONE neighbour, removing that
			 * border edge will result in an irregular triangle. 
			 * TODO Check if this test is really necessary... it seems that
			 * the second test will cover both possibilities for irregular
			 * triangles. */
			if(neighbours.size() == 1){
				/* It is irregular. Even though it's long enough to be
				 * considered for removal, it will be flagged and ignored. */
				this.consideredEdges.remove(e.getId());
				this.ignoredEdges.put(e.getId(), e);
			} else{
				HullEdge e0 = triangle.getEdges().get(0);
				HullEdge e1 = triangle.getEdges().get(1);

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
					HullTriangle tA = neighbours.get(0);
					HullTriangle tB = neighbours.get(1);
					this.triangles.remove(triangle.getId());
					tA.removeNeighbour(triangle);
					tB.removeNeighbour(triangle);

					/* Get the other two edges associated with the triangle, 
					 * as they will become the new border edges. */
					List<HullEdge> triangleEdges = triangle.getEdges();
					triangleEdges.remove(e);

					/* Remove the current edge. */
					this.edges.remove(e.getId());
					this.consideredEdges.remove(e.getId());

					/* Add the two new border edges, considering their 
					 * length as well. */
					HullEdge eA = triangleEdges.get(0);
					eA.setBorder(true);
					if(eA.getGeometry().getLength() > this.threshold){
						this.consideredEdges.put(eA.getId(), eA);
					} else{
						this.ignoredEdges.put(eA.getId(), eA);
					}
					eA.removeTriangle(triangle);

					HullEdge eB = triangleEdges.get(1);
					eB.setBorder(true);
					if(eB.getGeometry().getLength() > this.threshold){
						this.consideredEdges.put(eB.getId(), eB);
					} else{
						this.ignoredEdges.put(eB.getId(), eB);
					}
					eB.removeTriangle(triangle);

					/* Write the iteration's output. */
					if(this.printIterations){
						writeOutput(iteration++);
					}
				}
			}
		}

		/* Assemble the final concave Hull. Start by adding all the border
		 * edges. */
		List<LineString> borderEdges = new ArrayList<>();
		for(HullEdge e : this.consideredEdges.values()){
			borderEdges.add( e.getGeometry().toGeometry(this.geomFactory) );
		}
		for(HullEdge e : this.ignoredEdges.values()){
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
			LOG.warn("   --> Unique id for the group of points: " + facilityIdentifier);
			LOG.warn("   --> Unique points (" + this.filteredPoints.getNumGeometries() + "):");
			Coordinate[] ca = this.filteredPoints.getCoordinates();
			for(Coordinate c : ca){
				LOG.warn("       (" + c.x + ";" + c.y + ")");				
			}
			
			/* Create the convex hull. */
			LOG.warn("   --> Returning the convex hull.");
			return geomFactory.createMultiPoint(this.filteredPoints.getCoordinates()).convexHull();
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
		List<Coordinate> coordinateList = new ArrayList<>();
		
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


	/**
	 * Write out all the edges to the following two files:
	 * <ul>
	 *   <li> all border edges are written to one file;
	 *   <li> all edges that remain in the triangulation are written to another
	 *        file.
	 * </ul>
	 * @param iteration the current iteration number. 
	 */
	private void writeOutput(int iteration){
		/* Write the Delaunay triangles. */
		String filename = String.format("output/concaveHull/Threshold_%.0f_triangles.csv", this.threshold);
		BufferedWriter bw = IOUtils.getAppendingBufferedWriter(filename);
		try{
			for(HullEdge e : this.edges.values()){
				bw.write(String.format("%d,", iteration));
				bw.write(String.format("%.2f,", e.getOriginNode().getCoordinate().x));
				bw.write(String.format("%.2f,", e.getOriginNode().getCoordinate().y));
				bw.write(String.format("%.2f,", e.getDestinationNode().getCoordinate().x));
				bw.write(String.format("%.2f\n", e.getDestinationNode().getCoordinate().y));
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter " + filename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + filename);
			}
		}

		/* Write the Border triangles. */
		filename = String.format("output/concaveHull/Threshold_%.0f_border.csv", this.threshold);
		bw = IOUtils.getAppendingBufferedWriter(filename);
		try{
			for(HullEdge e : this.consideredEdges.values()){
				bw.write(String.format("%d,", iteration));
				bw.write(String.format("%.2f,", e.getOriginNode().getCoordinate().x));
				bw.write(String.format("%.2f,", e.getOriginNode().getCoordinate().y));
				bw.write(String.format("%.2f,", e.getDestinationNode().getCoordinate().x));
				bw.write(String.format("%.2f\n", e.getDestinationNode().getCoordinate().y));
			}
			for(HullEdge e : this.ignoredEdges.values()){
				bw.write(String.format("%d,", iteration));
				bw.write(String.format("%.2f,", e.getOriginNode().getCoordinate().x));
				bw.write(String.format("%.2f,", e.getOriginNode().getCoordinate().y));
				bw.write(String.format("%.2f,", e.getDestinationNode().getCoordinate().x));
				bw.write(String.format("%.2f\n", e.getDestinationNode().getCoordinate().y));
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter " + filename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + filename);
			}
		}
	}
	
	
	/**
	 * Implementation of the concave hull algorithm. Every iteration, starting
	 * with the zero-th iteration, will be written to file. Both the triangles, 
	 * and the border will be written to file.
	 * @param args two arguments required, and in the following order:
	 * <ol>
	 *   <li> <b>coordinate file</b> the absolute path of the coordinates to be
	 *        be read in. It is assumed that the first column of the CSV file 
	 *        will be the longitude, and the second column the latitude. Also,
	 *        the coordinate file is assumed to have a header line.
	 *   <li> <b>threshold</b> the edge length threshold that will be used for
	 *        the concave hull algorithm.
	 */
	public static void main(String[] args) {
		/* Read in the coordinates from file. */
		List<Coordinate> coordinates = ConcaveHull.getClusterCoords(args[0]);
		
		/* Convert the coordinates to a GeometryCollection. */
		GeometryFactory gf = new GeometryFactory();
		Geometry[] points = new Geometry[coordinates.size()];
		for(int i = 0; i < coordinates.size(); i++){
			points[i] = gf.createPoint(coordinates.get(i));
		}
		GeometryCollection gc = new GeometryCollection(points, gf);
		
		/* Instantiate and run the concave hull algorithm. */
		ConcaveHull ch = new ConcaveHull(gc, Double.parseDouble(args[1]), true);
		Geometry g = ch.getConcaveHull();
	}
	
}
