package playground.anhorni.choiceSetGeneration.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;

import playground.anhorni.choiceSetGeneration.helper.ZHFacilities;
import playground.anhorni.choiceSetGeneration.helper.ZHFacility;

import com.vividsolutions.jts.geom.Point;


public class ZHFacilitiesWriter {
	
	private FeatureType featureType;

	public void write(String outdir, ZHFacilities facilities)  {
						
		this.initGeometries();
		ArrayList<Feature> features = new ArrayList<Feature>();	
		ArrayList<Feature> featuresExact = new ArrayList<Feature>();	
		
		Iterator<ZHFacility> facilities_it = facilities.getZhFacilities().values().iterator();
		while (facilities_it.hasNext()) {
			ZHFacility facility = facilities_it.next();
			Coord coord = facility.getMappedPosition();
			features.add(this.createFeature(coord, facility.getId()));
			featuresExact.add(this.createFeature(facility.getExactPosition(), facility.getId()));
		}
		try {
			if (!features.isEmpty()) {
				ShapeFileWriter.writeGeometries((Collection<Feature>)features, outdir +"/shapefiles/zhFacilitiesPositionMapped2Net.shp");
				ShapeFileWriter.writeGeometries((Collection<Feature>)featuresExact, outdir +"/shapefiles/zhFacilitiesExactPosition.shp");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void initGeometries() {
		AttributeType [] attr = new AttributeType[2];
		attr[0] = AttributeTypeFactory.newAttributeType("Point", Point.class);
		attr[1] = AttributeTypeFactory.newAttributeType("ID", String.class);
		
		try {
			this.featureType = FeatureTypeBuilder.newFeatureType(attr, "point");
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}
	
	private Feature createFeature(Coord coord, Id id) {
		
		Feature feature = null;
		
		try {
			feature = this.featureType.create(new Object [] {MGC.coord2Point(coord),  id.toString()});
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}
		return feature;
	}
	
}
