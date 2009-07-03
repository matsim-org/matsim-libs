package playground.gregor.gis.mesh;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class MeshFromFloodingData {
	
	
	private final String netcdf;
	private final String outShape;
	private FeatureType ft;
	
	public MeshFromFloodingData(String netcdf, String outShape) {
		this.netcdf = netcdf;
		this.outShape = outShape;
	}

	private void run(){
		initFeatureType();
		GeometryFactory geofac = new GeometryFactory();
		List<Feature> fts = new ArrayList<Feature>();
		FloodingReader fl = new FloodingReader(this.netcdf,true);
		List<int[]> tris = fl.getTriangles();
		List<FloodingInfo> fis = fl.getFloodingInfos();
		Map<Integer, Integer> mapping = fl.getIdxMapping();
		int count = 0;
		for (int [] tri : tris) {
			if (count++ % 500 == 0) {
				System.out.println("count:" + count);
			}
			Coordinate [] coords = new Coordinate[4];
			double z = 0.;
			boolean cancel = false;
			for (int i = 0; i < 3; i++) {
				
				Integer idx  = mapping.get(tri[i]);
				if (idx == null) {
					cancel = true;
					break;
				}
				FloodingInfo fi = fis.get(idx);
				coords[i] = fi.getCoordinate();
				z += fi.getFloodingTime();
			}
			if (cancel) {
				continue;
			}
			
			z /= 3.;
			coords[3] = coords[0];
			LinearRing lr = geofac.createLinearRing(coords);
			Polygon p = geofac.createPolygon(lr, null);
			try {
				fts.add(this.ft.create(new Object []{p,z}));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
		}
		
		try {
			ShapeFileWriter.writeGeometries(fts, this.outShape);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void initFeatureType() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		AttributeType p = DefaultAttributeTypeFactory.newAttributeType(
				"Polygon", Polygon.class, true, null, null, targetCRS);
		AttributeType z = AttributeTypeFactory.newAttributeType(
				"dblAvgZ", Double.class);

		Exception ex;
		try {
			this.ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { p, z }, "Polygon");
			return;
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);
	}
	
	public static void main (String [] args) {
		String netcdf = "./test/input/org/matsim/evacuation/data/flooding.sww";
		String outShape = "../../analysis/mesh/mesh.shp";
		new MeshFromFloodingData(netcdf,outShape).run();
	}


}
