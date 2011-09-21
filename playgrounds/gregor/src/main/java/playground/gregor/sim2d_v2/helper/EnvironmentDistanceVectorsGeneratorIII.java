package playground.gregor.sim2d_v2.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.io.EnvironmentDistancesWriter;
import playground.gregor.sim2d_v2.simulation.floor.EnvironmentDistances;
import playground.gregor.sim2d_v2.simulation.floor.StaticEnvironmentDistancesField;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.distance.DistanceOp;

public class EnvironmentDistanceVectorsGeneratorIII {

	private static final Logger log = Logger.getLogger(EnvironmentDistanceVectorsGeneratorIII.class);

	private final Sim2DConfigGroup config;
	private QuadTree<EnvironmentDistances> distancesQuadTree;
	private Envelope envelope;
	private final GeometryFactory geofac;

	private List<LineString> walls;

	//physical parameters
	private final double maxSensingRange = 5;
	private double res =0.05;
	private double incr = 2 * Math.PI/8;
	private final double minDist = .001;


	public EnvironmentDistanceVectorsGeneratorIII(Config c) {
		this.config = ((Sim2DConfigGroup)c.getModule("sim2d"));
		this.geofac = new GeometryFactory();
	}

	public StaticEnvironmentDistancesField generate() {
		Set<Feature> features = null;
		ShapeFileReader reader = new ShapeFileReader();
		features = reader.readFileAndInitialize(this.config.getFloorShapeFile());
		this.envelope = reader.getBounds();
		this.distancesQuadTree = new QuadTree<EnvironmentDistances>(this.envelope.getMinX(), this.envelope.getMinY() , this.envelope.getMaxX(), this.envelope.getMaxY());
		initWalls(features);
		calculateDistanceVectors();
		return new StaticEnvironmentDistancesField(this.distancesQuadTree,this.maxSensingRange,this.res);

	}

	private void initWalls(Set<Feature> features) {

		this.walls = new ArrayList<LineString>();
		for (Feature ft :features) {
			Geometry geo = ft.getDefaultGeometry();
			handleGeo(geo);
		}

	}

	private void handleGeo(Geometry geo) {
		if (geo instanceof MultiPolygon) {
			addMP((MultiPolygon)geo);
		} else if (geo instanceof Polygon) {
			addP((Polygon)geo);
		} else if (geo instanceof MultiLineString) {
			addMLS((MultiLineString)geo);
		} else if (geo instanceof LineString) {
			this.walls.add((LineString) geo);
		} else {
			throw new RuntimeException("Unsupported geometry: " + geo.getGeometryType());
		}
	}

	private void addMLS(MultiLineString geo) {
		for (int i = 0; i < geo.getNumGeometries(); i++) {
			handleGeo(geo.getGeometryN(i));
		}
	}

	private void addP(Polygon geo) {
		handleGeo(geo.getExteriorRing());
		for (int i = 0; i < geo.getNumInteriorRing(); i++) {
			handleGeo(geo.getInteriorRingN(i));
		}
	}

	private void addMP(MultiPolygon geo) {
		for (int i = 0; i < geo.getNumGeometries(); i++) {
			handleGeo(geo.getGeometryN(i));
		}

	}

	private void calculateDistanceVectors() {
		int loop = 0;
		int yloop = 0;
		for (double x = this.envelope.getMinX(); x <= this.envelope.getMaxX(); x += this.res) {
			log.info("xloop:" + ++loop + "  yloop:" + yloop);
			for (double y = this.envelope.getMinY(); y <= this.envelope.getMaxY(); y += this.res) {
				yloop++;
				calculateDistanceVectors(x,y);
			}
		}

		//		GisDebugger.dump("/Users/laemmel/tmp/dump.shp");


	}

	private void calculateDistanceVectors(double x, double y) {
		Point point = this.geofac.createPoint(new Coordinate(x, y));
		if (wallDistance(point) > this.minDist) {
			EnvironmentDistances ed = scanEnvironment(x, y);
			if (ed != null) {
				//				try {
				this.distancesQuadTree.put(x, y, ed);
				//				} catch (Exception e) {
				//					e.printStackTrace();
				//					throw new RuntimeException(e);
				//				}
				//				GisDebugger.dump("/Users/laemmel/devel/dfg/tmp/dbg.shp");
			}

		} //else {
		//			GisDebugger.addGeometry(point);
		//			GisDebugger.dump("/Users/laemmel/devel/dfg/tmp/dbg.shp");
		//		}
	}

	private double wallDistance(Point point) {
		double minDist = Double.POSITIVE_INFINITY;
		for (Geometry geo :this.walls) {
			double dist = point.distance(geo);
			if (dist < minDist) {
				minDist = dist;
			}
		}

		return minDist;
	}

