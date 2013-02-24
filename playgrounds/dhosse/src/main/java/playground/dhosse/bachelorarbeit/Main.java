package playground.dhosse.bachelorarbeit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.matsim4opus.constants.InternalConstants;
import org.matsim.contrib.matsim4opus.gis.GridUtils;
import org.matsim.contrib.matsim4opus.gis.SpatialGrid;
import org.matsim.contrib.matsim4opus.gis.Zone;
import org.matsim.contrib.matsim4opus.gis.ZoneLayer;
import org.matsim.contrib.matsim4opus.utils.network.NetworkBoundaryBox;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

public class Main {
	
	private static SimpleFeatureBuilder builder;
	static ZoneLayer<Id> measuringPoints;
	
	public static void main(String args[]) {
		
//		String file1 = "C:/Users/Daniel/Dropbox/bsc/input/config.xml";
		String path = "C:/Users/Daniel/Dropbox/bsc/input";
		String file1 = "berlin_fis";
		String file2 = "berlin_osm";
		String file3 = "berlin_osm_main";
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		MatsimPopulationReader pr = new MatsimPopulationReader(scenario);
		MatsimNetworkReader nr = new MatsimNetworkReader(scenario);
		nr.readFile(path+"/"+file2+".xml");
		pr.readFile(path+"/"+"/test_population.xml");
		
		NetworkBoundaryBox bbox = new NetworkBoundaryBox();
		bbox.setDefaultBoundaryBox(scenario.getNetwork());
		
		measuringPoints = GridUtils.createGridLayerByGridSizeByNetwork(200, 
				   bbox.getBoundingBox());
		SpatialGrid freeSpeedGrid = new SpatialGrid(bbox.getBoundingBox(), 200);
		
//		initFeatureType();
//		Collection<SimpleFeature> features = createFeatures();
//		ShapeFileWriter.writeGeometries(features, "C:/Users/Daniel/Dropbox/bsc/pres/measuringPoints.shp");
			
		InternalConstants.setOpusHomeDirectory("C:/Users/Daniel/Dropbox/bsc");
		
		AccessibilityCalc ac = new AccessibilityCalc(/*parcels,*/ measuringPoints, freeSpeedGrid, (ScenarioImpl) scenario, file2);
		ac.runAccessibilityComputation();
		
//		NetworkInspector ni = new NetworkInspector(scenario);
//		if(ni.isRoutable())
//			System.out.println("Netzwerk ist routbar...");
//		else
//			System.out.println("Netzwerk ist nicht routbar");
//		ni.checkLinkAttributes();
//		ni.checkNodeAttributes();
//		ni.shpExportNodeStatistics(ni.getExitRoadNodes());
		
		//TODO: methode isRoutable nochmal umschreiben (s.u.), unterscheidung bei node degree 1: einbahnstraße oder sackgasse?, dimension des untersuchungsgebiets ausgeben lassen
		//TODO: wenn sackgasse: sackgasse, weil se aus dem untersuchungsgebiet rausführt oder "echte" sackgasse
		//TODO: eigenen!!! controlerListener schreiben, ohne vererbung und pipapo,accessibility berechnung soll auch OHNE simulation, OHNE population möglich sein
		//TODO: WAS MACHT DER PARCELBASEDACCESSIBILITYCONTROLERLISTENER GENAU???
		
	}
	
//	private static Collection<SimpleFeature> createFeatures() {
//		List<SimpleFeature> features = new ArrayList<SimpleFeature>();
//		Iterator<Zone<Id>> it = measuringPoints.getZones().iterator();
//		while(it.hasNext()){
//			Zone<Id> zone = it.next();
//			features.add(getFeature(zone));
//		}
//		return features;
//	}
//
//	private static SimpleFeature getFeature(final Zone<Id> zone) {
//		
//		Point p = MGC.coordinate2Point(zone.getGeometry().getCoordinate());
//		
//		try {
//			return builder.buildFeature(null, new Object[]{p});
//		} catch (IllegalArgumentException e) {
//			throw new RuntimeException(e);
//		}
//	}
//
//	private static void initFeatureType() {
//		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
//		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
//		typeBuilder.setName("node");
//		typeBuilder.setCRS(crs);
//		typeBuilder.add("location",Point.class);
//		builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
//		
//	}

}