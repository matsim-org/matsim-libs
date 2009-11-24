package playground.gregor.flooding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.evacuation.flooding.FloodingInfo;
import org.matsim.evacuation.flooding.FloodingReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

import playground.gregor.MY_STATIC_STUFF;

public class TriangularMeshSimplifier {


	private static final Logger log = Logger.getLogger(TriangularMeshSimplifier.class);

	private static final double MAX_LENGTH = 500;

	private final List<List<FloodingInfo>> geos = new ArrayList<List<FloodingInfo>>();

	boolean initialized = false;

	private final String netcdf;

	private FloodingReader reader;

	private Map<Integer, Integer> mapping;

	private final Map<Integer, Set<Edge>> network = new HashMap<Integer, Set<Edge>>();

	private final Map<Edge,Set<Triangle>> edgeTriangleMapping = new HashMap<Edge,Set<Triangle>>();
	private final Map<Triangle,Set<Edge>> triangleEdgeMapping = new HashMap<Triangle,Set<Edge>>();

	private  Set<Edge> edges;	
	private List<FloodingInfo> fis;

	private final Set<Edge> removed = new HashSet<Edge>();

	private FeatureType ft;

	private FeatureType ftLine;

	private Queue<Edge> queue;

//	private final Set<Triangle> rmTriangles = new HashSet<Triangle>();
//	private final Set<Triangle> mvTriangles = new HashSet<Triangle>();
//	private final Set<Triangle> newTriangles = new HashSet<Triangle>();
	private final Stack<Edge> badEdges = new Stack<Edge>();

	private final boolean run = true;

	private final GeometryFactory geofac = new GeometryFactory();

//	private final Stack<Edge> cleanUps = new Stack<Edge>();

	public TriangularMeshSimplifier(String netcdf) {
		this.netcdf = netcdf;
	}



	public List<List<FloodingInfo>> getInundationGeometries() {
		;
		if (!this.initialized){
			init();
		}
		
		for (Triangle tri : this.triangleEdgeMapping.keySet()) {
			List<FloodingInfo> fi = new ArrayList<FloodingInfo>();
			for (int i = 0; i < 3; i++) {
				int idx = this.mapping.get(tri.coords[i]);
				fi.add(this.fis.get(idx));
			}
			this.geos.add(fi);
			
		}
		
		return this.geos;

	}
	private void init() {
		this.reader = new FloodingReader(this.netcdf);
		this.reader.setReadFloodingSeries(true);
		this.reader.setReadTriangles(true);
		this.reader.setMaxTimeStep(120);
		double offsetEast = 632968.461027224;
		double offsetNorth = 9880201.726;
		this.reader.setOffset(offsetEast, offsetNorth);
		this.mapping = this.reader.getIdxMapping();
		this.fis = this.reader.getFloodingInfos();

		Set<Edge> edges = null;
		try {
			edges = loadNetwork();
		} catch (Exception e1) {
			e1.printStackTrace();
//			dumpGeos();
			throw new RuntimeException(e1);
		}
		this.edges = edges;
		this.queue = new PriorityQueue<Edge>();
		this.queue.addAll(edges);


		while (this.queue.size() > 0) {
			Edge e = this.queue.poll();
			if (this.removed.contains(e) || this.network.get(e.to) == null || this.network.get(e.from) == null) {
				continue;
			}

			if (e.length > MAX_LENGTH) {
				continue;
			} //else if (isBorderRegion(e)) {
			//				border.add(e);
			//				continue;
			//}

//			this.rmTriangles.clear();
//			this.mvTriangles.clear();
//			this.newTriangles.clear();
			contractEdge(e);
			while (!this.badEdges.isEmpty()) {
				handleBadEdge(this.badEdges.pop());
			}
//			while (!this.cleanUps.isEmpty()) {
////				cleanUpEdge(this.cleanUps.pop());
//				this.cleanUps.pop();
//			}
			//			if (this.rmTriangles.size() > 50 && this.newTriangles.size() > 50) {
			//				break;
			//			}
			if (this.run == false) {
				break;
			}

		}


//		dumpGeos();

		this.initialized = true;
	}


