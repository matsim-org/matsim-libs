package playground.gregor.multiPedCrowds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.IllegalAttributeException;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class ClosedLinesShapeGenerator {

	private final FeatureSource fsP;
	private final FeatureSource fsL;
	private final String out;
	private QuadTree<LSInfo> quad;

	public ClosedLinesShapeGenerator(FeatureSource fsP, FeatureSource fsL,
			String out) {
		this.fsP = fsP;
		this.fsL = fsL;
		this.out = out;
	}

	public void run() throws IOException, IllegalAttributeException {
		buildQuad();

		GeometryFactory geofac = new GeometryFactory();
		List<Feature> fts = new ArrayList<Feature>();
		FeatureType ftype = this.fsL.getSchema();
		Iterator it = this.fsL.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			Geometry geo = ft.getDefaultGeometry();
			if (geo instanceof MultiLineString) {
				MultiLineString mls = (MultiLineString) geo;
				LineString [] lss = new LineString[mls.getNumGeometries()];
				for (int i = 0; i < mls.getNumGeometries(); i++) {


					LineString ls = (LineString) mls.getGeometryN(i);
					LSInfo start = this.quad.get(ls.getStartPoint().getX(), ls.getStartPoint().getY());
					LSInfo end = this.quad.get(ls.getEndPoint().getX(), ls.getEndPoint().getY());
					if (!start.ls.equals(end.ls)) {
						throw new RuntimeException();
					}

					int size = Math.abs(start.index -end.index) + 1;
					if (size == 27) {
						size = 2;
						start.index = 28;
					}
					if (size == 1) {
						size = start.ls.getNumPoints();
						start = new LSInfo();
						start.ls = end.ls;
						start.index = 0;
						end.index = size-1;
					}
					Coordinate [] coords = new Coordinate[size];
					if (start.index < end.index) {
						for (int j = 0; j < size; j++) {
							coords[j] = start.ls.getCoordinateN(start.index + j);
						}
					} else {
						for (int j = 0; j < size; j++) {
							coords[j] = start.ls.getCoordinateN(end.index + j);
						}
					}
					LineString newLs = geofac.createLineString(coords);
					lss[i] = newLs;
				}

				MultiLineString newMls = geofac.createMultiLineString(lss);
				Feature ftt = ftype.create(new Object[]{newMls,ft.getAttribute("lines")});
				fts.add(ftt);
			}else {
				throw new RuntimeException();
			}

		}
		ShapeFileWriter.writeGeometries(fts, this.out);
	}

	private void buildQuad() throws IOException {
		Envelope e = this.fsP.getBounds();
		this.quad = new QuadTree<LSInfo>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		Iterator it = this.fsP.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			Geometry geo = ft.getDefaultGeometry();
			if (geo instanceof Polygon) {
				addPolygon((Polygon)geo);
			} else if (geo instanceof MultiPolygon) {
				for (int i = 0; i < geo.getNumGeometries(); i++) {
					Polygon ggeo = (Polygon) geo.getGeometryN(i);
					addPolygon(ggeo);
				}
			}
		}
	}

	private void addPolygon(Polygon geo) {

		LineString shell = geo.getExteriorRing();
		addLineString(shell);
		for (int i = 0; i< geo.getNumInteriorRing(); i++) {
			LineString hole = geo.getInteriorRingN(i);
			addLineString(hole);
		}

	}

	private void addLineString(LineString shell) {
		for (int i = 0; i < shell.getCoordinates().length; i++) {
			Coordinate c = shell.getCoordinateN(i);
			LSInfo li = new LSInfo();
			li.index = i;
			li.ls = shell;
			this.quad.put(c.x, c.y, li);

		}

	}


	private static class LSInfo {
		LineString ls;
		int index;

	}

	public static void main(String[] args) throws IOException {
		String lines = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/floor_plan/boundaries_transformed.shp";
		String poly = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/floor_plan/floor_transformed.shp";
		String out = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/floor_plan/boundaries_transformed_closed.shp";

		FeatureSource fsP = ShapeFileReader.readDataFile(poly);
		FeatureSource fsL = ShapeFileReader.readDataFile(lines);

		try {
			new ClosedLinesShapeGenerator(fsP,fsL,out).run();
		} catch (IllegalAttributeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
