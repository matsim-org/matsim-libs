package playground.gregor.flooding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.evacuation.flooding.FloodingInfo;
import org.matsim.evacuation.flooding.FloodingReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.MY_STATIC_STUFF;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class TriangularMeshSimplifier {


	private static final Logger log = Logger.getLogger(TriangularMeshSimplifier.class);

	private static final double MAX_LENGTH = 500;

	private static final double TWO_PI = 2 * Math.PI;
	private static final double PI_HALF =  Math.PI / 2;

	private static final double MIN_LENGTH = 2;

	private final List<List<FloodingInfo>> geos = new ArrayList<List<FloodingInfo>>();

	boolean initialized = false;

	private final String netcdf;

	private FloodingReader reader;

	private Map<Integer, Integer> mapping;

	private final Map<Integer, Set<Edge>> network = new HashMap<Integer, Set<Edge>>();

	private final Map<Edge,Set<Triangle>> edgeTriangleMapping = new HashMap<Edge,Set<Triangle>>();
	private final Map<Triangle,Set<Edge>> triangleEdgeMapping = new HashMap<Triangle,Set<Edge>>();

	private List<FloodingInfo> fis;

	private final Set<Edge> removed = new HashSet<Edge>();

	private FeatureType ft;

	private FeatureType ftLine;

	private Queue<Edge> queue;


	private final Set<Edge> cleanUps = new HashSet<Edge>();

	private final Map<Triangle,Set<Edge>> saveTriangleEdgeMapping = new HashMap<Triangle,Set<Edge>>();
	private final Map<Edge,Set<Triangle>> saveEdgeTriangleMapping = new HashMap<Edge,Set<Triangle>>();
	private final Map<Integer, Set<Edge>> saveNetwork = new HashMap<Integer, Set<Edge>>();
	private final Set<Triangle> newTriangles = new HashSet<Triangle>();
	private final Set<Triangle> oldTriangles = new HashSet<Triangle>();
	private final Set<Edge> newEdges = new HashSet<Edge>();

	private final Set<Edge> touched  = new HashSet<Edge>();


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
			throw new RuntimeException(e1);
		}
		this.queue = new PriorityQueue<Edge>();
		this.queue.addAll(edges);

		checkEdgeConsistency();


		log.info("start!");
		for (int i = 0; i < 10; i++) {
			runIteration();
			this.queue.addAll(this.edgeTriangleMapping.keySet());
			this.removed.clear();
		}


