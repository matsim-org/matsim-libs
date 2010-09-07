/* *********************************************************************** *
 * project: org.matsim.*
 * ConvexMeshSimplifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.flooding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.evacuation.flooding.FloodingInfo;
import org.matsim.evacuation.flooding.FloodingReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.MY_STATIC_STUFF;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ConvexMeshSimplifier {

	private static final Logger log = Logger.getLogger(ConvexMeshSimplifier.class);

	private static final double PI_HALF = Math.PI /2;


	private static final double TWO_PI =  2 * Math.PI;


	private static final boolean DEBUG = true;
	List<List<Integer>> geos = new ArrayList<List<Integer>>();


	private FloodingReader reader;


	private List<FloodingInfo> fis;

	private final Set<Edge> removed = new HashSet<Edge>();
	private final Set<Edge> marked = new HashSet<Edge>();
//	private final List<Edge> marked2 = new ArrayList<Edge>();
//	private final Set<Edge> warn = new HashSet<Edge>();
//	private final Set<Edge> produced = new HashSet<Edge>();

	private Map<Integer, Integer> mapping;

	private Map<Coordinate, Integer> coordKeyMap;
	private QuadTree<Coordinate> coordQuadTree;

	//	private QuadTree<Polygon> polygons;



	private final Map<Integer, Set<Edge>> network = new HashMap<Integer, Set<Edge>>();
	//	private final Map<Integer>


	private final GeometryFactory geofac = new GeometryFactory();

	private FeatureType ft;


	private FeatureType ftLine;

	private boolean initialized = false;

	public ConvexMeshSimplifier(String netcdf) {
		this.reader = new FloodingReader(netcdf);
		this.reader.setReadTriangles(true);
		this.reader.setReadFloodingSeries(true);
		this.reader.setMaxTimeStep(90);
		double offsetEast = 632968.461027224;
		double offsetNorth = 9880201.726;
		this.reader.setOffset(offsetEast, offsetNorth);
	}

	
	public ConvexMeshSimplifier(String netcdf, String aoi) {
		this.reader = new FloodingReader(netcdf);
		this.reader.setReadTriangles(true);
		this.reader.setReadFloodingSeries(true);
		this.reader.setMaxTimeStep(90);
		
		try {
			FeatureSource geocoll = ShapeFileReader.readDataFile(aoi);
			Feature ft =  (Feature) geocoll.getFeatures().iterator().next();
			Geometry geo = ft.getDefaultGeometry();
			this.reader.setFloodingArea(geo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		double offsetEast = 632968.461027224;
		double offsetNorth = 9880201.726;
		this.reader.setOffset(offsetEast, offsetNorth);
	}


	public ArrayList<List<FloodingInfo>> getInundationGeometries(){
		
		ArrayList<List<FloodingInfo>> ret = new ArrayList<List<FloodingInfo>>();
		
		if (!this.initialized  ) {
			run();
		}
		for (List<Integer> geo : this.geos) {
			List<FloodingInfo> info = new ArrayList<FloodingInfo>();
			for (int i = 0; i < geo.size()-1; i++) {
				Integer idx = this.mapping.get(geo.get(i));
				info.add(this.fis.get(idx));
			}
			ret.add(info);
		}
		
		return ret;
	}
	
	private void run() {
		this.fis = this.reader.getFloodingInfos();

		this.mapping = this.reader.getIdxMapping();
		Stack<Edge> edges = loadNetwork();
		loadCoordMapping();
		this.reader = null;
		//		Queue<Edge> edges = new ConcurrentLinkedQueue<Edge>();

		//		edges.clear();
		edges.push(getE1());
		//		edges.add(e1);
		//		tmp.clear();

		runEdgesII(edges);



		if (DEBUG) {
			dump(this.geos,"../../tmp/geometries.shp",true);
			List<List<Integer>> geos = new ArrayList<List<Integer>>();
			//			for (Set<Edge> es : this.network.values()) {
			//				for (Edge e : es) {
			//					List<Integer> geo = new ArrayList<Integer>();
			//					geo.add(e.from);
			//					geo.add(e.to);
			//					geos.add(geo);
			//				}
			//			}
			//			dump(geos,"../../tmp/edges.shp",false);
			geos.clear();
			for (Edge e : this.removed) {
				List<Integer> geo = new ArrayList<Integer>();
				geo.add(e.from);
				geo.add(e.to);
				geos.add(geo);
			}
			dump(geos,"../../tmp/removed.shp",false);
//			geos.clear();
//			for (Edge e : this.marked2) {
//				List<Integer> geo = new ArrayList<Integer>();
//				geo.add(e.from);
//				geo.add(e.to);
//				geos.add(geo);
//			}
//			//			}
//			dump(geos,"../../tmp/marked2.shp",false);

		}
		
		this.initialized = true;
	}



	private void runEdgesII(Stack<Edge> edges) {
		int next = 1;
		while (!edges.isEmpty()) {

			if (this.removed.contains(edges.peek())) {
				Edge e = edges.pop();
				e.successors.clear();
				this.marked.remove(e);
				
			} else {
				Queue<Edge> tmp = runEdgeII(edges.pop());
				while(tmp.size() > 0) {
					edges.push(tmp.poll());
				}
			}
			if (this.geos.size() >= next) {
				log.info("convex geos found: " + this.geos.size());
				next *= 2;
			}
			if (this.geos.size() % 10000 == 0) {
				for (Set<Edge> es : this.network.values()) {
					Stack<Edge> rm = new Stack<Edge>();
					for (Edge e: es) {
						if (this.removed.contains(e)) {
							rm.push(e);
						}
					}
					for (Edge e : rm) {
						es.remove(e);
					}
				}
			}
		}
		log.info("convex geos found: " + this.geos.size());
	}

	private void loadCoordMapping() {
		this.coordKeyMap = new HashMap<Coordinate, Integer>();
		Envelope e = this.reader.getEnvelope();
		this.coordQuadTree = new QuadTree<Coordinate>(e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY());
		for (Entry<Integer, Integer> entry : this.mapping.entrySet()) {
			Coordinate c = this.fis.get(entry.getValue()).getCoordinate();
			this.coordKeyMap.put(c, entry.getKey());
			this.coordQuadTree.put(c.x, c.y, c);
		}

	}


	private Queue<Edge> runEdgeII(Edge root) {


		Queue<Edge> ret = new ConcurrentLinkedQueue<Edge>();

		Set<Integer> el = new HashSet<Integer>();
		Stack<Edge> elStack = new Stack<Edge>();
		Set<Edge> blamed = new HashSet<Edge>();

		el.add(root.to);
		elStack.push(root);
		Edge current = root;

		boolean turnRight = true;
		do {
			current = getNextEdgeConvex(current, blamed, turnRight);

			if (current == null) {
				current = elStack.pop();
				el.remove(current.to);
				Stack<Edge> stack = current.successors;
				while (!stack.isEmpty()) {
					Edge e = stack.pop();
					blamed.remove(e);
					while (!e.successors.isEmpty()) {
						stack.push(e.successors.pop());
					}
				}

				blamed.add(current);

				if (elStack.size() == 0) {
					if (!turnRight) {
						el.add(root.to);
						elStack.push(root);
						current = root;
						turnRight = false;
					} else {
						return ret;
					}
				} 
				current = elStack.peek();
			} else if (!current.equals(root) && el.contains(current.to)) {
				elStack.peek().successors.add(current);
				current.successors.clear();
				blamed.add(current);
				current = elStack.peek();
			} else {
				elStack.peek().successors.add(current);
				current.successors.clear();
				elStack.push(current);
				el.add(current.to);
			}


		}while (!current.equals(root) || elStack.size() == 1);



		cleanUp(elStack,el);


		List<Integer> coords = new ArrayList<Integer>();

		Edge e = elStack.pop();
		coords.add(e.to);
		while (!elStack.isEmpty()) {
			e = elStack.pop();
			ret.add(new Edge(e.to,e.from));
			coords.add(e.to);
		}

		this.geos.add(coords);
		return ret;
	}



	private boolean cleanUp(Stack<Edge> coords, Set<Integer> el) {

		Coordinate [] shell = new Coordinate[coords.size()];
		Set<Edge> shellEdges = new HashSet<Edge>();
		int pos = 0;

		Envelope e = new Envelope();
		for (Edge edge : coords) {
			int idx = this.mapping.get(edge.to);
			shellEdges.add(edge);
			Coordinate c = this.fis.get(idx).getCoordinate();
			shell[pos++] = c;
			e.expandToInclude(c.x, c.y);
		}
		LinearRing lr = this.geofac.createLinearRing(shell);
		Polygon p = this.geofac.createPolygon(lr, null);

		Collection<Coordinate> values = new ArrayList<Coordinate>();
		this.coordQuadTree.get(e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY(), values);
		for (Coordinate c : values) {
			Point point = this.geofac.createPoint(c);
			if (p.contains(point) && p.getExteriorRing().distance(point) > 0.01) {
				int idx = this.coordKeyMap.get(c);
				Set<Edge> s = this.network.get(idx);
				if (s == null) {
					continue;
				}

				for (Edge edge : s) {
					this.removed.add(edge);
					this.removed.add(new Edge(edge.to,edge.from));

				}

				this.network.remove(idx);
			}
		}


		//walk around the shell and remove edges cutting geometry
		for (int i = 1; i < coords.size(); i++) {
			Set<Edge> s = this.network.get(coords.get(i).to);
			int j = i + 1;
			if (j >= coords.size()) {
				j = 1;
			}
			int pred = coords.get(i-1).to;
			int succ = coords.get(j).to;

			for (Edge edge : s) {
				if ((edge.to != pred) && (edge.to != succ) && el.contains(edge.to)) {
					this.removed.add(edge);

				}
			}
		}


		for (int i = 1; i < coords.size(); i++) {
			Edge edge = coords.get(i);
			if (this.marked.contains(edge)) {
				this.removed.add(edge);

				this.removed.add(new Edge(edge.to,edge.from));
			} else {
				this.marked.add(edge);
				this.marked.add(new Edge(edge.to,edge.from));
			}
		}
		return true;
	}



	private double getAngle(Edge e1, Edge e2, boolean turnRight) {
		Integer idx = this.mapping.get(e1.to);
		Coordinate zero = this.fis.get(idx).getCoordinate();
		idx = this.mapping.get(e1.from);
		Coordinate c0 = this.fis.get(idx).getCoordinate();
		Coordinate v0 = new Coordinate(c0.x-zero.x,c0.y-zero.y);
		idx = this.mapping.get(e2.to);
		Coordinate c1 = this.fis.get(idx).getCoordinate();
		Coordinate v1 = new Coordinate(c1.x-zero.x,c1.y-zero.y);
		double alpha = getPhaseAngle(v0);
		double beta = getPhaseAngle(v1);
		double gamma = 0;
		if (turnRight) {gamma = beta - alpha;}
		else { gamma = alpha - beta; }
		gamma = gamma < 0 ? gamma + TWO_PI : gamma;
		return gamma;
	}


	private Edge getNextEdgeConvex(Edge e, Set<Edge> blamed, boolean turnRight) {
		Set<Edge> fwd = this.network.get(e.to);
		if (fwd == null) {
			return null;
		}
		
		Integer idx = this.mapping.get(e.to);
		if (idx == null) {
			return null;
		}
		Coordinate zero = this.fis.get(idx).getCoordinate();
		idx = this.mapping.get(e.from);
		Coordinate c0 = this.fis.get(idx).getCoordinate();
		Coordinate v0 = new Coordinate(c0.x-zero.x,c0.y-zero.y);
		double alpha = getPhaseAngle(v0);

		double maxGamma = 0;
		Edge bestEdge = null;
		for (Edge tmp : fwd) {

			if (this.removed.contains(tmp) || blamed.contains(tmp) ) {
				continue;
			}

			idx = this.mapping.get(tmp.to);
			if (idx == null) {
				return null;
			}
			Coordinate c1 = this.fis.get(idx).getCoordinate();
			Coordinate v1 = new Coordinate(c1.x-zero.x,c1.y-zero.y);
			double beta = getPhaseAngle(v1);
			double gamma = 0;
			if (turnRight) {gamma = beta - alpha;}
			else {gamma = alpha - beta;}
			gamma = gamma < 0 ? gamma + TWO_PI : gamma;
			if (gamma <= Math.PI && gamma > maxGamma) {
				maxGamma = gamma;
				bestEdge = tmp;
			}
		}

		return bestEdge;
	}

	private double getPhaseAngle(Coordinate v0) {
		double alpha = 0.0;
		if (v0.x > 0) {
			alpha = Math.atan(v0.y/v0.x);
		} else if (v0.x < 0) {
			alpha = Math.PI + Math.atan(v0.y/v0.x);
		} else { // i.e. DX==0
			if (v0.y > 0) {
				alpha = PI_HALF;
			} else {
				alpha = -PI_HALF;
			}
		}
		if (alpha < 0.0) alpha += TWO_PI;
		return alpha;
	}

	private Edge getE1() {
		Set<Edge> edges = null;
		int i = 12000; //(int)(Math.random() * 1000);
		while(edges == null) {
			edges = this.network.get(i++);
		}

		return edges.iterator().next();
	}

	private void dump(List<List<Integer>> geos, String file, boolean polygon) {
		initFeatures();
		GeometryFactory geofac = new GeometryFactory();
		List<Feature> fts = new ArrayList<Feature>();
		double ii = 0;
		for (List<Integer> geo : geos) {
			Coordinate [] coords = new Coordinate[geo.size()];
			int j = 0;
			boolean cont = false;
			for (Integer i : geo) {
				Integer idx = this.mapping.get(i);
				if (idx == null) {
					cont = true;
					break;
				}

				FloodingInfo fi = this.fis.get(idx);

				coords[j++] = fi.getCoordinate();

			}
			if (cont) {
				continue;	
			}

			if (polygon) {
				LinearRing lr = geofac.createLinearRing(coords);
				Polygon p = this.geofac.createPolygon(lr, null);
				try {
					fts.add(this.ft.create(new Object[]{p,ii++,0.}));
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}				
			} else {
				LineString ls = geofac.createLineString(coords);

				try {
					fts.add(this.ftLine.create(new Object[]{ls,ii++,0.}));
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			ShapeFileWriter.writeGeometries(fts,file);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}



	private Stack<Edge> loadNetwork() {

		Stack<Edge> ret = new Stack<Edge>();
		List<int[]> tris = this.reader.getTriangles();
		for (int [] tri : tris) {
			int j = 2;
			for (int i = 0; i < 3; i++) {


				Integer idx0  = this.mapping.get(tri[j]);
				Integer idx1  = this.mapping.get(tri[i]);

				if (idx0 == null || idx1 == null ) {
					j = i;
					continue;
				}
				if (this.fis.get(idx0) == null || this.fis.get(idx1) == null) {
					j = i;
					continue;
				}
				Set<Edge> node = this.network.get(tri[j]);
				if (node == null) {
					node = new HashSet<Edge>();
					this.network.put(tri[j], node);
				}
				Edge e = new Edge(tri[j], tri[i]);
				Edge e2 =new Edge(tri[i], tri[j]);
				node.add(e);
				Set<Edge> node2 = this.network.get(tri[i]);
				if (node2 == null) {
					node2 = new HashSet<Edge>();
					this.network.put(tri[i], node2);
				}
				node2.add(e2);
				j = i;
				ret.push(e2);
				ret.push(e);
			}
		}
		return ret;
	}


	public static class Edge {
		int from;
		int to;
		int hash;

		Stack<Edge> successors = new Stack<Edge>();

		public Edge(int n1, int n2) {
			this.from = n1;
			this.to = n2;
			this.hash = (n1 + " " + n2).hashCode();

		}

		@Override
		public int hashCode() {
			return this.hash;
		}

		@Override
		public boolean equals(Object obj) {
			return this.hash == obj.hashCode();
		}

	}


	public static void main(String [] args) {
		String netcdf = MY_STATIC_STUFF.SWW_ROOT + "/" + MY_STATIC_STUFF.SWW_PREFIX + 1 + MY_STATIC_STUFF.SWW_SUFFIX;
		String aoi = MY_STATIC_STUFF.SWW_ROOT + "/aoi.shp";
//		new BasicInundationGeometryLoader(netcdf).getInundationGeometries();
		new ConvexMeshSimplifier(netcdf,aoi).run();

	}


	private void initFeatures() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		AttributeType p = DefaultAttributeTypeFactory.newAttributeType(
				"Polygon", Polygon.class, true, null, null, targetCRS);
		AttributeType l = DefaultAttributeTypeFactory.newAttributeType(
				"LineString", LineString.class, true, null, null, targetCRS);
		AttributeType z = AttributeTypeFactory.newAttributeType(
				"dblAvgZ", Double.class);
		AttributeType t = AttributeTypeFactory.newAttributeType(
				"dblAvgT", Double.class);

		Exception ex;
		try {
			this.ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { p, z, t }, "Polygon");
			this.ftLine = FeatureTypeFactory.newFeatureType(new AttributeType[] { l, z, t }, "Line");
			return;
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);

	}

	private static class Dump {

		private static final String file = "../../tmp/geometries.shp";
		private FeatureType ft;
		private final Collection<Feature> fts = new ArrayList<Feature>();
		private FeatureType ftLine;

		public Dump(List<List<FloodingInfo>> geometries) {
			initFeatures();
			createFts(geometries);
			try {
				ShapeFileWriter.writeGeometries(this.fts, file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void createFts(List<List<FloodingInfo>> geometries) {

			GeometryFactory geofac = new GeometryFactory();

			for (List<FloodingInfo> geo : geometries) {
				if (geo.size() < 3) {
					throw new RuntimeException("this should never happen!");
				}
				Coordinate [] coords = new Coordinate[geo.size()+1];
				for (int i = 0; i < geo.size(); i++) {
					coords[i] = geo.get(i).getCoordinate();
				}
				coords[geo.size()] = coords[0];

				LinearRing shell = geofac.createLinearRing(coords);
				Polygon p = geofac.createPolygon(shell, null);
				try {
					this.fts.add(this.ft.create(new Object[]{p,0.,0.}));
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}
			}

		}

		private void initFeatures() {
			CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
			AttributeType p = DefaultAttributeTypeFactory.newAttributeType(
					"Polygon", Polygon.class, true, null, null, targetCRS);
			AttributeType l = DefaultAttributeTypeFactory.newAttributeType(
					"LineString", LineString.class, true, null, null, targetCRS);
			AttributeType z = AttributeTypeFactory.newAttributeType(
					"dblAvgZ", Double.class);
			AttributeType t = AttributeTypeFactory.newAttributeType(
					"dblAvgT", Double.class);

			Exception ex;
			try {
				this.ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { p, z, t }, "Polygon");
				this.ftLine = FeatureTypeFactory.newFeatureType(new AttributeType[] { l, z, t }, "Line");
				return;
			} catch (FactoryRegistryException e) {
				ex = e;
			} catch (SchemaException e) {
				ex = e;
			}
			throw new RuntimeException(ex);

		}

	}


}
