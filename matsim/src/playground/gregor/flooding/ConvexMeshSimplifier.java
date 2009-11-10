package playground.gregor.flooding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;


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
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.evacuation.flooding.FloodingInfo;
import org.matsim.evacuation.flooding.FloodingReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import playground.gregor.MY_STATIC_STUFF;

public class ConvexMeshSimplifier {


	private static final double PI_HALF = Math.PI /2;


	private static final double TWO_PI =  2 * Math.PI;


	private static final boolean DEBUG = true;
	List<List<Integer>> geos = new ArrayList<List<Integer>>();
	private final boolean run  = true;


	private final FloodingReader reader;


	private List<FloodingInfo> fis;

	private final Set<Edge> removed = new HashSet<Edge>();
	private final Set<Edge> marked = new HashSet<Edge>();
	private final Set<Edge> warn = new HashSet<Edge>();

	private Map<Integer, Integer> mapping;

	private Map<Coordinate, Integer> coordKeyMap;
	private QuadTree<Coordinate> coordQuadTree;

	private QuadTree<Polygon> polygons;
	
	private final Map<Integer, Set<Edge>> network = new HashMap<Integer, Set<Edge>>();
	//	private final Map<Integer>


	private final GeometryFactory geofac = new GeometryFactory();

	private FeatureType ft;


	private FeatureType ftLine;

	public ConvexMeshSimplifier(String netcdf) {
		this.reader = new FloodingReader(netcdf);
		this.reader.setReadTriangles(true);
		this.reader.setReadFloodingSeries(true);
		double offsetEast = 632968.461027224;
		double offsetNorth = 9880201.726;
		this.reader.setOffset(offsetEast, offsetNorth);
	}

	private void run() {
		this.fis = this.reader.getFloodingInfos();

		this.mapping = this.reader.getIdxMapping();
		//		List<List<Integer>> geos = new ArrayList<List<Integer>>();
		//		for (int [] tri : this.reader.getTriangles()) {
		//			List<Integer> geo = new ArrayList<Integer>();
		//			boolean add = true;
		//			for (int i = 0; i < 3; i++) {
		//				if (this.mapping.get(tri[i]) == null) {
		//					add = false;
		//					break;
		//				}
		//				geo.add(tri[i]);
		//			}
		//			geo.add(tri[0]);
		//			if (add){
		//				geos.add(geo);
		//			}
		//		}
		//		dump2(geos);
		//		if (true) {
		//			return;
		//		}
		Queue<Edge> edges = loadNetwork();
		loadCoordMapping();

//		Queue<Edge> edges = new ConcurrentLinkedQueue<Edge>();
		Edge e1 = getE1();
		edges.add(e1);
		Stack<Edge> stack = new Stack<Edge>();
		stack.addAll(edges);
		edges.clear();
		while (!stack.isEmpty()) {
			if (stack.size() % 1000 == 0) {
				System.out.println(stack.size());
			}
			
//			if (this.geos.size() == 100) {
//				this.run = false;
//			}
			if (!this.run) {
				break;
			}
			
//			System.out.println("removed.size: " + this.removed.size());
			if (this.removed.contains(stack.peek())) {
				stack.pop();
			} else {
				runEdges(stack);
			}
		}

		dump(this.geos,"../../tmp/geometries.shp",true);
		if (DEBUG) {
			List<List<Integer>> geos = new ArrayList<List<Integer>>();
			for (Set<Edge> es : this.network.values()) {
				for (Edge e : es) {
					List<Integer> geo = new ArrayList<Integer>();
					geo.add(e.from);
					geo.add(e.to);
					geos.add(geo);
				}
			}

			dump(geos,"../../tmp/edges.shp",false);
		}
	}

	private void loadCoordMapping() {
		this.coordKeyMap = new HashMap<Coordinate, Integer>();
		Envelope e = this.reader.getEnvelope();
		this.coordQuadTree = new QuadTree<Coordinate>(e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY());
		this.polygons = new QuadTree<Polygon>(e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY());
		for (Entry<Integer, Integer> entry : this.mapping.entrySet()) {
			Coordinate c = this.fis.get(entry.getValue()).getCoordinate();
			this.coordKeyMap.put(c, entry.getKey());
			this.coordQuadTree.put(c.x, c.y, c);
		}

	}

