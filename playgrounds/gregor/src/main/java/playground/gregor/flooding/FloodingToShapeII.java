package playground.gregor.flooding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
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

import playground.gregor.MY_STATIC_STUFF;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class FloodingToShapeII {
	
	private static final Logger log = Logger.getLogger(FloodingToShapeII.class);
	
	public static void main(String [] args) {
		List<Triangle> geos = new ArrayList<Triangle>();
		
		for (int i = 0; i < MY_STATIC_STUFF.SWW_COUNT; i++) {
			log.info("Reading netcdf file:" + i);
			String file = MY_STATIC_STUFF.SWW_ROOT + "/" + MY_STATIC_STUFF.SWW_PREFIX + i + MY_STATIC_STUFF.SWW_SUFFIX;
			FloodingReader fr = new FloodingReader(file);
			double offsetEast = 632968.461027224;
			double offsetNorth = 9880201.726;
			fr.setOffset(offsetEast, offsetNorth);
			fr.setReadFloodingSeries(true);
			fr.setReadTriangles(true);
			Map<Integer, Integer> mapping = fr.getIdxMapping();
			List<int[]> tris = fr.getTriangles();
			for (int [] tri : tris) {
				Triangle t = new Triangle();
				boolean success = true;
				for (int j = 0; j < 3; j++) {
					Integer idx = mapping.get(tri[j]);
					if (idx == null) {
						success = false;
						break;
					}
					FloodingInfo fi = fr.getFloodingInfos().get(idx);
					t.coords[j] = fi.getCoordinate();
					t.time += fi.getFloodingTime();
					double height = 0;
					for (double d : fi.getFloodingSeries()) {
						if (d > height) {
							height = d;
						}
					}
					t.floodingHeight += height;
				}
				if (success) {
					t.time /= 3;
					t.floodingHeight /= 3;
					t.coords[3] = t.coords[0];
					geos.add(t);
				}
			}
		}
		Stack<Triangle> tris = new Stack<Triangle>();
		tris.addAll(geos);
		geos.clear();
		
		GeometryFactory geofac = new GeometryFactory();
		FeatureType ft = createFeatureType();
		Collection<Feature> fts = new ArrayList<Feature>();
		while (tris.size() > 0) {
			Triangle tri = tris.pop();
						
			LinearRing lr = geofac.createLinearRing(tri.coords);
			Polygon p = geofac.createPolygon(lr, null);
			
			try {
				fts.add(ft.create(new Object[]{p,tri.time,tri.floodingHeight}));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
		}
		
		try {
			ShapeFileWriter.writeGeometries(fts, "/home/laemmel/arbeit/diss/qgis/floodingII.shp");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static FeatureType createFeatureType() {
		
		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType(
				"Polygon", Polygon.class, true, null, null,crs);
		AttributeType dblTime = AttributeTypeFactory.newAttributeType(
				"floodingTime", Double.class);
		AttributeType dblHeight = AttributeTypeFactory.newAttributeType(
				"floodingHeight", Double.class);
		
		Exception ex;
		try {
			return  FeatureTypeFactory.newFeatureType(new AttributeType[] {
					geom, dblTime, dblHeight }, "FloodingTriangle");
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);
	}

	private static class Triangle {
		Coordinate [] coords = new Coordinate [4];
		double time = 0;
		double floodingHeight = 0;
	}
}
