package playground.gregor.gis.centroid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

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
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Point;

public class CentroidFromPolygon {

	private final String input;
	private final String output;
	private ArrayList<Feature> features;
	private FeatureType ftRunCompare;

	public CentroidFromPolygon(String input, String output) {
		this.input = input;
		this.output = output;
	}
	
	public void run() throws IOException {
		initFeatures();
		FeatureSource fts = ShapeFileReader.readDataFile(this.input);
		
		Iterator it = fts.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			Point p = ft.getDefaultGeometry().getCentroid();
			try {
				this.features.add(this.ftRunCompare.create(new Object[]{p,ft.getAttribute("LAND_USE")}));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
		}

		ShapeFileWriter.writeGeometries(this.features, this.output);
		
	}
	
	private void initFeatures() {
		this.features = new ArrayList<Feature>();
		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, crs);
		AttributeType risk = AttributeTypeFactory.newAttributeType("LANDUSE", String.class);
		
		try {
			this.ftRunCompare = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, risk}, "LANDUSE");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}

	}

	public static void main(String [] args) {
		String input = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/gis/buildings_v20100315/raw_buildings_v20100315.shp";
		String output = "/home/laemmel/devel/tmp/buildings_as_points_v20100331.shp";
		try {
			new CentroidFromPolygon(input,output).run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
