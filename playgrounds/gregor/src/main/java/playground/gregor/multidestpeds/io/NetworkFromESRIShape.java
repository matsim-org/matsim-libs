package playground.gregor.multidestpeds.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

public class NetworkFromESRIShape {

	private final Scenario sc;
	private final GeometryFactory geofac = new GeometryFactory();


	private final double oneUnitInSIUnit = 1;
	private final double fetchRadius = .25;

	public NetworkFromESRIShape(Scenario sc) {
		this.sc = sc;
	}

	public void processShapeFile(String fileName) {
		ShapeFileReader r = new ShapeFileReader();
		Set<Feature> fts = r.readFileAndInitialize(fileName);
		List<LineString> ls = new ArrayList<LineString>();
		for (Feature ft : fts) {
			Geometry geo = ft.getDefaultGeometry();
			if (geo instanceof LineString) {
				ls.add((LineString)geo);
			} else if (geo instanceof MultiLineString) {
				for (int i = 0; i < geo.getNumGeometries(); i++) {
					LineString l = (LineString) geo.getGeometryN(i);
					ls.add(l);
				}
			} else {
				throw new RuntimeException(geo.getGeometryType() + " not supported!!");
			}
		}
		List<LineString> simplified = simplify(ls);
		List<LineString> splitted = splitAtIntersections(simplified);
		createNetwork(splitted,r.getBounds());
	}

	private void createNetwork(List<LineString> ls, Envelope envelope) {
		QuadTree<Node> tree = new QuadTree<Node>(envelope.getMinX()*this.oneUnitInSIUnit,envelope.getMinY()*this.oneUnitInSIUnit,envelope.getMaxX()*this.oneUnitInSIUnit,envelope.getMaxY()*this.oneUnitInSIUnit);

		Network net = this.sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		int nodeIds = 0;
		for (LineString l : ls) {
			double fx = l.getStartPoint().getX() * this.oneUnitInSIUnit;
			double fy = l.getStartPoint().getY() * this.oneUnitInSIUnit;
			double tx = l.getEndPoint().getX() * this.oneUnitInSIUnit;
			double ty = l.getEndPoint().getY() * this.oneUnitInSIUnit;

			Collection<Node> coll = tree.get(fx, fy, this.fetchRadius);
			if (coll.size()==0) {
				Coord c = new CoordImpl(fx,fy);
				Node n = fac.createNode(this.sc.createId(nodeIds++ +""), c);
				net.addNode(n);
				tree.put(fx, fy, n);
			}

			coll = tree.get(tx, ty, this.fetchRadius);
			if (coll.size()==0) {
				Coord c = new CoordImpl(tx,ty);
				Node n = fac.createNode(this.sc.createId(nodeIds++ +""), c);
				net.addNode(n);
				tree.put(tx, ty, n);
			}
		}

		int linkIds = 0;
		for (LineString l : ls) {
			Node n1 = tree.get(l.getStartPoint().getX()*this.oneUnitInSIUnit, l.getStartPoint().getY()*this.oneUnitInSIUnit);
			Node n2 = tree.get(l.getEndPoint().getX()*this.oneUnitInSIUnit, l.getEndPoint().getY()*this.oneUnitInSIUnit);
			Link l1 = fac.createLink(this.sc.createId(linkIds++ +""),n1, n2);
			net.addLink(l1);
			Link l2 = fac.createLink(this.sc.createId(linkIds++ +""),n2, n1);
			net.addLink(l2);
			l1.setFreespeed(1.34);
			double length = CoordUtils.calcDistance(n1.getCoord(), n2.getCoord());
			l1.setLength(length);
			l2.setFreespeed(1.34);
			l2.setLength(length);
		}

	}

	private List<LineString> simplify(List<LineString> ls) {
		List<LineString> ret = new ArrayList<LineString>();
		for (LineString l : ls) {
			for (int i = 0; i < l.getNumPoints()-1; i++) {
				Coordinate [] tmp = {l.getCoordinateN(i),l.getCoordinateN(i+1)};
				ret.add(this.geofac.createLineString(tmp));
			}
		}

		return ret;
	}

	private List<LineString> splitAtIntersections(List<LineString> ls) {
		List<LineString> ret = new ArrayList<LineString>();
		Map<LineString,List<Coordinate>> intersections = new HashMap<LineString,List<Coordinate>>();

		for (int i = 0; i < ls.size()-1; i++) {
			LineString ls1 = ls.get(i);
			//			List<Coordinate> selfIntersects = getIntersections(ls1,ls1);
			for (int j = i+1; j < ls.size(); j++) {
				LineString ls2 = ls.get(j);
				List<Coordinate> intersects = getIntersections(ls1,ls2);
				addInterSections(ls1,intersects,intersections);
				List<Coordinate> intersects2 = getIntersections(ls2,ls1);
				addInterSections(ls2,intersects2,intersections);
			}

		}
		for (LineString l : ls) {
			ret.addAll(splitLineString(l,intersections.get(l)));

		}

		return ret;
	}

	private Collection<? extends LineString> splitLineString(LineString l,
			List<Coordinate> list) {
		List<LineString> ret = new ArrayList<LineString>();
		if (list == null) {
			ret.add(l);
		} else if (list.size() > 1){
			throw new RuntimeException("Not yet implemented!");

		}else{
			Coordinate splittingC = list.iterator().next();
			ret.add(this.geofac.createLineString(new Coordinate[]{l.getStartPoint().getCoordinate(),splittingC}));
			ret.add(this.geofac.createLineString(new Coordinate[]{splittingC,l.getEndPoint().getCoordinate()}));
		}

		return ret;
	}

	private void addInterSections(LineString ls, List<Coordinate> intersects, Map<LineString, List<Coordinate>> intersections) {
		if (intersects.size() == 0) {
			return;
		}
		List<Coordinate> tmp = intersections.get(ls);
		if (tmp == null) {
			tmp = new ArrayList<Coordinate>();
			intersections.put(ls, tmp);
		}
		tmp.addAll(intersects);
	}

	private List<Coordinate> getIntersections(LineString ls1, LineString ls2) {
		List<Coordinate> ret = new ArrayList<Coordinate>();
		Geometry g = ls1.intersection(ls2);

		if (g instanceof Point) {
			if (g.distance(ls1.getStartPoint())>this.fetchRadius && ls1.getEndPoint().distance(g) > this.fetchRadius){
				ret.add(g.getCoordinate());
			}
		} else if(g instanceof GeometryCollection) {
			GeometryCollection geoColl = (GeometryCollection) g;
			int num = geoColl.getNumGeometries();
			for (int i = 0; i < num; i++) {
				Geometry gg = geoColl.getGeometryN(i);
				if (!(g instanceof Point)) {
					throw new RuntimeException(gg.getGeometryType() + " not supported!!");
				}
				if (gg.distance(ls1.getStartPoint())>this.fetchRadius && ls1.getEndPoint().distance(gg) > this.fetchRadius){
					ret.add(g.getCoordinate());
				}
			}
		}

		return ret;
	}

	public static void main(String [] args) {
		String networkShape = "/Users/laemmel/devel/dfg/data/links.shp";
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		new NetworkFromESRIShape(sc).processShapeFile(networkShape);

		new NetworkWriter(sc.getNetwork()).write("/Users/laemmel/devel/dfg/data/90gradNetwork.xml");
	}

}
