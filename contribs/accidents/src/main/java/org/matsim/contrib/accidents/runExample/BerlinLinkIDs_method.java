package org.matsim.contrib.accidents.runExample;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.accidents.AccidentControlerListener;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;


/**
* @author mmayobre
*/

public class BerlinLinkIDs_method {
	private static final Logger log = Logger.getLogger(AccidentControlerListener.class);
	
	String configFile = "./data/input/be_251/config.xml";
	Config config = ConfigUtils.loadConfig(configFile);
	Scenario scenario = ScenarioUtils.loadScenario(config);
	
	public void selectBerlinLinkIDs(){
		
		log.info("Reading file...");
		
		Map<String, SimpleFeature> popDensityFeatures = new HashMap<>();
		Map<String, Double> popDensityData = new HashMap<>();
		
		SimpleFeatureSource ftsPlaces = ShapeFileReader.readDataFile("./data/input/osmBerlin/gis.osm_places_a_free_1_GK4.shp");
		try (SimpleFeatureIterator itPlaces = ftsPlaces.getFeatures().features()){
			while (itPlaces.hasNext()){
				SimpleFeature ftPlaces = itPlaces.next();
				String osmId = ftPlaces.getAttribute("osm_id").toString();
				double popDensity = Double.parseDouble(ftPlaces.getAttribute("pop_dens").toString());
				popDensityFeatures.put(osmId, ftPlaces);
				popDensityData.put(osmId, popDensity);
			}
			itPlaces.close();
			DataStore ds = (DataStore) ftsPlaces.getDataStore();
			ds.dispose();
			log.info("Reading shp file for population density... Done.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.info("Writing LinkIDs of Berlin in a csv-file...");
		File fileBerlinLinkIDs = new File("./data/output/be_251/berlin_linkIDs.csv");
		FileWriter fw;
		BufferedWriter bw = null;
		try {
			fw = new FileWriter(fileBerlinLinkIDs);
			bw = new BufferedWriter(fw);
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (Link link : this.scenario.getNetwork().getLinks().values()) {
			CoordinateTransformation ctScenarioCRS2osmCRS = TransformationFactory.getCoordinateTransformation(this.scenario.getConfig().global().getCoordinateSystem(), "EPSG:31468");
			Coord linkCoordinateTransformedToOSMCRS = ctScenarioCRS2osmCRS.transform(link.getCoord());
			Point p = MGC.xy2Point(linkCoordinateTransformedToOSMCRS.getX(), linkCoordinateTransformedToOSMCRS.getY()); 
		
			for (SimpleFeature feature : popDensityFeatures.values()) {
				if (((Geometry) feature.getDefaultGeometry()).contains(p)) {
					try {
						bw.write(link.getId().toString());
						bw.write(";");
						//bw.write("Kante liegt in Berlin");
						//bw.write(";");
						bw.newLine();
						break;
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				}
			}
		}
		try {
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Writing LinkIDs of Berlin in a csv-file! -----------> DONE!");
	}
}	
