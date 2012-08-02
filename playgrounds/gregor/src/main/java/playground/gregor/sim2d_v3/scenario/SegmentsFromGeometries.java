package playground.gregor.sim2d_v3.scenario;

import org.geotools.feature.Feature;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class SegmentsFromGeometries {

	private static double SUSPENSION_POINTS_MAX_DIST = .5;


	private final QuadTree<float[]> floatSegQuad;

	public SegmentsFromGeometries(ShapeFileReader reader) {
		Envelope e = reader.getBounds();
		this.floatSegQuad = new QuadTree<float[]>(e.getMinX()-1000,e.getMinY()-1000,e.getMaxX()+1000,e.getMaxY()+1000);
		for (Feature ft : reader.getFeatureSet()){
			Geometry geo = ft.getDefaultGeometry();
			handleGeometry(geo);
		}
	}

	private void handleGeometry(Geometry geo) {
		if (geo instanceof LineString){
			handleLineString((LineString)geo);
		} else if (geo instanceof Polygon) {
			handlePolygon((Polygon)geo);
		} else if (geo instanceof MultiLineString) {
			handleMultiGeometry(geo);
		} else if (geo instanceof MultiPolygon) {
			handleMultiGeometry(geo);
		}

	}


	private void handleMultiGeometry(Geometry geo) {
		for (int i = 0; i < geo.getNumGeometries(); i++) {
			Geometry tmp = geo.getGeometryN(i);
			handleGeometry(tmp);
		}
	}


	private void handlePolygon(Polygon geo) {
		LineString ls = geo.getExteriorRing();
		handleLineString(ls);
	}


	private void handleLineString(LineString geo) {
		Coordinate [] coords = geo.getCoordinates();
		for (int i = 0; i < coords.length-1; i++) {
			handleSegment(coords[i],coords[i+1]);
		}

	}

	private void handleSegment(Coordinate c0, Coordinate c1) {
		
		
		
		double length = c0.distance(c1);
		double dx = SUSPENSION_POINTS_MAX_DIST*(c1.x-c0.x)/length;
		double dy = SUSPENSION_POINTS_MAX_DIST*(c1.y-c0.y)/length;
		int increments = (int) (length/SUSPENSION_POINTS_MAX_DIST);

		float xold = (float) c0.x;
		float yold = (float) c0.y;
		float[] seg = {xold,yold,(float) c1.x,(float) c1.y};
		this.floatSegQuad.put(c0.x, c0.y, seg);
		for (int i = 1; i <= increments; i++) {
			double x = c0.x + i*dx;
			double y = c0.y + i*dy;
			this.floatSegQuad.put(x, y, seg);
		}
		this.floatSegQuad.put(c1.x, c1.y, seg);
	}


	public QuadTree<float[]> getFloatSegQuadTree() {
		return this.floatSegQuad;
	}
}
