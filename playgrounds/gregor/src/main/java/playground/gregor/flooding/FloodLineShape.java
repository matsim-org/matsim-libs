package playground.gregor.flooding;

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
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.evacuation.flooding.FloodingLine;
import org.matsim.evacuation.flooding.FloodingReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class FloodLineShape {

	private final String output;
	private FeatureType featureType;
	private GeometryFactory geofac;
	private FeatureType featureType2;
	private final String output2;
	private final List<FloodingReader> frs;
	private final CoordinateReferenceSystem crs;


	public FloodLineShape(List<FloodingReader> frs2,
			CoordinateReferenceSystem crs) {
		this.frs = frs2;
		this.crs = crs;
		this.output = "../../tmp/floodLine1.shp";
		this.output2 = "../../tmp/floodLine2.shp";
		run();
	}

	private void run() {
		initFeatureType();
		Collection<Feature> fts = new ArrayList<Feature>();
		Collection<Feature> fts2 = new ArrayList<Feature>();
		for (FloodingReader fr : this.frs) {
//			fr.setReadTriangles(true);
			FloodingLine fl = new FloodingLine(fr);
			int singletons = 0;
			for (int i = 0; i <= 50; i += 1) {
				List<ArrayList<Coordinate>> allCoords = fl.getFloodLine(i);
				for (ArrayList<Coordinate> coords : allCoords) {
	
					if (coords.size() == 1) {
						singletons++;
						continue;
					}
					Coordinate[] coordinates = new Coordinate[coords.size()];
					for (int j = 0; j < coords.size(); j++) {
						coordinates[j] = coords.get(j);
					}
					LineString ls = this.geofac.createLineString(coordinates);
					try {
						fts.add(this.featureType.create(new Object[] { ls, i }));
					} catch (IllegalAttributeException e) {
						e.printStackTrace();
					}
				}
			}
			System.out.println("singletons:" + singletons);
//			fr.resetCache();
		}
		
//		for (FloodingReader fr : this.frs) {
//			for (FloodingInfo fi : fr.getFloodingInfos()) {
//				Point p = this.geofac.createPoint(fi.getCoordinate());
//				try {
//					fts2.add(this.featureType2.create(new Object[] {p, fi.getFloodingTime()}));
//				} catch (IllegalAttributeException e) {
//					e.printStackTrace();
//				}
//			}
////			fr.resetCache();
//		}
//		
		
		try {
			ShapeFileWriter.writeGeometries(fts, this.output);
//			ShapeFileWriter.writeGeometries(fts2, this.output2);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initFeatureType() {
		AttributeType[] attribs = new AttributeType[2];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("LineString",
				LineString.class, true, null, null, this.crs);
		attribs[1] = AttributeTypeFactory.newAttributeType("time",
				Integer.class);

		AttributeType[] attribs2 = new AttributeType[2];
		attribs2[0] = DefaultAttributeTypeFactory.newAttributeType("Point",
				Point.class, true, null, null, this.crs);
		attribs2[1] = AttributeTypeFactory.newAttributeType("time",
				Integer.class);		
		try {
			this.featureType = FeatureTypeBuilder.newFeatureType(attribs,"node");
			this.featureType2 = FeatureTypeBuilder.newFeatureType(attribs2,"node2");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		this.geofac = new GeometryFactory();
	}

//	public static void main(String[] args) {
//		 String netcdf = "../../inputs/flooding/flooding_old.sww";
////		String netcdf = "test/input/playground/gregor/data/flooding.sww";
//		String output = "../../analysis/mesh/floodLine.shp";
//		String output2 = "../../analysis/mesh/floodPoints.shp";
//		ScenarioImpl scenario = new ScenarioImpl();
//		scenario.getConfig().global().setCoordinateSystem("WGS84_UTM47S");
//		FloodLineShape fls = new FloodLineShape(netcdf, output, output2);
//		fls.run();
//	}

}
