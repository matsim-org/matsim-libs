package playground.anhorni.locationchoice.cs.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.ShapeFileWriter;
import com.vividsolutions.jts.geom.Point;
import playground.anhorni.locationchoice.cs.helper.ZHFacility;


public class ZHFacilitiesWriter {
	
	private FeatureType featureType;

	public void write(String outdir, TreeMap<Id, ArrayList<ZHFacility>> zhFacilitiesByLink)  {
						
		this.initGeometries();
		ArrayList<Feature> features = new ArrayList<Feature>();	
		ArrayList<Feature> featuresExact = new ArrayList<Feature>();	
		
		Iterator<ArrayList<ZHFacility>> facilities_it = zhFacilitiesByLink.values().iterator();
		while (facilities_it.hasNext()) {
			ArrayList<ZHFacility> facilitiesList = facilities_it.next();
			
			Iterator<ZHFacility> facilitiesList_it = facilitiesList.iterator();			
			while (facilitiesList_it.hasNext()) {
				ZHFacility facility = facilitiesList_it.next();	
				Coord coord = facility.getMappedposition();
				features.add(this.createFeature(coord, facility.getId()));
				featuresExact.add(this.createFeature(facility.getExactPosition(), facility.getId()));
			}
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
