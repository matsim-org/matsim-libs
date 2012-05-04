package playground.gregor.multidestpeds.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class AsciiFromShp {



	public static void main(String [] args) throws IOException, IllegalAttributeException {
		String out = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/floor_plan/boundaries_closed_transformed.shp";
		//		String out = "/Users/laemmel/tmp/hjsk.shp";
		FeatureSource fs = ShapeFileReader.readDataFile(out);
		Iterator it = fs.getFeatures().iterator();

		List<Feature> fts = new ArrayList<Feature>();
		System.out.println("#linenr,typ,x,y");
		int i = 0;
		while (it.hasNext() ) {
			Feature ft = (Feature) it.next();
			fts.add(ft);
			LineString ls = (LineString)((MultiLineString)ft.getDefaultGeometry()).getGeometryN(0);
			for (int j = 0; j < ls.getNumPoints(); j++) {
				System.out.println(i +"," + ft.getAttribute("lines") + "," + ls.getCoordinateN(j).x + "," + ls.getCoordinateN(j).y);

			}
			i++;

		}
		GeometryFactory gf = new GeometryFactory();
		LineString ls = gf.createLineString(new Coordinate[]{new Coordinate(1,21),new Coordinate(13,23)});
		MultiLineString ml = gf.createMultiLineString(new LineString[]{ls});
		
		FeatureType ft = initFeatures();
		
		
		Feature lsFeature = ft.create(new Object[]{ml,"wand"});
		fts.add(lsFeature);
		
		ShapeFileWriter.writeGeometries(fts, "/Users/laemmel/tmp/!!!ftsTest.shp");
		
	}
	
	private static FeatureType initFeatures() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG: 32632");
		AttributeType l = DefaultAttributeTypeFactory.newAttributeType(
				"MultiLineString", MultiLineString.class, true, null, null, targetCRS);
		AttributeType t = AttributeTypeFactory.newAttributeType(
				"name", String.class);

		Exception ex;
		try {
			FeatureType ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { l,t }, "Boundary");
			return ft;
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);

	}
}
