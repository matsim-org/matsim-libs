package playground.gregor.gis.medialaxis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

public class Test {

	public static void main(String [] args) throws FactoryRegistryException, SchemaException, IllegalAttributeException {
		VoronoiDiagramBuilder vd = new VoronoiDiagramBuilder();
		DelaunayTriangulationBuilder dt = new DelaunayTriangulationBuilder();


		String in = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/floor_plan/floor_transformed.shp";
		ShapeFileReader sf = new ShapeFileReader();
		sf.readFileAndInitialize(in);
		Set<Feature> fts = sf.getFeatureSet();
		Geometry geo = null;

		vd.setClipEnvelope(sf.getBounds());
		for (Feature ft : fts) {
			geo = ft.getDefaultGeometry();
			vd.setSites(geo);
			//						dt.setSites(geo);

		}

		//		List<Coordinate> coords = new ArrayList<Coordinate>();
		//		Envelope e = sf.getBounds();
		//		for (double x = e.getMinX(); x < e.getMaxX(); x += 100) {
		//			for (double y = e.getMinY(); y < e.getMaxY(); y += 100) {
		//				Coordinate c = new Coordinate(x, y);
		//
		//				if (geo.contains(MGC.coord2Point(MGC.coordinate2Coord(c)))){
		//
		//					coords.add(c);
		//				}
		//			}
		//		}

		//		dt.setSites(coords);

		//		GeometryCollection dia = (GeometryCollection) dt.getTriangles(new GeometryFactory());
		GeometryCollection dia = (GeometryCollection) vd.getDiagram(new GeometryFactory());
		System.out.println(dia);

		ArrayList<Feature> features = new ArrayList<Feature>();
		CoordinateReferenceSystem crs = sf.getCoordinateSystem();
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("Polygon", Polygon.class, true, null, null, crs);
		AttributeType floor = AttributeTypeFactory.newAttributeType("floor", Integer.class);
		FeatureType ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { geom, floor }, "FLOORS");

		List<Polygon> polygons = new ArrayList<Polygon>();
		for (int i =0 ; i < dia.getNumGeometries(); i++) {
			Polygon p = (Polygon) dia.getGeometryN(i);
			features.add(ft.create(new Object[]{p,i}));
		}

		ShapeFileWriter.writeGeometries(features, "/Users/laemmel/tmp/test.shp");

	}

}
