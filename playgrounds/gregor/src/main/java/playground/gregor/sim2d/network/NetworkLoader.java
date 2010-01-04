package playground.gregor.sim2d.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

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
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilder;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class NetworkLoader {

	private static final Logger log = Logger.getLogger(NetworkLoader.class);
	
	private static final double RANGE = 0.45;

	private static final double OVER_LENGTH = 2.;
	private static final double OVER_LENGTHII = 1./600;

	private static final boolean DEBUG = true;

	String shapefile = "../../../../sim2d/sg4model.shp";
	boolean initialized = false;
	private Envelope env;
	private FeatureSource fs;

	private final Set<Geometry> geos = new HashSet<Geometry>();

	private final List<LineString> visibilityGraph = new ArrayList<LineString>();

	private FeatureType ft;

	private FeatureType ftLine;

	private FeatureType ftPoint;

	private List<Coordinate> nodes;

	GeometryFactory geofac = new GeometryFactory();

	private final NetworkLayer network;

	Map<Link, Link> linkLinkMapping = new HashMap<Link, Link>();
	HashMap<Node,Set<Link>> removedLinks = new HashMap<Node, Set<Link>>();

	private FreespeedTravelTimeCost cost;

	private Dijkstra router;


	public NetworkLoader(NetworkLayer net) {
		this.network = net;
	}

	public Map<MultiPolygon,List<Link>> getFloors() {
		if (!this.initialized) {
			loadNetwork();
		}
		Map<MultiPolygon,List<Link>> mps = new HashMap<MultiPolygon,List<Link>>();
		
		for (Geometry geo : this.geos) {
			MultiPolygon mp = null;
			if (geo instanceof MultiPolygon) {
				mp = (MultiPolygon) geo;
			} else if (geo instanceof Polygon) {
				mp = this.geofac.createMultiPolygon(new Polygon[]{(Polygon) geo});
			} else {
				throw new RuntimeException("Could not create Geometry of:" + geo);
			}
			
			//TODO needs to be changed as soon as we have several floors (multi polygon)
			log.warn("needs to be changed as soon as we have several floors (multi polygon)");
			List<Link> links = new ArrayList<Link>(this.network.getLinks().values());
			mps.put(mp,links);
		}
		return mps;
	}
	
	public Network loadNetwork() {

		if (!this.initialized) {
			init();
		}
		Network ret = null;
		loadShapeFile();
		simplifyGeometries();
		createNodes();
		createVisibilityGraph();
		simplifyVisibilityGraph();

		this.network.setEffectiveCellSize(0.26);
		this.network.setEffectiveLaneWidth(0.71);
		new NetworkCleaner().run(this.network);

		if (DEBUG) {
			ScenarioImpl scenario = new ScenarioImpl();
			scenario.getConfig().global().setCoordinateSystem("WGS84_UTM33N");
			//		CoordinateReferenceSystem crs = MGC.getCRS("WGS84_UTM33N");
			FeatureGeneratorBuilder builder = new FeatureGeneratorBuilder(this.network);
			//		builder.setCoordinateReferenceSystem(crs);
			builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
			builder.setWidthCoefficient(0.5);
			builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);		
			new Links2ESRIShape(this.network,"../../../../tmp/simplifiedNetwork.shp", builder).write();
		}

		return ret;
	}


	private void simplifyVisibilityGraph() {
		this.cost = new FreespeedTravelTimeCost();
		this.router = new Dijkstra(this.network, this.cost, this.cost);

		LinkComp comp = new LinkComp();

		boolean simplified = true;
		int iter = 0;
		while(simplified) {
			System.out.println("iter:" + iter++);
			simplified = false;
			for (Node node : this.network.getNodes().values()) {

				Queue<Link> queue = new PriorityQueue<Link>(node.getOutLinks().size(),comp);
				queue.addAll(node.getOutLinks().values());
				while (queue.size() > 0) {
					Link link = queue.poll();

					removeLink(link);
					if (!isRemovalValid(node)) {
						addLink(link);
					} else {
						simplified = true;
						break;
					}
				}



			}

		}

	}


	private void addLink(Link l) {
		this.network.addLink(l);
		Link lOpp = this.linkLinkMapping.get(l);
		this.network.addLink(lOpp);
		this.removedLinks.get(l.getFromNode()).remove(l);
		this.removedLinks.get(lOpp.getFromNode()).remove(lOpp);
	}

	private void removeLink(Link l) {
		this.network.removeLink(l);
		Link lOpp = this.linkLinkMapping.get(l);
		this.network.removeLink(lOpp);
		Set<Link> links1 = this.removedLinks.get(l.getFromNode());
		if (links1 == null) {
			links1 = new HashSet<Link>();
			this.removedLinks.put(l.getFromNode(), links1);
		}
		links1.add(l);
		Set<Link> links2 = this.removedLinks.get(lOpp.getFromNode());
		if (links2 == null) {
			links2 = new HashSet<Link>();
			this.removedLinks.put(lOpp.getFromNode(), links2);
		}
		links2.add(lOpp);
	}

	private boolean isRemovalValid(Node node) {
		for (Link l : this.removedLinks.get(node)) {
			double c = this.cost.getLinkTravelCost(l, 0);
			Path path = this.router.calcLeastCostPath(l.getFromNode(), l.getToNode(), 0);
			//				if ((path.travelCost  >= c * OVER_LENGTH) && (path.travelCost  >= c+OVER_LENGTHII)) {
			if (path == null || path.travelCost  >= c+OVER_LENGTHII) {
				return false;
			}
		}
		return true;
	}




	private void createVisibilityGraph() {
		int id = 0;
		List<NodeImpl> nodes = new ArrayList<NodeImpl>(this.network.getNodes().values());

		for (int i = 0; i < nodes.size()-1; i++) {
			for (int j = i + 1; j < nodes.size(); j++) {
				NodeImpl n1 = nodes.get(i);
				NodeImpl n2 = nodes.get(j);
				Coordinate [] coords = {MGC.coord2Coordinate(n1.getCoord()), MGC.coord2Coordinate(n2.getCoord())};
				LineString ls = this.geofac.createLineString(coords);
				boolean visible = true;
				for (Geometry geo : this.geos) {
					if (geo.intersects(ls)) {
						visible = false;
						break;
					}
				}
				if (visible) {
					this.visibilityGraph.add(ls);
					Link l1 = this.network.createAndAddLink(new IdImpl(id), n1, n2, ls.getLength(), 1.66, 3600., 1.);
					Link l2 = this.network.createAndAddLink(new IdImpl(100000 + id++), n2, n1, ls.getLength(), 1.66, 3600., 1.);
					this.linkLinkMapping.put(l1, l2);
					this.linkLinkMapping.put(l2, l1);
				}
			}
		}

		if (DEBUG) {
			dumpLineStrings(this.visibilityGraph,"../../../../tmp/visibilityGraph.shp");
		}
	}




	private void simplifyGeometries() {
		Queue<Geometry> queue = new PriorityQueue<Geometry>(this.geos.size(),new GeoComp());
		queue.addAll(this.geos);
		//		Set<Geometry> elements = new HashSet<Geometry>(this.geos);
		Set<Geometry> removed = new HashSet<Geometry>();
		while (queue.size() > 0) {
			//			System.out.println("queue:" + queue.size() + " elements:" + this.geos.size());
			Geometry geo = queue.poll();
			if (!this.geos.contains(geo)){
				continue;
			}
			Geometry other = null;

			for (Iterator<Geometry> it = this.geos.iterator(); it.hasNext();) {
				other = it.next();
				if (other.equals(geo)) {
					other = null;
					continue;
				}
				//HACK for now we want only one big MultiPolygon
				if (geo.intersects(other) || true){
					break;
				}
				if (it.hasNext() == false) {
					other = null;
				}
			}

			if (other != null) {
				this.geos.remove(other);
				this.geos.remove(geo);
				removed.add(geo);
				removed.add(other);
				Geometry newGeo = geo.union(other);
				this.geos.add(newGeo);
				queue.add(newGeo);
			}
		}

		//		for (Geometry geo : this.geos) {
		//			if (!(geo instanceof Polygon)) {
		//				throw new RuntimeException("Something went wrong geometry simplification!");
		//			}
		//			Polygon p = (Polygon) geo;
		//			LineString ls = p.getExteriorRing();
		//			
		//		}

		if (DEBUG) {
			dumpPolygons(this.geos,"../../../../tmp/mergedGeometries.shp");
		}

	}



	private static class LinkComp implements Comparator<Link> {

		public int compare(Link o1, Link o2) {
			if (o1.getLength() > o2.getLength()) {
				return -1;
			} else if (o1.getLength() < o2.getLength()) {
				return 1;
			}
			return 0;
		}

	}


	private static class GeoComp implements Comparator<Geometry> {

		public int compare(Geometry o1, Geometry o2) {
			if (o1.getCentroid().getCoordinate().x < o2.getCentroid().getCoordinate().x) {
				return -1;
			} else if(o1.getCentroid().getCoordinate().x > o2.getCentroid().getCoordinate().x) {
				return 1;
			} 
			return 0;
		}

	}

	private void loadShapeFile() {
		Iterator it = null;
		try {
			it = this.fs.getFeatures().iterator();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			this.geos.add(ft.getDefaultGeometry());
		}
	}


	private void createNodes() {

		QuadTree<Coordinate> quad = new QuadTree<Coordinate>(this.env.getMinX(),this.env.getMinY(),this.env.getMaxX(),this.env.getMaxY());

		for (Geometry geo : this.geos) {
			Coordinate[] coords = geo.getCoordinates();
			for (Coordinate c : coords) {
				List<Point> points = new ArrayList<Point>(); 
				double incr = 2*Math.PI/16;
				for (double alpha = 0; alpha < 2*Math.PI; alpha += incr) {
					double cos = Math.cos(alpha);
					double sin = Math.sin(alpha);
					double x = c.x + cos * RANGE;
					double y = c.y + sin * RANGE;
					Coordinate node = new Coordinate(x,y);
					points.add(this.geofac.createPoint(node));

				}
				Point best = null;
				double dist = Double.NEGATIVE_INFINITY;
				for (Point p : points) {
					if (geo.contains(p)) {
						continue;
					}
					if (p.distance(geo) > dist) {
						dist = p.distance(geo);
						best = p;
					}
				}
				quad.put(best.getX(), best.getY(), best.getCoordinate());
			}
		}
		List<Coordinate> coords = new ArrayList<Coordinate>(quad.values());
		Set<Coordinate> removed = new HashSet<Coordinate>();
		for (Coordinate coord : coords) {
			if (removed.contains(coord)) {
				continue;
			}
			Collection<Coordinate> coll = quad.get(coord.x, coord.y, 2*RANGE/3);
			if (coll.size() > 1) {
				double x = 0;
				double y = 0;
				for (Coordinate tmp : coll) {
					x += tmp.x;
					y += tmp.y;
					quad.remove(tmp.x, tmp.y, tmp);
					removed.add(tmp);
				}
				Coordinate newCoord = new Coordinate(x/coll.size(),y/coll.size());
				quad.put(newCoord.x, newCoord.y, newCoord);

			}

		}


		this.nodes = new ArrayList<Coordinate>(quad.values());

		int id = 0;
		for (Coordinate c: this.nodes) {
			this.network.createAndAddNode(new IdImpl(id++), MGC.coordinate2Coord(c));
		}

		if (DEBUG) {
			dumpPoints(quad.values(),"../../../../tmp/nodes.shp");
		}
	}




	private void init() {
		try {
			this.fs = ShapeFileReader.readDataFile(this.shapefile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try {
			this.env = this.fs.getBounds();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (DEBUG) {
			initFeatures();
		}


		this.initialized = true;
	}





	private void dumpLineStrings(List<LineString> elements,
			String string) {
		Collection<Feature> fts = new ArrayList<Feature>();
		for (Geometry geo : elements) {
			if (geo instanceof MultiPolygon) {
				geo = geo.getGeometryN(0);
			}
			try {
				fts.add(this.ftLine.create(new Object [] {geo,0,0}));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
		}
		try {
			ShapeFileWriter.writeGeometries(fts, string);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void dumpPolygons(Collection<Geometry> elements, String string) {
		Collection<Feature> fts = new ArrayList<Feature>();
		for (Geometry geo : elements) {
			MultiPolygon mp = null;
			if (geo instanceof MultiPolygon) {
				mp = (MultiPolygon) geo;
			} else if (geo instanceof Polygon) {
				mp = this.geofac.createMultiPolygon(new Polygon[]{(Polygon) geo});
			} else {
				throw new RuntimeException("Could not create Geometry of:" + geo);
			}
			try {
				fts.add(this.ft.create(new Object [] {mp,0,0}));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
		}
		try {
			ShapeFileWriter.writeGeometries(fts, string);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void dumpPoints(Collection<Coordinate> coords, String string) {
		Collection<Feature> fts = new ArrayList<Feature>();
		GeometryFactory geofac = new GeometryFactory();
		for (Coordinate c : coords) {
			Point p = geofac.createPoint(c);
			try {
				fts.add(this.ftPoint.create(new Object[]{p,0,0}));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}

		}
		try {
			ShapeFileWriter.writeGeometries(fts, string);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void initFeatures() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
		AttributeType p = DefaultAttributeTypeFactory.newAttributeType(
				"MultiPolygon", MultiPolygon.class, true, null, null, targetCRS);
		AttributeType l = DefaultAttributeTypeFactory.newAttributeType(
				"LineString", LineString.class, true, null, null, targetCRS);
		AttributeType po = DefaultAttributeTypeFactory.newAttributeType(
				"Point", Point.class, true, null, null, targetCRS);
		AttributeType z = AttributeTypeFactory.newAttributeType(
				"dblAvgZ", Double.class);
		AttributeType t = AttributeTypeFactory.newAttributeType(
				"dblAvgT", Double.class);

		Exception ex;
		try {
			this.ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { p, z, t }, "Polygon");
			this.ftLine = FeatureTypeFactory.newFeatureType(new AttributeType[] { l, z, t }, "Line");
			this.ftPoint = FeatureTypeFactory.newFeatureType(new AttributeType[] { po, z, t }, "Point");
			return;
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);

	}

	public static void main(String [] args) {
		Gbl.createConfig(null);
		new NetworkLoader(new NetworkLayer()).loadNetwork();
	}
}