	private EnvironmentDistances scanEnvironment(double x, double y) {
		Coordinate location = new Coordinate(x, y, 0);
		EnvironmentDistances ed = new EnvironmentDistances(location);
		double alpha = 0;
		for (; alpha < 2 * Math.PI;) {
			Coordinate[] coords = new Coordinate[4];
			coords[0] = location;

			double cos = Math.cos(alpha);
			double sin = Math.sin(alpha);

			double x1 = x + cos * this.maxSensingRange;
			double y1 = y + sin * this.maxSensingRange;
			Coordinate c1 = new Coordinate(x1, y1);
			coords[1] = c1;

			alpha += this.incr;

			cos = Math.cos(alpha);
			sin = Math.sin(alpha);
			double x2 = x + cos * this.maxSensingRange;
			double y2 = y + sin * this.maxSensingRange;
			Coordinate c2 = new Coordinate(x2, y2);
			coords[2] = c2;
			coords[3] = location;

			calcAndAddSectorObject(ed, coords);


		}

		return ed;
	}

	private void calcAndAddSectorObject(EnvironmentDistances ed, Coordinate[] coords) {
		Polygon p = this.geofac.createPolygon(this.geofac.createLinearRing(coords), null);
		Point origin = this.geofac.createPoint(coords[0]);
		//		Geometry g = this.environment.intersection(p);
		Geometry g = getNearestWallIntersection(p,origin);
		if (g != null && (g instanceof LineString)) {
			//			GisDebugger.addGeometry(this.geofac.createLineString(new Coordinate[]{origin.getCoordinate(), new Coordinate(origin.getX()+0.1, origin.getY()+0.1),new Coordinate(origin.getX()+0.1, origin.getY()-0.1),new Coordinate(origin.getX()-0.1, origin.getY()-0.1),new Coordinate(origin.getX()-0.1, origin.getY()+0.1)}));
			DistanceOp op = new DistanceOp(g, origin);
			Coordinate[] tmp = op.closestPoints();
			//			GisDebugger.addGeometry(this.geofac.createLineString(tmp));


			double fX = tmp[1].x - tmp[0].x;
			double fY = tmp[1].y - tmp[0].y;
			double dist = Math.sqrt(Math.pow(fX, 2) + Math.pow(fY, 2));
			if (dist > this.maxSensingRange || dist <= this.minDist) {
				throw new RuntimeException("this should not happen!!");
			}
			ed.addEnvironmentDistanceLocation(tmp[0]);


		}

	}

	private Geometry getNearestWallIntersection(Polygon p, Point origin) {

		double nearestDist = Double.POSITIVE_INFINITY;
		Geometry nearest = null;

		for (Geometry g : this.walls) {
			Geometry tmp = g.intersection(p);
			if (tmp.isEmpty()) {
				continue;
			}
			double dist = tmp.distance(origin);
			if (dist < nearestDist) {
				nearestDist = dist;
				nearest = tmp;
			}
		}

		if (nearest instanceof MultiLineString) {
			//			GisDebugger.addGeometry(this.geofac.createLineString(new Coordinate[]{origin.getCoordinate(), new Coordinate(origin.getX()+0.1, origin.getY()+0.1),new Coordinate(origin.getX()+0.1, origin.getY()-0.1),new Coordinate(origin.getX()-0.1, origin.getY()-0.1),new Coordinate(origin.getX()-0.1, origin.getY()+0.1)}));
			for (int i = 0; i < nearest.getNumGeometries(); i++) {
				Geometry geo = nearest.getGeometryN(i);
				//				GisDebugger.addGeometry(geo);
				double dist = geo.distance(origin);
				if (dist <= nearestDist) {
					nearest = geo;
				}
			}
			//			GisDebugger.dump("/Users/laemmel/tmp/dump.shp");
		}
		return nearest;
	}


	public void setResolution(double res) {
		this.res = res;

	}

	public void setIncr(double incr) {
		this.incr = incr;
	}

	public static void main(String [] args) {
		String cf = args[0];
		Config c = ConfigUtils.loadConfig(cf);
		Module module = c.getModule("sim2d");
		Sim2DConfigGroup s = null;
		if (module == null) {
			s = new Sim2DConfigGroup();
		} else {
			s = new Sim2DConfigGroup(module);
		}
		c.getModules().put("sim2d", s);

		StaticEnvironmentDistancesField fl = new EnvironmentDistanceVectorsGeneratorIII(c).generate();
		new EnvironmentDistancesWriter().write(s.getStaticEnvFieldFile(), fl);
	}
}
