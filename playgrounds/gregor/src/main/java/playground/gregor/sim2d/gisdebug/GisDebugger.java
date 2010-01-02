package playground.gregor.sim2d.gisdebug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Point;

public class GisDebugger {

	private static FeatureType ft;

	private static FeatureType ftLine;

	private static FeatureType ftPoint;
	
	private static List<Geometry> geos = new ArrayList<Geometry>();
	
	private static boolean init = false;
	
	private static final GeometryFactory geofac = new GeometryFactory();
	
	public static void addGeometry(Geometry geo) {
		geos.add(geo);
	}
	
	public static void dump(String file) {
		if (!init) {
			initFeatures();
			init = true;
		}
		Collection<Feature> fts = new  ArrayList<Feature>();
		double d = 0;
		for (Geometry geo : geos) {
			if (geo instanceof MultiPolygon) {
				try {
					fts.add(ft.create(new Object[] {geo,d,d++}));
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}
			} else if (geo instanceof Polygon) {
				MultiPolygon mp = geofac.createMultiPolygon(new Polygon[]{(Polygon) geo});
				try {
					fts.add(ft.create(new Object[] {mp,d,d++}));
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}
			}else if (geo instanceof LineString) {
				try {
					fts.add(ftLine.create(new Object[] {geo,d,d++}));
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}				
			} else {
				throw new RuntimeException("type of Geometry is not supported" + geo);
			}
			
		}
		try {
			ShapeFileWriter.writeGeometries(fts, file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		geos.clear();
	}



	
	
	private static void initFeatures() {
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
			ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { p, z, t }, "MultiPolygon");
			ftLine = FeatureTypeFactory.newFeatureType(new AttributeType[] { l, z, t }, "Line");
			ftPoint = FeatureTypeFactory.newFeatureType(new AttributeType[] { po, z, t }, "Point");
			return;
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);

	}

}
