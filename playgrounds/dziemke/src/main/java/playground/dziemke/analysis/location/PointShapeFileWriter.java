package playground.dziemke.analysis.location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

public class PointShapeFileWriter {
	// Other objects
	private static PointFeatureFactory pointFeatureFactory;
	
	
	public static <T> void writeShapeFilePoints(String outputShapeFile, Map <Id<T>,Coord> coords, String attributeLabel) {
		if (coords.isEmpty()==true) {
			System.out.println("Map ist leer!");
		} else {
			initFeatureType(attributeLabel);
			Collection <SimpleFeature> features = createFeatures(coords);
			ShapeFileWriter.writeGeometries(features, outputShapeFile);
			System.out.println("ShapeFile with points wrtitten to " + outputShapeFile);
		}
	}

	
	private static void initFeatureType(String attributeLabel) {
		// Before single feature can be created, the type has to be initialized here
		
		// Via "addAttribute" a attributes of the feature type can be added and its name specified.
		// The value for this attribute can then be filled in when a single attribute of this type is created.
		
		// The effect of "setName" could not be retrieved yet.
		
		new PointFeatureFactory.Builder().
		setCrs(MGC.getCRS(TransformationFactory.DHDN_GK4)).
		setName("points").
		addAttribute(attributeLabel, String.class).
		//addAttribute("Attribute2", String.class).
		create();
	}	
	
	
	private static <T> Collection <SimpleFeature> createFeatures(Map<Id<T>,Coord> coords) {
		List <SimpleFeature> features = new ArrayList <SimpleFeature>();
		for (Id<?> id : coords.keySet()){
			Coord coord = coords.get(id);
			//features.add(getFeature(coords.get(i), i));
			Object[] attributes = new Object[]{id};
			//Object[] attributes = new Object[2];
			//attributes[1] = i;
			SimpleFeature feature = pointFeatureFactory.createPoint(coord, attributes, null);
			features.add(feature);
		}
		return features;
	}	

}