	private void runEdges(Stack<Edge> stack) {
//		Edge e = edges.poll();
		Edge e = stack.pop();
		Edge e0 = e;
		Stack<Integer> coords = new Stack<Integer>();
		Set<Integer> fwdH = new HashSet<Integer>();

		Stack<Integer> coordsReverse = new Stack<Integer>();
		Set<Integer> rwdH = new HashSet<Integer>();

		Integer coord = e.to;
		coords.push(coord);
		fwdH.add(coord);
		Integer coordReverse = e.from;
		coordsReverse.push(coordReverse);
		rwdH.add(coordReverse);
		Edge rev = new Edge(e.to,e.from);
		
		List<Integer> nn = new ArrayList<Integer>();
		nn.add(e.from);
		nn.add(e.to);

		boolean runForward = true;
		boolean runBackward = true;
		boolean success = false;
		
//		System.out.println(this.geos.size());
		
		while (runForward || runBackward) {

			if (runForward) {
				e = getNextEdgeConvex(e,true);
				if (e == null) {
					break;
				}
				coords.push(e.to);
			}

			if (runBackward) {
				rev = getNextEdgeConvex(rev,false);
				if (rev == null) {
					break;
				}
			
				coordsReverse.push(rev.to);
			}

			if (e.to == coordsReverse.get(0)) {
//				System.out.println("valid forward circle size:" + coords.size());
				coords.push(coords.firstElement());
				success = true;
				break;
			} else if (runForward && fwdH.contains(e.to)) {
//				System.out.println("invalid forward circle");
				runForward = false;
			}

			if (fwdH.contains(rev.to)) {
//				System.out.println("backward match");
				Stack<Integer> tmp = coords;
				coords.clear();
					
//				handleForwardMatch(coords,coordsReverse,false);
//				coords = coordsReverse;
//				success = true;
				break;
			}
			if (rwdH.contains(e.to)) {
//				if (this.geos.size() == 81) {
//					this.run = false;
//					this.geos.clear();
//					this.geos.add(coords);
//					this.geos.add(coordsReverse);
////					this.geos.add(nn);
//					break;
//				}
//				System.out.println("forward match");
				success = handleForwardMatch(coords,coordsReverse,true);
//				success = true;
				break;
			}

			if (fwdH.contains(e.to)) {
				break;
			}
			if (rwdH.contains(rev.to)) {
				break;
			}
			
			if (rev.to == coords.get(0)) {
//				System.out.println("valid backward circle  size:" + coordsReverse.size());
				coordsReverse.push(coordsReverse.firstElement());
				coords.clear();
				while (!coordsReverse.isEmpty()) {
					coords.push(coordsReverse.pop());
				}
				success = true;
				break;
			} else if (runBackward && rwdH.contains(rev.to)) {
//				System.out.println("invalid backward circle!");
				runBackward = false;
			}

			if (runForward) {
				fwdH.add(e.to);
			}
			if (runBackward) {
				rwdH.add(rev.to);
			}


		}
		if (!success) {
			if (!this.warn.contains(e0)) {
				this.warn.add(e0);
				stack.add(0, e0);
			}
			return;
		}
		
		fwdH.clear();
		fwdH.addAll(coords);
		if (cleanUp(coords,fwdH)) {
//		Iterator<Integer> it = coords.iterator();
//		int e1 = it.next();
//		while (it.hasNext()) {
//			int e2 = it.next();
//			Edge edge = new Edge(e2,e1);
//			edges.add(edge);
//			e1 = e2;
//		}
		this.geos.add(coords);
		System.out.println("Geos: " + this.geos.size() + " current size:" + coords.size());
		}
		//		geos.add(coordsReverse);
		//		geos.add(nn);
		


	}

	private boolean cleanUp(Stack<Integer> coords, Set<Integer> fwdH) {

		Coordinate [] shell = new Coordinate[coords.size()];
		int pos = 0;

		Envelope e = new Envelope();
		for (Integer i : coords) {
			int idx = this.mapping.get(i);
			Coordinate c = this.fis.get(idx).getCoordinate();
			shell[pos++] = c;
			e.expandToInclude(c.x, c.y);
		}
		LinearRing lr = this.geofac.createLinearRing(shell);
		Polygon p = this.geofac.createPolygon(lr, null);
		Polygon ppp = this.polygons.get(p.getCentroid().getX(), p.getCentroid().getY());

		if (ppp != null && p.equals(ppp)) {
			return false;
		}
		this.polygons.put(p.getCentroid().getX(), p.getCentroid().getY(), p);
		
		Collection<Coordinate> values = new ArrayList<Coordinate>();
		this.coordQuadTree.get(e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY(), values);
		for (Coordinate c : values) {
			Point pp = this.geofac.createPoint(c);
			if (p.contains(pp)) {
				int idx = this.coordKeyMap.get(c);
				this.mapping.remove(idx);
				Set<Edge> s = this.network.get(idx);
				if (s == null) {
					this.network.remove(idx);
					continue;
				}
				
				for (Edge edge : s) {
					this.removed.add(edge);
					Set<Edge> tmp  = this.network.get(edge.to);
					if (tmp != null) {
						Edge tmpE = new Edge(edge.to,edge.from);

						tmp.remove(tmpE);
						this.removed.add(tmpE);
					}
				}
				
				this.network.remove(idx);
			}
		}
		
		
		//walk around the shell and remove edges cutting geometry
		for (int i = 1; i < coords.size(); i++) {
			Set<Edge> s = this.network.get(coords.get(i));
			Stack<Edge> rm = new Stack<Edge>();
			int j = i + 1;
			if (j >= coords.size()-1) {
				j = 0;
			}
			for (Edge edge : s) {
				if ((edge.to != coords.get(i-1) && edge.to != coords.get(j)) && fwdH.contains(edge.to)) {
					rm.add(edge);
					this.removed.add(edge);
				}
			}
			while (!rm.isEmpty()) {
				s.remove(rm.pop());
			}
		}
		for (int i = 1; i < coords.size(); i++) {
			Edge ee = new Edge(coords.get(i-1),coords.get(i));
			if (this.marked.contains(ee)) {
				this.removed.add(ee);
			} else {
				this.marked.add(ee);
			}
		}

		return true;
	}