	private void cleanUpEdge(Edge pop) {
		if (this.removed.contains(pop)) {
			return;
		}
		Set<Triangle> tris = this.edgeTriangleMapping.get(pop);
		if (tris == null) {
			return;
		}
		
		if (tris.size() != 2) {
			return;
		}
		double norm = 0;
		for (Triangle tri : tris) {
			norm += getNormalizedVariance(tri);
//			System.out.println(norm);
		}
		List<Triangle> tl = new ArrayList<Triangle>(tris);
		Triangle tri1 = tl.get(0);
		Triangle tri2 = tl.get(1);
		int [] touch = new int[2];
		int countTouch = 0;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				if (tri1.coords[i] == tri2.coords[j]) {
					touch[countTouch++] = tri1.coords[i];
					break;
				}
			}
			if (countTouch == 2) {
				break;
			}
		}
		int coords1[] = new int [3];
		int coords2[] = new int [3];
		coords1[0] = touch[0];
		coords2[0] = touch[1];
		int count = 1;
		for (int i = 0; i < 3; i++) {
			if (tri1.coords[i] != coords1[0] && tri1.coords[i] != coords2[0]) {
				coords1[count] = tri1.coords[i];
				coords2[count++] = tri1.coords[i];
			}
			if (tri2.coords[i] != coords1[0] && tri2.coords[i] != coords2[0]) {
				coords1[count] = tri2.coords[i];
				coords2[count++] = tri2.coords[i];
			}
		}

		double norm2 = getNormalizedVariance(coords1);
		norm2 += getNormalizedVariance(coords2);
		
		
//		if (norm2 > norm) {
//			Edge newEdge = createEdge(coords1[1],coords1[2]);
//			Edge newEdge2 = createEdge(coords1[2],coords1[1]);
//			this.network.get(coords1[1]).add(newEdge);
//			this.network.get(coords1[2]).add(newEdge2);
//			this.queue.add(newEdge);
//			this.edges.add(newEdge);
//			removeEdge(pop);
//			addEdge(newEdge);
//		}
		
	
		//TODO
		// if norm > threshold
