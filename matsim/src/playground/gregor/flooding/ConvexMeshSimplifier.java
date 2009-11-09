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

public class ConvexMeshSimplifier {


	private static final double PI_HALF = Math.PI /2;


	private static final double TWO_PI =  2 * Math.PI;


	private final FloodingReader reader;


	private List<FloodingInfo> fis;


	private Map<Integer, Integer> mapping;

	private final Map<Integer, Set<Edge>> network = new HashMap<Integer, Set<Edge>>();
	private final Map<Integer, Set<Edge>> networkReverse = new HashMap<Integer, Set<Edge>>();


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
		loadNetwork();
		
		Queue<Edge> edges = new ConcurrentLinkedQueue<Edge>();
		Edge e1 = getE1();
		edges.add(e1);
		while (edges.size() > 0) {
			runEdges(edges);
		}

		
		dump();
	}

	private void runEdges(Queue<Edge> edges) {
		Edge e = edges.poll();
		List<Integer> coords = new ArrayList<Integer>();
		Set<Integer> fwdH = new HashSet<Integer>();
		
		List<Integer> coordsReverse = new ArrayList<Integer>();
		Set<Integer> rwdH = new HashSet<Integer>();
		
		Integer coord = e.to;
		coords.add(coord);
		Integer coordReverse = e.from;
		coordsReverse.add(coordReverse);
		Edge rev = new Edge(e.to,e.from);
		List<List<Integer>> geos = new ArrayList<List<Integer>>();
		List<Integer> nn = new ArrayList<Integer>();
		nn.add(e.from);
		nn.add(e.to);
		
		boolean runForward = true;
		boolean runBackward = true;
		
		while (runForward || runBackward) {
			
			if (runForward) {
				e = getForwardEdge(e);
				coords.add(e.to);
			}
			
			if (runBackward) {
				rev = getBackWardEdge(rev);
				coordsReverse.add(rev.to);
			}
	
			if (e.to == coords.get(0)) {
				System.out.println("valid forward circle size:" + coords.size());
				break;
			} else if (runForward && fwdH.contains(e.to)) {
				System.out.println("invalid forward circle");
				runForward = false;
			}
			
			if (fwdH.contains(rev.to)) {
				System.out.println("backward match");
				break;
			}
			if (rwdH.contains(e.to)) {
				System.out.println("forward match");
				break;
			}
			
			if (rev.to == coordsReverse.get(0)) {
				System.out.println("valid backward circle  size:" + coordsReverse.size());
				break;
			} else if (runBackward && rwdH.contains(rev.to)) {
					System.out.println("invalid backward circle!");
					runBackward = false;
			}
			
			if (runForward) {
				fwdH.add(e.to);
			}
			if (runBackward) {
				rwdH.add(rev.to);
			}


		}
		
		geos.add(coords);
		geos.add(coordsReverse);
		geos.add(nn);
		dump2(geos);
		
		
	}

	private Edge getForwardEdge(Edge e) {
		Set<Edge> fwd = this.network.get(e.to);
		Integer idx = this.mapping.get(e.to);
		Coordinate zero = this.fis.get(idx).getCoordinate();
		idx = this.mapping.get(e.from);
		Coordinate c0 = this.fis.get(idx).getCoordinate();
		Coordinate v0 = new Coordinate(c0.x-zero.x,c0.y-zero.y);
		double alpha = getPhaseAngle(v0);
		
		
		
		double maxGamma = 0;
		Edge bestEdge = null;
		for (Edge tmp : fwd) {
			idx = this.mapping.get(tmp.to);
			Coordinate c1 = this.fis.get(idx).getCoordinate();
			Coordinate v1 = new Coordinate(c1.x-zero.x,c1.y-zero.y);
			double beta = getPhaseAngle(v1);
			double gamma = beta -alpha;
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

	private Edge getBackWardEdge(Edge e) {
		Set<Edge> fwd = this.network.get(e.to);
		Integer idx = this.mapping.get(e.to);
		Coordinate zero = this.fis.get(idx).getCoordinate();
		idx = this.mapping.get(e.from);
		Coordinate c0 = this.fis.get(idx).getCoordinate();
		Coordinate v0 = new Coordinate(c0.x-zero.x,c0.y-zero.y);
		double alpha = getPhaseAngle(v0);
		
		
		
		double maxGamma = 0;
		Edge bestEdge = null;
		for (Edge tmp : fwd) {
			idx = this.mapping.get(tmp.to);
			Coordinate c1 = this.fis.get(idx).getCoordinate();
			Coordinate v1 = new Coordinate(c1.x-zero.x,c1.y-zero.y);
			double beta = getPhaseAngle(v1);
			double gamma = alpha - beta;
			gamma = gamma < 0 ? gamma + TWO_PI : gamma;
			if (gamma <= Math.PI && gamma > maxGamma) {
				maxGamma = gamma;
				bestEdge = tmp;
			}
		}
		
		return bestEdge;
	}

	private Edge getE1() {
		Set<Edge> edges = null;
		int i = 12000; //(int)(Math.random() * 1000);
		while(edges == null) {
			edges = this.network.get(i++);
		}

		return edges.iterator().next();
	}

	private void dump2(List<List<Integer>> geos) {
		initFeatures();
		GeometryFactory geofac = new GeometryFactory();
		List<Feature> fts = new ArrayList<Feature>();
		double ii = 0;
		for (List<Integer> geo : geos) {
			Coordinate [] coords = new Coordinate[geo.size()];
			int j = 0;
			for (Integer i : geo) {
				Integer idx = this.mapping.get(i);
				FloodingInfo fi = this.fis.get(idx);
				
				coords[j++] = fi.getCoordinate();
				
			}
			LineString ls = geofac.createLineString(coords);
			
			try {
				fts.add(this.ftLine.create(new Object[]{ls,ii++,0.}));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
		}
		try {
			ShapeFileWriter.writeGeometries(fts,"../../tmp/lineStrings.shp");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void dump() {
		Set<Edge> es = this.network.get(500);
		Edge e = es.iterator().next();
		List<FloodingInfo> geo = new ArrayList<FloodingInfo>();
		for (int j = 0; j < 5; j++) {
			Integer idx = this.mapping.get(e.from);
			FloodingInfo fi = this.fis.get(idx);
			geo.add(fi);
			Iterator<Edge> it = this.network.get(e.to).iterator();
			e = it.next();
			if (it.hasNext()) {
				e = it.next();
			}
		}
		List<List<FloodingInfo> > geos = new ArrayList<List<FloodingInfo>>();
		geos.add(geo);
		new Dump(geos);		
	}

	private void loadNetwork() {
		List<int[]> tris = this.reader.getTriangles();
		for (int [] tri : tris) {
			int j = 2;
			for (int i = 0; i < 3; i++) {
			
				
				Integer idx0  = this.mapping.get(tri[j]);
				Integer idx1  = this.mapping.get(tri[i]);
				if (idx0 == null || idx1 == null) {
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
//				Set<Edge> nodeReverse = this.networkReverse.get(tri[i]);
//				if (nodeReverse == null) {
//					nodeReverse = new HashSet<Edge>();
//					this.networkReverse.put(tri[i], nodeReverse);
//				}
//				nodeReverse.add(e);
			}
		}
		
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