	private boolean handleForwardMatch(Stack<Integer> coords,
			Stack<Integer> coordsReverse, boolean turnRight) {
		int fwdTop = coords.peek();
		while (coordsReverse.peek()  != fwdTop) {
			coordsReverse.pop();
		}

		double gamma = 0;
		coords.pop();
		coordsReverse.pop();
		Edge e1 = new Edge(coords.peek(),fwdTop);
		Edge e2 = new Edge(fwdTop,coordsReverse.peek());
		gamma = getAngle(e1,e2,turnRight);
		boolean addEdge = false;
		while (gamma <=0 || gamma > Math.PI) {
			//DEBUG
			if (coordsReverse.size() == 1) {
				return false;
			}
			coordsReverse.pop();
			e1 = new Edge(coords.peek(),fwdTop);
			e2 = new Edge(fwdTop,coordsReverse.peek());
			gamma = getAngle(e1,e2,turnRight);
			addEdge = true;
			

			
		}
		coords.push(fwdTop);
		if (addEdge) {
			addEdge(fwdTop,coordsReverse.peek());
		}
		while (!coordsReverse.isEmpty()) {
			coords.push(coordsReverse.pop());
		}
		coords.push(coords.firstElement());
		return true;
	}

	private void addEdge(int from, int to) {
		if (from == to) {
			System.err.println("from == to");
			return;
		}
		Set<Edge> node = this.network.get(from);
		if (node == null) {
			node = new HashSet<Edge>();
			this.network.put(from, node);
		}
		Edge e = new Edge(from, to);
		node.add(e);

		Set<Edge> node2 = this.network.get(to);
		if (node2 == null) {
			node2 = new HashSet<Edge>();
			this.network.put(to, node2);
		}
		Edge e2 =new Edge(to, from);
		node2.add(e2);
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

	private Edge getNextEdgeConvex(Edge e, boolean turnRight) {
		Set<Edge> fwd = this.network.get(e.to);
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



	private Queue<Edge> loadNetwork() {
		Queue<Edge> edges = new ConcurrentLinkedQueue<Edge>();
		
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
//				addEdge(idx0, idx1);
				Set<Edge> node = this.network.get(tri[j]);
				if (node == null) {
					node = new HashSet<Edge>();
					this.network.put(tri[j], node);
				}
				Edge e = new Edge(tri[j], tri[i]);
				edges.add(e);
				Edge e2 =new Edge(tri[i], tri[j]);
				edges.add(e2);
				node.add(e);
				Set<Edge> node2 = this.network.get(tri[i]);
				if (node2 == null) {
					node2 = new HashSet<Edge>();
					this.network.put(tri[i], node2);
				}
				node2.add(e2);
				j = i;
				//				Set<Edge> nodeReverse = this.networkReverse.get(tri[i]);
				//				if (nodeReverse == null) {
				//					nodeReverse = new HashSet<Edge>();
				//					this.networkReverse.put(tri[i], nodeReverse);
				//				}
				//				nodeReverse.add(e);
			}
		}
		return edges;
	}


	private static class Edge {
		int from;
		int to;
		int hash;

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
		String netcdf = MY_STATIC_STUFF.SWW_ROOT + "/" + MY_STATIC_STUFF.SWW_PREFIX + 2 + MY_STATIC_STUFF.SWW_SUFFIX;
		new ConvexMeshSimplifier(netcdf).run();

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