//		dumpGeos();

		this.initialized = true;
	}


	private void runIteration() {
		int count = 0;
		while (this.queue.size() > 0) {
			if (count++ % 10000 == 0) {
				log.info("count: " + count);
			}
			Edge e = this.queue.poll();
			if (!checkEdgeConsistency(e)){
				continue;
			}
			if (this.removed.contains(e) || this.network.get(e.to) == null || this.network.get(e.from) == null ) {
				continue;
			}
			
			int depth = calcDepth(e);
			double maxLength = MIN_LENGTH * Math.pow(2, depth);

			
			

			if (e.length > maxLength) {
				continue;
			}

			contractEdge(e);
			deepClean(this.cleanUps);
			this.cleanUps.clear();
		}


	}



	private int calcDepth(Edge e) {
		if(this.edgeTriangleMapping.get(e).size() < 2) {
			return 1;
		} 

		
		Set<Edge> s1 = new HashSet<Edge>();
		s1.add(e);
		s1.addAll(this.network.get(e.from));
		s1.addAll(this.network.get(e.to));
		List<Integer> fib = new ArrayList<Integer>();
//		fib.add(1);
		fib.add(2);
		fib.add(3);
		fib.add(5);
		fib.add(8);
		fib.add(13);
		
		for (int i : fib) {
			if (getMinNeighbors(s1) == 1){
				return i;			
			}
			Set<Edge> ss1 = new HashSet<Edge>();
			for (Edge ee : s1) {
				ss1.addAll(this.network.get(ee.to));
				
			}
			s1.addAll(ss1);
		}
		return 21;
	}



	private int getMinNeighbors(Set<Edge> s1) {
		for (Edge e : s1) {
			for (Edge ee : this.network.get(e.to)) {
				if (s1.contains(ee)) {
					continue;
				}
				if (this.edgeTriangleMapping.get(ee).size() < 2) {
					return 1;
				}
			}
		}
		return 2;
	}



	private void deepClean(Collection<Edge> edges) {


		Stack<Edge> current = new Stack<Edge>();
		for (int i = 1; i <= 50; i++) {
			current.addAll(edges);
			Collections.shuffle(current);
			while (!current.isEmpty()) {
				cleanUpEdge(current.pop(), 1./(10.*i));
				this.oldTriangles.clear();
				this.newEdges.clear();
				this.newTriangles.clear();
				this.saveEdgeTriangleMapping.clear();
				this.saveTriangleEdgeMapping.clear();
				this.saveNetwork.clear();
				this.oldTriangles.clear();
			}
			current.addAll(this.touched);
			this.touched.clear();
		}

	}



	private void checkEdgeConsistency() {
		for (Set<Triangle> set : this.edgeTriangleMapping.values()) {
			if(set.size() > 2) {
				throw new RuntimeException("At least one edge is associated with more then two triangles!");
			}
		}

	}

	private boolean checkEdgesConsistency(Set<Edge> edges) {

		for (Edge edge : edges) {
			if (!checkEdgeConsistency(edge)) {
				return false;
			}
		}

		return true;
	}

	private boolean checkEdgeConsistency(Edge edge) {
		Set<Triangle> set = this.edgeTriangleMapping.get(edge);
		if (set == null) {
			return false;
		}

		if(set.size() > 2) {
			return false;
		}
		for (Triangle tri : set) {
			if (!this.triangleEdgeMapping.get(tri).contains(edge)) {
				throw new RuntimeException("no reverse mapping!");
			}
		}

		Set<Edge> s1 = this.network.get(edge.from);
		Set<Edge> s2 = this.network.get(edge.to);
		Set<Integer> nodes = new HashSet<Integer>();
		for (Edge e1 : s1) {
			nodes.add(e1.to);
		}
		int neighbors = 0;
		for (Edge e2 : s2) {
			if (nodes.contains(e2.to)) {
				neighbors++;
			}
		}
		if (neighbors > 2 || neighbors == 0) {
			return false;
		}
		return true;
	}

	private void cleanUpEdge(Edge pop, double crit) {

		if (this.removed.contains(pop)) {
			return;
		}
		checkEdgeConsistency(pop);

		Set<Triangle> tris = this.edgeTriangleMapping.get(pop);
		if (tris == null) {
			return;
		}

		if (tris.size() != 2) {
			return;
		}

		List<Triangle> tl = new ArrayList<Triangle>(tris);
		Triangle tri1 = tl.get(0);
		Triangle tri2 = tl.get(1);
		double norm = Double.POSITIVE_INFINITY;
		for (Triangle tri : tris) {
			norm = Math.min(norm,getMinAngle(tri.coords));
		}


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

		if (!isUnionConvex(coords1,coords2)) {
			return;
		}

		double norm2 = getMinAngle(coords1);
		norm2 = Math.min(norm2,getMinAngle(coords2));

		if (MatsimRandom.getRandom().nextDouble() < crit) {
			if (MatsimRandom.getRandom().nextBoolean()){
				norm = 0;
			} else {
				norm2 = 0;
			}
		}
		if (norm2 > norm ) {
			saveState(pop);
			Edge newEdge = createEdge(coords1[1],coords1[2]);

			Edge newEdge2 = createEdge(coords1[2],coords1[1]);
			this.network.get(coords1[1]).add(newEdge);
			this.network.get(coords1[2]).add(newEdge2);
			this.queue.add(newEdge);
			removeEdge(pop);
			if (!createTriangles(newEdge)){
				undoChanges();
			}else if (!checkEdgeConsistency(newEdge)) {
				undoChanges();
			}
		}
	}



	private boolean isUnionConvex(int[] coords1, int[] coords2) {
		int [] coords = {coords1[0],coords1[1],coords2[0],coords1[2],coords1[0]};
		int over = 0;
		int under = 0;
		boolean turnRight = true;
		for (int i= 0; i < 3; i++) {
			Edge e1 = createEdge(coords[i], coords[i+1]);
			Edge e2 = createEdge(coords[i+1], coords[i+2]);
			double alpha = getAngle(e1,e2,turnRight);
			if (alpha > Math.PI) {
				over++;
			} else {
				under++;
			}
		}
		Edge e1 = createEdge(coords[3], coords[0]);
		Edge e2 = createEdge(coords[0], coords[1]);
		double alpha = getAngle(e1,e2,turnRight);
		if (alpha > Math.PI) {
			over++;
		} else {
			under++;
		}
		if (Math.min(over,under) > 0) {
			return false;
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





	private double getMinAngle(int[] coords1) {
		Edge e0 = createEdge(coords1[0], coords1[1]);
		Edge e1 = createEdge(coords1[1], coords1[2]);
		Edge e2 = createEdge(coords1[2], coords1[0]);
		boolean turnRight = true;
		double alpha = getAngle(e0, e1, turnRight);
		if (alpha > Math.PI) {
			turnRight = false;
			alpha -= Math.PI;
		}
		alpha = Math.min(alpha, getAngle(e1,e2,turnRight));
		alpha = Math.min(alpha, getAngle(e2,e0,turnRight));

		return alpha;

		//		double avgLength = e0.length + e1.length + e2.length;
		//		avgLength /= 3;
		//
		//		double norm = Math.pow(e0.length-avgLength, 2) + Math.pow(e1.length-avgLength, 2) + Math.pow(e2.length-avgLength, 2);
		//		norm /= 3;
		//		norm = Math.sqrt(norm) / avgLength;
		//
		//		return norm;
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
	}

	private void contractEdge(Edge e) {


		Set<Edge> fromNode = this.network.get(e.from);
		Set<Edge> toNode = this.network.get(e.to);

		if (this.edgeTriangleMapping.get(e).size() < 2) {
			return;
		}

		int degFrom = getMinNeighborDegree(fromNode);
		int degTo = getMinNeighborDegree(toNode);

//		if (degFrom <=1 && degTo <= 1) {
//			return;
//		}
//		//
//		if (degFrom == 0 || degTo == 0) {
//			return;
//		}

		saveState(e);
		//	
		boolean ret = false;
		if (degFrom <=1) {
			ret = contractEdge(e,toNode, fromNode,e.from, e.to);
		} else if (degTo <= 1) {
			ret = contractEdge(e,fromNode,toNode,e.to, e.from);
		} else if (degFrom > degTo) {
			ret = contractEdge(e,fromNode,toNode,e.to, e.from);	
		} else {
			ret = contractEdge(e,toNode, fromNode,e.from, e.to);	
		}

		if(!ret) {
			undoChanges();
		} else if (!checkEdgesConsistency(this.touched)){
			log.error("inconsistence!");
			undoChanges();
		}
		this.newEdges.clear();
		this.newTriangles.clear();
		this.saveEdgeTriangleMapping.clear();
		this.saveTriangleEdgeMapping.clear();
		this.saveNetwork.clear();
		this.oldTriangles.clear();
		this.touched.clear();
	}



	private void undoChanges() {



		for (Edge edge : this.newEdges) {
			this.removed.add(edge);
			this.edgeTriangleMapping.remove(edge);
			this.network.get(edge.from).remove(edge);
			this.network.get(edge.to).remove(edge);
		}
		for (Triangle tri : this.newTriangles) {
			Set<Edge> tmp = this.triangleEdgeMapping.remove(tri);
			for (Edge e : tmp) {
				if (this.edgeTriangleMapping.get(e) != null) {
					this.edgeTriangleMapping.remove(e);
				}
			}
		}

		for (Entry<Edge,Set<Triangle>> entry : this.saveEdgeTriangleMapping.entrySet()) {
			this.edgeTriangleMapping.put(entry.getKey(),entry.getValue());
		}
		for (Entry<Triangle,Set<Edge>> entry : this.saveTriangleEdgeMapping.entrySet()) {
			this.triangleEdgeMapping.put(entry.getKey(), entry.getValue());
		}

		for (Entry<Integer,Set<Edge>> entry : this.saveNetwork.entrySet()) {
			this.network.put(entry.getKey(), entry.getValue());

		}
		//
		//
		//		for (Edge e: this.saveEdgeTriangleMapping.keySet()) {
		//			checkEdgeConsistency(e);
		//		}
		//		
		this.cleanUps.clear();




	}



	private void saveState(Edge e) {
		List<Edge> edges = new ArrayList<Edge>();

		edges.addAll(getAllAssociatedEdges(this.network.get(e.from)));
		edges.addAll(getAllAssociatedEdges(this.network.get(e.to)));
		for (Edge ee : edges) {

			Set<Edge> s1 = this.network.get(ee.from);
			Set<Edge> s1n = new HashSet<Edge>();
			s1n.addAll(s1);
			this.saveNetwork.put(ee.from, s1n);


			Set<Triangle> s2 = this.edgeTriangleMapping.get(ee);
			Set<Triangle> s2n = new HashSet<Triangle>();
			s2n.addAll(s2);
			this.saveEdgeTriangleMapping.put(ee, s2n);


			Set<Triangle> tris  = this.edgeTriangleMapping.get(ee);
			for (Triangle tri : tris) {
				Set<Edge> s3 = this.triangleEdgeMapping.get(tri);
				Set<Edge> s3n = new HashSet<Edge>();
				s3n.addAll(s3);
				this.saveTriangleEdgeMapping.put(tri,s3n);
			}
		}
	}



	private List<Edge> getAllAssociatedEdges(Collection<Edge> set) {
		List<Edge> ret = new ArrayList<Edge>();
		for (Edge e : set) {
			ret.add(e);
			Set<Edge> s = this.network.get(e.to);
			for (Edge ee : s) {
				ret.add(ee);
			}
			Set<Edge> s2 = this.network.get(e.from);
			for (Edge ee : s2) {
				ret.add(ee);
			}

		}
		return ret;
	}



	private boolean contractEdge(Edge e, Set<Edge> delete, Set<Edge> keep,int keepId, int rmId) {
		List<Edge> genEdges = new ArrayList<Edge>();
		Set<Edge> rms = new HashSet<Edge>();
		for (Edge del : delete) {
			rms.add(del);
			if (del.to == keepId) {
				continue;
			}
			Edge e1 = createEdge(del.to, keepId);
			this.newEdges.add(e1);
			genEdges.add(e1);
		}
		for (Edge rm : rms) {
			removeEdge(rm);
		}

		for (Edge edge : genEdges) {
			this.network.get(edge.from).add(edge);
			Edge e2 = createEdge(edge.to, edge.from);
			this.network.get(e2.from).add(e2);
		}


		boolean b = true;
		for (Edge edge : genEdges) {
			if (!testCreateTriangles(edge)){
				return false;
			}

		}

		if (b) {
			for (Edge edge : genEdges) {
				if (!createTriangles(edge)){
					return false;
				}

				this.queue.add(edge);
				this.cleanUps.add(edge);
			} 
		} 

		return true;

	}


	private boolean testCreateTriangles(Edge e) {
		Set<Edge> s1 = this.network.get(e.from);
		Set<Edge> s2 = this.network.get(e.to);
		Set<Integer> tmp = new HashSet<Integer>();
		Set<Integer> nodes = new HashSet<Integer>();
		for (Edge edge : s1) {
			tmp.add(edge.to);
		}
		for (Edge edge : s2) {
			if (tmp.contains(edge.to)) {
				nodes.add(edge.to);
			}
		}
		if (nodes.size() == 0) {
			//			throw new RuntimeException("Trying to add a non connected edge!");
			return false;
		} else if (nodes.size() > 2) {
			return false;
		}
		return true;
	}



	private boolean createTriangles(Edge e) {
		Set<Edge> s1 = this.network.get(e.from);
		Set<Edge> s2 = this.network.get(e.to);
		Set<Integer> tmp = new HashSet<Integer>();
		Set<Integer> nodes = new HashSet<Integer>();
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
			return false;
		} else if (nodes.size() > 2) {
			return false;
		}



		for (int i : nodes) {
			if(!addEdgeTriangle(s1,s2,i,e)){
				return false;
			}
		}
		return true;


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
			throw new RuntimeException("Edge was not associated with any triangle!");
		}
		if (tris.size() > 2) {
			throw new RuntimeException("Edge is associated with more then 2 triangles!");
		}
		for (Triangle tri : tris) {
			this.oldTriangles.add(tri);
			Set<Edge> edges = this.triangleEdgeMapping.remove(tri);
			for (Edge edge : edges) {
				if (edge.equals(e)) {
					continue;
				}
				this.cleanUps.add(edge);

				if (this.edgeTriangleMapping.get(edge) != null) {
					this.edgeTriangleMapping.get(edge).remove(tri);
				}
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




	private boolean addEdgeTriangle(Set<Edge>s1, Set<Edge> s2, int node,Edge e){
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
		this.newTriangles.add(tri1);

		this.triangleEdgeMapping.put(tri1, edges);

		for (Edge edge : edges) {

			Set<Triangle> tris = this.edgeTriangleMapping.get(edge);
			if (tris == null){
				tris = new HashSet<Triangle>();
				this.edgeTriangleMapping.put(edge, tris);
			}
			this.touched.add(edge);
			tris.add(tri1);
			if(!checkEdgeConsistency(edge)){
				return false;
			}
		}
		return true;
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

		for (Edge e : fromNode) {
			if (this.edgeTriangleMapping.get(e).size() <= 1){
				return 1;
			}
		}
		return 2;
	}



	private Set<Edge> loadNetwork() {

		Set<Edge> ret = new HashSet<Edge>();
		List<int[]> tris = this.reader.getTriangles();
		for (int [] tri : tris) {
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

				Set<Edge> node = this.network.get(tri[j]);
				if (node == null) {
					node = new HashSet<Edge>();
					this.network.put(tri[j], node);
				}
				Edge e = createEdge(tri[j], tri[i]); //new Edge(,dist);
				Edge e2 =createEdge(tri[i], tri[j]);


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

		@Override
		public String toString() {
			return (this.coords[0] + " " + this.coords[1] + " " + this.coords[2]);
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
