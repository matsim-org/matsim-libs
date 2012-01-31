package playground.gregor.gis.buildinglinkmapping;

import java.util.ArrayList;
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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.evacuation.base.Building;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.MultiPolygon;

public class BuildingLinkMapping {
	
	private static FeatureType ft;


	public static void main(String [] args) {
		String conf = "/Users/laemmel/arbeit/papers/2012/lastMile/matsim/config.xml";
		Config c = ConfigUtils.loadConfig(conf);
		Scenario sc = ScenarioUtils.loadScenario(c);
		Loader loader = new Loader(sc);
		loader.loadData();
		List<Building> buildings = loader.getBuildings();
		System.out.println(buildings.size());
		initFeatures();
		
		List<Feature> features = new ArrayList<Feature>();
		for (Building b : buildings) {
			Coord cc = MGC.point2Coord(b.getGeo().getCentroid());
			LinkImpl l = ((NetworkImpl)sc.getNetwork()).getNearestLink(cc);
			try {
				
	
				
				while(l.getId().toString().contains("s")) {
					l = (LinkImpl) l.getFromNode().getInLinks().values().iterator().next();
				}
				String srtId = l.getId().toString();
				if (srtId.contains("e")) {
					continue;
				}
				int intId = Integer.parseInt(srtId);
				if (intId > 100000) {
					intId -= 100000;
				}
				
				Feature f = ft.create(new Object[]{b.getGeo(),0.,Integer.toString(intId)});
				features.add(f);
			} catch (IllegalAttributeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			
		}
		
		ShapeFileWriter.writeGeometries(features, "/Users/laemmel/arbeit/papers/2012/lastMile/matsim/buildings.shp");
//		((NetworkImpl)sc.getNetwork()).getNearestLink(coord)
	}
	
	
	private static void initFeatures() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG: 32747");
		AttributeType p = DefaultAttributeTypeFactory.newAttributeType(
				"MultiPolygon", MultiPolygon.class, true, null, null, targetCRS);
		AttributeType z = AttributeTypeFactory.newAttributeType(
				"dblAvgZ", Double.class);
		AttributeType t = AttributeTypeFactory.newAttributeType(
				"name", String.class);

		Exception ex;
		try {
			ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { p, z, t }, "MultiPolygon");
			return;
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);

	}

}