//		removeEdge(...);
//		addEdge(...);
	}

	

	private double getNormalizedVariance(int[] coords1) {
		Edge e0 = createEdge(coords1[0], coords1[1]);
		Edge e1 = createEdge(coords1[1], coords1[2]);
		Edge e2 = createEdge(coords1[0], coords1[2]);
		double avgLength = e0.length + e1.length + e2.length;
		avgLength /= 3;
		
		double norm = Math.pow(e0.length-avgLength, 2) + Math.pow(e1.length-avgLength, 2) + Math.pow(e2.length-avgLength, 2);
		norm /= 3;
		norm /= avgLength;
		
		return norm;
	}



	private double getNormalizedVariance(Triangle tri) {
		Set<Edge> edges = this.triangleEdgeMapping.get(tri);
		double avgLength = 0;
		for (Edge edge : edges) {
			avgLength += edge.length;
		}
		avgLength /= 3;
		
		double norm = 0;
		for (Edge edge : edges) {
			norm += Math.pow(edge.length-avgLength, 2);
		}
		norm /= 3;
		norm /= avgLength;
		
		return norm;
	}



	private void handleBadEdge(Edge edge) {
		Set<Triangle> tmp = this.edgeTriangleMapping.get(edge);
		if (tmp == null) {
			log.warn("edge not associated with any triangle!");
			//			removeEdge(edge);
			return;
		}
		List<Triangle> tris = new ArrayList<Triangle>(tmp);
		List<Triangle> rms = new ArrayList<Triangle>();
		for (int i = 0; i < tris.size()-1; i++) {
			for (int j = i+1; j < tris.size(); j++) {
				Triangle rm = getOverlappedTriangle(tris.get(i),tris.get(j));
				if (rm != null) {
					rms.add(rm);
//					this.mvTriangles.add(rm);
				}
			}
		}
		if (rms.size() == 0) {
			//			throw new RuntimeException("There must be at least one removable triangle!!");
			log.warn("couldn not repair, just removing edge!");
			//			removeEdge(edge);
			return;
		}

		if (rms.size() > 1) {
			log.warn("not implemented yet!");
			return;
		}
		Set<Edge> affected = new HashSet<Edge>();
		affected.addAll(this.triangleEdgeMapping.remove(rms.get(0)));
		for (Edge e : affected) {
			this.edgeTriangleMapping.get(e).remove(rms.get(0));
		}
		affected.remove(edge);
		for (Edge e : affected) {
			removeEdge(e);
		}
	}



	private Triangle getOverlappedTriangle(Triangle tri1, Triangle tri2) {
		Polygon p1 = getTrianglePolygon(tri1);
		Polygon p2 = getTrianglePolygon(tri2);

		if (p1.contains(p2)) {
			return tri2;
		} 

		if (p2.contains(p1)) {
			return tri1;
		}

		if (p1.covers(p2)) {
			log.warn("poly covered but not contained!");
			return tri2;
		} 

		if (p2.covers(p1)) {
			log.warn("poly covered but not contained!");
			return tri1;
		}

		//		if (p1.overlaps(p2)) {
		//			log.warn("poly only overlaped!");
		//			if (p1.getArea() > p2.getArea()) {
		//				return tri1;
		//			} else {
		//				return tri2;
		//			}
		//		}

		return null;
	}



	private Polygon getTrianglePolygon(Triangle tri) {
		Coordinate [] coords = new Coordinate[4];
		for (int i = 0; i < 3; i++) {
			Integer idx = this.mapping.get(tri.coords[i]);
			coords[i] = this.fis.get(idx).getCoordinate();
		}
		coords[3] = coords[0];
		LinearRing lr = this.geofac.createLinearRing(coords);
		return this.geofac.createPolygon(lr, null);
	}



	//DEBUG
	private void dumpGeos() {

		List<List<Integer>> geos = new ArrayList<List<Integer>>();
		for (Triangle tri : this.triangleEdgeMapping.keySet()) {
			List<Integer> geo = new ArrayList<Integer>();
			geo.add(tri.coords[0]);
			geo.add(tri.coords[1]);
			geo.add(tri.coords[2]);
			geo.add(tri.coords[0]);
			geos.add(geo);

		}
		dump(geos,"../../tmp/geometries.shp",true);
//
//		geos.clear();
//		for (Triangle tri : this.rmTriangles) {
//			List<Integer> geo = new ArrayList<Integer>();
//			geo.add(tri.coords[0]);
//			geo.add(tri.coords[1]);
//			geo.add(tri.coords[2]);
//			geo.add(tri.coords[0]);
//			geos.add(geo);
//
//		}
//		dump(geos,"../../tmp/rmTriangles.shp",true);
//
//		geos.clear();
//		for (Triangle tri : this.mvTriangles) {
//			List<Integer> geo = new ArrayList<Integer>();
//			geo.add(tri.coords[0]);
//			geo.add(tri.coords[1]);
//			geo.add(tri.coords[2]);
//			geo.add(tri.coords[0]);
//			geos.add(geo);
//
//		}
//		dump(geos,"../../tmp/mvTriangles.shp",true);
//		geos.clear();
//		for (Edge e : this.badEdges) {
//			List<Integer> geo = new ArrayList<Integer>();
//			geo.add(e.from);
//			geo.add(e.to);
//			geos.add(geo);
//		}
//		dump(geos,"../../tmp/badEdges.shp",false);
//		geos.clear();
//		for (Triangle tri : this.newTriangles) {
//			List<Integer> geo = new ArrayList<Integer>();
//			geo.add(tri.coords[0]);
//			geo.add(tri.coords[1]);
//			geo.add(tri.coords[2]);
//			geo.add(tri.coords[0]);
//			geos.add(geo);
//
//		}
//		dump(geos,"../../tmp/newTriangles.shp",true);
//
//		geos.clear();
//		this.edges.clear();
//		for (Set<Edge> node : this.network.values()) {
//			for (Edge e : node) {
//				if (!this.removed.contains(e)) {
//					//					throw new RuntimeException("This will never happen!");
//
//					this.edges.add(e);
//				}
//			}
//		}
//		for (Edge e : this.badEdges) {
//			List<Integer> geo = new ArrayList<Integer>();
//			geo.add(e.from);
//			geo.add(e.to);
//			geos.add(geo);
//		}
//		dump(geos,"../../tmp/badEdges.shp",false);
//
//		//		for (Edge e : this.edges) {
//		//			List<Integer> geo = new ArrayList<Integer>();
//		//			geo.add(e.from);
//		//			geo.add(e.to);
//		//			geos.add(geo);
//		//		}
//		//		dump(geos,"../../tmp/edges.shp",false);
	}

	private void contractEdge(Edge e) {
		Set<Edge> fromNode = this.network.get(e.from);
		Set<Edge> toNode = this.network.get(e.to);
		//		contractEdge(e,toNode, fromNode,e.from, e.to);


		int degFrom = getMinNeighborDegree(fromNode);
		int degTo = getMinNeighborDegree(toNode);
		if (degFrom <=4 && degTo <= 4) {
			return;
		}
		if (degFrom <=2 || degTo <= 2) {
			return;
		}
		
		if (degFrom <=3) {
			contractEdge(e,toNode, fromNode,e.from, e.to);
		} else if (degTo <= 3) {
			contractEdge(e,fromNode,toNode,e.to, e.from);
		} else if (degFrom > degTo) {
			contractEdge(e,fromNode,toNode,e.to, e.from);	
		} else {
			contractEdge(e,toNode, fromNode,e.from, e.to);	
		}

		//		if (getMinNeighborDegree(fromNode) > 2) {
		//			
		//		} else  if (getMinNeighborDegree(toNode) > 2) {
		//			
		//		}

	}



	private void contractEdge(Edge e, Set<Edge> delete, Set<Edge> keep,int keepId, int rmId) {
		//		keep.remove(e);
		//		removeEdge(e);
//		Integer idx1 = this.mapping.get(keepId);
//		Coordinate c1 = this.fis.get(idx1).getCoordinate();
		Set<Edge> genEdges = new HashSet<Edge>();
		List<Edge> rms = new ArrayList<Edge>();
		for (Edge del : delete) {
			rms.add(del);

			int from = del.to;
			if (from == keepId) {
				continue;
			}
//			Integer idx2 = this.mapping.get(from);
//			Coordinate c2 = this.fis.get(idx2).getCoordinate();
//			double dist = c2.distance(c1);
			Edge e1 = createEdge(from, keepId);//new Edge(from,keepId,dist);
			//			Edge e2 = new Edge(keepId,from,dist);
			if (!this.edges.contains(e1)) {
				genEdges.add(e1);
			}


		}
		for (Edge rm : rms) {
			removeEdge(rm);
		}

		for (Edge edge : genEdges) {
			this.network.get(edge.from).add(edge);
			Edge e2 = createEdge(edge.to, edge.from);//new Edge(edge.to,edge.from,edge.length);
			this.network.get(e2.from).add(e2);
			this.edges.add(edge);
		}
		for (Edge edge : genEdges) {
			addEdge(edge);
			this.queue.add(edge);
//			this.cleanUps.push(edge);
		}

		if (keep.size() < 2 ) {
			System.err.println("this should not happen!");
		}

		//		if (this.network.remove(rmId) == null){
		//			throw new RuntimeException("will never happen");
		//		}
	}


	private void addEdge(Edge e) {
		Set<Edge> s1 = this.network.get(e.from);
		Set<Edge> s2 = this.network.get(e.to);
		Set<Integer> tmp = new HashSet<Integer>();
		List<Integer> nodes = new ArrayList<Integer>();
		for (Edge edge : s1) {
			tmp.add(edge.to);
		}
		for (Edge edge : s2) {
			if (tmp.contains(edge.to)) {
				nodes.add(edge.to);
			}
		}
		if (nodes.size() == 0) {
			log.warn("Trying to add a non connected edge!");
		} else if (nodes.size() > 2) {
			//			log.error("Each edge can only be part of two triangles!");
			//			this.run = false;
			this.badEdges.push(e);
			//			this.badEdges.addAll(s1);
			//			this.badEdges.addAll(s2);
			//			throw new RuntimeException("Each edge can only be part of two triangles!");
		}


		for (int i : nodes) {
			addEdgeTriangle(s1,s2,i,e);
		}


	}

	private void removeEdge(Edge e) {
		if (!this.network.get(e.to).remove(e)) { 
			throw new RuntimeException("Trying to remove no existing edge!");
		}
		if (!this.network.get(e.from).remove(e)) { 
			throw new RuntimeException("Trying to remove no existing edge!");
		}
		Set<Triangle> tris = this.edgeTriangleMapping.get(e); 
		if (tris == null){
			log.warn("Edge was not associated with any triangle!");
			this.edgeTriangleMapping.remove(e);
			this.removed.add(e);

			if (this.network.get(e.from).size() == 0) {
				this.network.remove(e.from);
			}
			return;
		}
		for (Triangle tri : tris) {
			Set<Edge> edges = this.triangleEdgeMapping.remove(tri);
			for (Edge edge : edges) {
				if (edge.equals(e)) {
					continue;
				}
				this.edgeTriangleMapping.get(edge).remove(tri);
//				this.rmTriangles.add(tri);
			}
		}
		this.edgeTriangleMapping.remove(e);
		this.removed.add(e);

		if (this.network.get(e.from).size() == 0) {
			this.network.remove(e.from);
		}
		if (this.network.get(e.to).size() == 0) {
			this.network.remove(e.to);
		}
	}

	private void addEdgeTriangle(Set<Edge>s1, Set<Edge> s2, int node,Edge e){
		Set<Edge> edges = new HashSet<Edge>();
		for (Edge edge : s1) {
			if (edge.to == node) {
				edges.add(edge);
			}
		}
		for (Edge edge : s2) {
			if (edge.to == node) {
				edges.add(edge);
			}
		}
		edges.add(e);
		if (edges.size() != 3) {
			throw new RuntimeException("A triangle has three edges!");
		}


		int [] coords = {e.from, e.to, node};
		Triangle tri1 = new Triangle(coords);

		//		//DEBUG
		//		if (this.newTriangles.contains(tri1)) {
		//			log.error("already there!");
		//		}
//		this.newTriangles.add(tri1);

		this.triangleEdgeMapping.put(tri1, edges);

		for (Edge edge : edges) {
			Set<Triangle> tris = this.edgeTriangleMapping.get(edge);
			if (tris == null){
				tris = new HashSet<Triangle>();
				this.edgeTriangleMapping.put(edge, tris);
			}
			tris.add(tri1);
		}
	}

	private void addInitialEdgeTriangle(Edge e, Triangle tri) {
		Set<Triangle> tris = this.edgeTriangleMapping.get(e);
		if (tris == null) {
			tris = new HashSet<Triangle>();
			this.edgeTriangleMapping.put(e, tris);
		} else if (tris.size() >= 2) {
			throw new RuntimeException("Each edge can only be part of two triangles!");
		}
		tris.add(tri);
		Set<Edge> edges = this.triangleEdgeMapping.get(tri);
		if (edges == null) {
			edges = new HashSet<Edge>();
			this.triangleEdgeMapping.put(tri, edges);
		} else if (edges.size() >= 3) {
			throw new RuntimeException("A triangle has only three edges!");
		}
		edges.add(e);
	}




	private int getMinNeighborDegree(Set<Edge> fromNode) {

		if (fromNode.size() <= 2) {
			return fromNode.size();
		}


		int minDegree = Integer.MAX_VALUE;
		for (Edge e : fromNode) {
			Set<Edge> node = this.network.get(e.to);
			if (node.size() < minDegree) {
				minDegree = node.size();
			}
		}
		return minDegree;
	}



	private Set<Edge> loadNetwork() {

		Set<Edge> ret = new HashSet<Edge>();
		List<int[]> tris = this.reader.getTriangles();
		int cccc = 0;
		for (int [] tri : tris) {
			//			if (cccc++ > 10000) {
			//				break;
			//			}
			boolean cont = false;
			for (int i = 0; i < 3; i++) {
				if (this.mapping.get(tri[i]) == null) {
					cont = true;
					break;
				}
			}
			if (cont) {
				continue;
			}
			Arrays.sort(tri);//each triangle must have a unique representation  
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

//				double dist = this.fis.get(idx1).getCoordinate().distance(this.fis.get(idx0).getCoordinate());
				Set<Edge> node = this.network.get(tri[j]);
				if (node == null) {
					node = new HashSet<Edge>();
					this.network.put(tri[j], node);
				}
				Edge e = createEdge(tri[j], tri[i]); //new Edge(,dist);
				Edge e2 =createEdge(tri[i], tri[j]);

				//				if (ret.containsKey(e2)) {
				//					Edge e3 = ret.get(e2);
				//					if (e2.to != e3.to && e2.to != e3.from) {
				//						int iii = 0;
				//						iii++;
				//						e3.hash = 0;
				//					}
				//				}

				node.add(e);
				Set<Edge> node2 = this.network.get(tri[i]);
				if (node2 == null) {
					node2 = new HashSet<Edge>();
					this.network.put(tri[i], node2);
				}
				node2.add(e2);
				j = i;
				ret.add(e2);
				Triangle triangle = new Triangle(tri);
				addInitialEdgeTriangle(e,triangle);

			}
		}
		return ret;
	}

	private Edge createEdge(int n1, int n2) {
		int idx1 = this.mapping.get(n1);
		int idx2 = this.mapping.get(n2);
		Coordinate c1 = this.fis.get(idx1).getCoordinate();
		Coordinate c2 = this.fis.get(idx2).getCoordinate();
		
		double length = c1.distance(c2); 
		return new Edge(n1,n2,length);
	}


	private static class Edge implements Comparable<Edge> {
		int from;
		int to;
		int hash;

		private final double length;

		public Edge(int n1, int n2, double length) {
			this.from = n1;
			this.to = n2;
			this.hash = (Math.min(n1,n2) + " " + Math.max(n1,n2)).hashCode();
			this.length = length;

		}

		@Override
		public int hashCode() {
			return this.hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Edge) {
				Edge other = (Edge)obj;
				return ((this.from == other.from) || this.from == other.to) && ((this.to == other.from) || this.to == other.to); 
			}
			return false;
		}

		public int compareTo(Edge o) {
			if (this.length < o.length) {
				return -1;
			} else if (o.length < this.length){
				return 1;
			}
			return 0;
		}

		@Override
		public String toString() {
			return  "from:"+ this.from + " to:" + this.to;
		}

	}

	private static class Triangle {

		private final int[] coords;
		private final int hash;

		public Triangle(int [] coords) {
			if (coords.length != 3) {
				throw new RuntimeException("A triangle consists of 3 coords!");
			}
			Arrays.sort(coords);
			this.coords = coords;
			this.hash = (coords[0] + " " + coords[1] + " " + coords[2]).hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Triangle) {
				Triangle other = (Triangle)obj;
				return this.coords[0] == other.coords[0] && this.coords[1] == other.coords[1] && this.coords[2] == other.coords[2];
			}
			return false;
		}

		@Override
		public int hashCode() {
			return this.hash;
		}


	}

	public static void main(String [] args) {
		String netcdf = MY_STATIC_STUFF.SWW_ROOT + "/" + MY_STATIC_STUFF.SWW_PREFIX + 1 + MY_STATIC_STUFF.SWW_SUFFIX;
		new TriangularMeshSimplifier(netcdf).getInundationGeometries();
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
				Polygon p = geofac.createPolygon(lr, null);
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
