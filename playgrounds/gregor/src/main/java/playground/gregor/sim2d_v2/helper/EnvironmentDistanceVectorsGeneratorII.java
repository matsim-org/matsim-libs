package playground.gregor.sim2d_v2.helper;

import java.io.IOException;
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.distance.DistanceOp;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.io.EnvironmentDistancesWriter;
import playground.gregor.sim2d_v2.simulation.floor.EnvironmentDistances;
import playground.gregor.sim2d_v2.simulation.floor.StaticEnvironmentDistancesField;

public class EnvironmentDistanceVectorsGeneratorII {

	private static final Logger log = Logger.getLogger(EnvironmentDistanceVectorsGeneratorII.class);

	private final Sim2DConfigGroup config;
	private QuadTree<EnvironmentDistances> distancesQuadTree;
	private MultiPolygon environment;
	private Envelope envelope;
	private final GeometryFactory geofac;


	//physical parameters
	private final double maxSensingRange = 5;
	private double res =.05;
	private double incr = 2 * Math.PI/16;
	private final double minDist = .001;

	public EnvironmentDistanceVectorsGeneratorII(Config c) {
		this.config = ((Sim2DConfigGroup)c.getModule("sim2d"));
		this.geofac = new GeometryFactory();
	}

	public StaticEnvironmentDistancesField generate() {
		Set<Feature> features = null;
		ShapeFileReader reader = new ShapeFileReader();
		try {
			features = reader.readFileAndInitialize(this.config.getFloorShapeFile());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.envelope = reader.getBounds();
		this.distancesQuadTree = new QuadTree<EnvironmentDistances>(this.envelope.getMinX(), this.envelope.getMinY() , this.envelope.getMaxX(), this.envelope.getMaxY());
		initMultiPolygon(features);
		calculateDistanceVectors();
		return new StaticEnvironmentDistancesField(this.distancesQuadTree,this.maxSensingRange,this.res);

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

	}

	private void calculateDistanceVectors(double x, double y) {
		Point point = this.geofac.createPoint(new Coordinate(x, y));
		if (!this.environment.covers(point) && this.environment.distance(point) > this.minDist) {
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

		Geometry g = this.environment.intersection(p);
		if ((g instanceof MultiPolygon) || !(g instanceof GeometryCollection)) {
			DistanceOp op = new DistanceOp(g, this.geofac.createPoint(coords[0]));
			Coordinate[] tmp = op.closestPoints();
			double fX = tmp[1].x - tmp[0].x;
			double fY = tmp[1].y - tmp[0].y;
			double dist = Math.sqrt(Math.pow(fX, 2) + Math.pow(fY, 2));
			if (dist > this.maxSensingRange || dist <= this.minDist) {
				throw new RuntimeException("this should not happen!!");
			}
			ed.addEnvironmentDistanceLocation(tmp[0]);
			//			GisDebugger.addGeometry(this.geofac.createLineString(new Coordinate[] {coords[0],tmp[0]}));

		}

	}

	private void initMultiPolygon(Set<Feature> features) {
		List<Polygon> p = new ArrayList<Polygon>();
		for (Feature ft : features) {
			Geometry geo = ft.getDefaultGeometry();
			if (geo instanceof Polygon) {
				p.add((Polygon) geo);
			} else if (geo instanceof MultiPolygon) {
				for (int i = 0; i < geo.getNumGeometries(); i++) {
					p.add((Polygon) geo.getGeometryN(i));
				}
			} else {
				throw new RuntimeException("error processing Geomtry" + geo.getClass() + " only Polygon or MultiPolygon are allowed!");
			}
		}
		GeometryFactory geofac = new GeometryFactory();

		Polygon[] polygons = new Polygon[p.size()];
		for (int i = 0; i < p.size(); i++) {
			polygons[i] = p.get(i);
		}

		this.environment = (MultiPolygon) geofac.createMultiPolygon(polygons).buffer(0);


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

		StaticEnvironmentDistancesField fl = new EnvironmentDistanceVectorsGeneratorII(c).generate();
		new EnvironmentDistancesWriter().write(s.getStaticEnvFieldFile(), fl);
	}

	public void setResolution(double res) {
		this.res = res;

	}

	public void setIncr(double incr) {
		this.incr = incr;
	}


}
