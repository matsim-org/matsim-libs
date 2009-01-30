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
import org.matsim.basic.v01.Id;
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
		
		Iterator<ArrayList<ZHFacility>> facilities_it = zhFacilitiesByLink.values().iterator();
		while (facilities_it.hasNext()) {
			ArrayList<ZHFacility> facilitiesList = facilities_it.next();
			
			Iterator<ZHFacility> facilitiesList_it = facilitiesList.iterator();			
			while (facilitiesList_it.hasNext()) {
				ZHFacility facility = facilitiesList_it.next();	
				Coord coord = facility.getCenter();
				Feature feature = this.createFeature(coord);
				features.add(feature);
			}
		}
		try {
			if (!features.isEmpty()) {
				ShapeFileWriter.writeGeometries((Collection<Feature>)features, outdir + "facilitiesMapped2Net.shp");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void initGeometries() {
		AttributeType [] attr = new AttributeType[1];
		attr[0] = AttributeTypeFactory.newAttributeType("Point", Point.class);
		
		try {
			this.featureType = FeatureTypeBuilder.newFeatureType(attr, "point");
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}
	
	private Feature createFeature(Coord coord) {
		
		Feature feature = null;
		
		try {
			feature = this.featureType.create(new Object [] {MGC.coord2Point(coord)});
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}
		return feature;
	}
	
}
