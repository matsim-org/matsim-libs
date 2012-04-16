package playground.tnicolai.matsim4opus.utils.misc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.matsim4opus.gis.EuclideanDistance;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.utils.io.ReadFromUrbanSimModel;

import com.vividsolutions.jts.geom.Point;

public class Shoot {
	
	private static final Logger log = Logger.getLogger(Shoot.class);
	
	private static final String shapeFile = "/Users/thomas/Development/opus_home/data/zurich_parcel/shapefiles/zone.shp";
	private static final String networkFile = "/Users/thomas/Development/opus_home/data/zurich_parcel/data/data/network/ivtch-changed.xml";
	private static final int year = 2000;
	private static final double radius = 200.; // meter
	private static long id;
	
	/**
	 * This is a sample method to spread UrbanSim "home" or "work" locations within their assigned zone.
	 * 
	 * 
	 * @param args
	 */
	public static void main(String args[]){
		try {
			Constants.MATSIM_4_OPUS_TEMP = "/Users/thomas/Development/opus_home/matsim4opus/tmp/";
			BufferedWriter writer = IOUtils.getBufferedWriter(Constants.MATSIM_4_OPUS_TEMP + "shootTest.csv");
			writer.write("zone_id, x, y, counter");
			writer.newLine();
			
			Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			((NetworkConfigGroup)scenario.getConfig().getModule(NetworkConfigGroup.GROUP_NAME)).setInputFile(networkFile);
			ScenarioUtils.loadScenario(scenario);
			
			boolean isShapeFileApproach = true; // switch here
			
			if(!isShapeFileApproach)
				simpleApproach(writer);		// use this if no shape file available -> shoots within a given radius of the zone centroid
			else
				shapeFileApproach(writer); // use this if shape file is available -> shoots within feature (zone) boundaries. (more accurate)
			
			writer.flush();
			writer.close();
			log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void simpleApproach(BufferedWriter writer)
			throws IOException {
		
		ReadFromUrbanSimModel readFromUrbanSim = new ReadFromUrbanSimModel(year);
		ActivityFacilitiesImpl parcels = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
		ActivityFacilitiesImpl zones   = new ActivityFacilitiesImpl("urbansim zones");
		readFromUrbanSim.readFacilitiesParcel(parcels, zones);	
		
		Iterator<ActivityFacility> zoneIterator = zones.getFacilities().values().iterator();
		Random rnd = new Random();
		Id testID = new IdImpl(610);
		
		while(zoneIterator.hasNext()){
			ActivityFacility zone = zoneIterator.next();
			Coord zoneCoordinate = zone.getCoord();
			
//			if(zone.getId().equals(testID)){
			for(int i = 0; i < 10; i++){ // creates 10 test shoots per zone
				Coord c = getRandomPointinRadius(rnd, zoneCoordinate, radius);
				log.info(String.valueOf(zone.getId()) + "," + String.valueOf(c.getX()) + "," + String.valueOf(c.getY()) + "," + String.valueOf(i));
				writer.write(String.valueOf(zone.getId()) + "," + String.valueOf(c.getX()) + "," + String.valueOf(c.getY()) + "," + String.valueOf(i));
				writer.newLine();
			}
//			}
		}
	}
	
	private static Coord getRandomPointinRadius(Random rnd, Coord zoneCoordinate, double radius){
		Coord p = null;
		double x, y;
		double distance;
		do {
			x = (zoneCoordinate.getX() - radius) + (rnd.nextDouble() * 2 * radius);
			y = (zoneCoordinate.getY() - radius) + (rnd.nextDouble() * 2 * radius);
			p = new CoordImpl(x, y);
			
			distance = EuclideanDistance.getEuclidianDistance(zoneCoordinate, p);
			
		} while ( distance > radius );
		return p;
	}

	private static void shapeFileApproach(BufferedWriter writer)
			throws IOException {
		
		FeatureSource fts = ShapeFileReader.readDataFile(shapeFile); //reads the shape file in
		Random rnd = new Random();

		Iterator<Feature> it = fts.getFeatures().iterator();
		Map<Id, Feature> featureMap = new HashMap<Id, Feature>();
		Point p;
		while (it.hasNext()) {
			
			Feature feature = it.next();
			id = (Long) feature.getAttribute("ZONE_ID");
			featureMap.put(new IdImpl(id), feature);

			for(int i = 0; i < 10; i++){ // creates 10 test shoots per zone
				p = getRandomPointInFeature(rnd, feature);
				log.info(String.valueOf(id) + "," + String.valueOf(p.getX()) + "," + String.valueOf(p.getY()) + "," + String.valueOf(i));
				writer.write(String.valueOf(id) + "," + String.valueOf(p.getX()) + "," + String.valueOf(p.getY()) + "," + String.valueOf(i));
				writer.newLine();
			}
		}
		
		// testing
		ReadFromUrbanSimModel readFromUrbanSim = new ReadFromUrbanSimModel(year);
		ActivityFacilitiesImpl parcels = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
		ActivityFacilitiesImpl zones   = new ActivityFacilitiesImpl("urbansim zones");
		readFromUrbanSim.readFacilitiesParcel(parcels, zones);	
		
		Iterator<ActivityFacility> zoneIterator = zones.getFacilities().values().iterator();
		while(zoneIterator.hasNext()){
			ActivityFacility zone = zoneIterator.next();
			Feature f = featureMap.get( zone.getId() );
			Id test = new IdImpl((Long)f.getAttribute("ZONE_ID"));
			
			if(zone.getId().compareTo(test) != 0)
				System.out.println("Not equal zone:" + zone.getId() + ", feature:" + f.getAttribute("ZONE_ID"));
		}
	}
	
	private static Point getRandomPointInFeature(Random rnd, Feature feature) {
		Point p = null;
		double x, y;
		do {
			x = feature.getBounds().getMinX() + rnd.nextDouble() * (feature.getBounds().getMaxX() - feature.getBounds().getMinX());
			y = feature.getBounds().getMinY() + rnd.nextDouble() * (feature.getBounds().getMaxY() - feature.getBounds().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!feature.getDefaultGeometry().contains(p));
		return p;
	}

}
