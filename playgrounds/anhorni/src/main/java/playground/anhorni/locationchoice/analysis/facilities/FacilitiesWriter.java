package playground.anhorni.locationchoice.analysis.facilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Point;

public class FacilitiesWriter {
	
	private FeatureType featureType;

	public int[] write(List<ActivityFacilityImpl> facilities)  {
						
		this.initGeometries();
		ArrayList<Feature> features = new ArrayList<Feature>();	
		int numberOfShops[] = {0,0,0,0,0,0};
		
		Iterator<ActivityFacilityImpl> facilities_it = facilities.iterator();
		while (facilities_it.hasNext()) {
			ActivityFacilityImpl facility = facilities_it.next();
			Coord coord = facility.getCoord();
			
			
			if (facility.getActivityOptions().get("shop_retail_gt2500sqm") != null) {
				features.add(this.createFeature(coord, facility.getId(), "shop_retail_gt2500sqm"));
				numberOfShops[0]++;
			}
			else if (facility.getActivityOptions().get("shop_retail_get1000sqm") != null) {
				features.add(this.createFeature(coord, facility.getId(), "shop_retail_get1000sqm"));
				numberOfShops[1]++;
			}
			else if (facility.getActivityOptions().get("shop_retail_get400sqm") != null) {
				features.add(this.createFeature(coord, facility.getId(), "shop_retail_get400sqm"));
				numberOfShops[2]++;
			}
			else if (facility.getActivityOptions().get("shop_retail_get100sqm") != null) {
				features.add(this.createFeature(coord, facility.getId(), "shop_retail_get100sqm"));	
				numberOfShops[3]++;
			}
			else if (facility.getActivityOptions().get("shop_retail_lt100sqm") != null) {
				features.add(this.createFeature(coord, facility.getId(), "shop_retail_lt100sqm"));
				numberOfShops[4]++;
			}
			else if (facility.getActivityOptions().get("shop_other") != null) {
				features.add(this.createFeature(coord, facility.getId(), "shop_other"));
				numberOfShops[5]++;
			}		
		}
		try {
			if (!features.isEmpty()) {
				ShapeFileWriter.writeGeometries((Collection<Feature>)features, "output/zhFacilities.shp");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return numberOfShops;
	}


	private void initGeometries() {
		AttributeType [] attr = new AttributeType[3];
		attr[0] = AttributeTypeFactory.newAttributeType("Point", Point.class);
		attr[1] = AttributeTypeFactory.newAttributeType("ID", String.class);
		attr[2] = AttributeTypeFactory.newAttributeType("Type", String.class);
		
		try {
			this.featureType = FeatureTypeBuilder.newFeatureType(attr, "point");
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}
	
	private Feature createFeature(Coord coord, Id id, String type) {
		
		Feature feature = null;
		
		try {
			feature = this.featureType.create(new Object [] {MGC.coord2Point(coord),  id.toString(), type});
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}
		return feature;
	}
	
}
