package playground.droeder.southAfrica.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.vsp.analysis.VspAnalyzer;
import playground.vsp.analysis.modules.act2mode.ActivityToModeAnalysis;
import playground.vsp.analysis.modules.boardingAlightingCount.BoardingAlightingCountAnalyzer;
import playground.vsp.analysis.modules.ptAccessibility.PtAccessibility;
import playground.vsp.analysis.modules.ptPaxVolumes.PtPaxVolumesAnalyzer;
import playground.vsp.analysis.modules.ptTripAnalysis.traveltime.TTtripAnalysis;
import playground.vsp.analysis.modules.stuckAgents.GetStuckEventsAndPlans;
import playground.vsp.analysis.modules.transitSchedule2Shp.TransitSchedule2Shp;
import playground.vsp.analysis.modules.transitVehicleVolume.TransitVehicleVolumeAnalyzer;

import com.vividsolutions.jts.geom.Geometry;



public class RsaAnalysis {

	/**
	 * 
	 * @param args OutputDir RunId iteration nrOfHeatMapTiles
	 */
	public static void main(String[] args) {
		
		String targetCoordinateSystem = TransformationFactory.WGS84_UTM35S; // Gauteng
		
		Set<String> ptModes = new HashSet<String>(){{
			add("taxi");
//			add("bus");
			add("pt");
		}};
		
		Set<String> networkModes = new HashSet<String>(){{
			add("car");
			add("ride");
		}};
		
		
		Config c = ConfigUtils.createConfig();
		c.scenario().setUseTransit(true);
		c.transit().setTransitModes(ptModes);
		
		Scenario sc = ScenarioUtils.createScenario(c);
		((PopulationFactoryImpl) sc.getPopulation().getFactory()).setRouteFactory("taxi", new ExperimentalTransitRouteFactory());
		((PopulationFactoryImpl) sc.getPopulation().getFactory()).setRouteFactory("pt", new ExperimentalTransitRouteFactory());
		
		OutputDirectoryHierarchy dir = new OutputDirectoryHierarchy(args[0] + "/" + args[1] + "/", 
				args[1], true, true);
//		new TransitScheduleReader(sc).readFile(dir.getOutputFilename("0.transitSchedule.xml.gz"));
//		new MatsimNetworkReader(sc).readFile(dir.getOutputFilename(Controler.FILENAME_NETWORK));
//		new MatsimPopulationReader(sc).readFile(dir.getOutputFilename(Controler.FILENAME_POPULATION));
		
		new TransitScheduleReader(sc).readFile(dir.getIterationFilename(Integer.parseInt(args[2]), "transitScheduleScored.xml.gz"));
		new MatsimNetworkReader(sc).readFile(dir.getOutputFilename(Controler.FILENAME_NETWORK));
		new MatsimFacilitiesReader((ScenarioImpl) sc).readFile(dir.getOutputFilename("output_facilities.xml.gz"));
		new MatsimPopulationReader(sc).readFile(dir.getIterationFilename(Integer.parseInt(args[2]), "plans.xml.gz"));
		
		List<Integer> cluster = new ArrayList<Integer>(){{
			add(100);
			add(200);
			add(400);
			add(600);
			add(800);
			add(1000);
			add(1500);
			add(2000);
			add(3000);
			add(4000);
			add(5000);
		}};
		
		SortedMap<String, List<String>> activityCluster = new TreeMap<String, List<String>>();
		List<String> activities = new ArrayList<String>();
		
		activities.add("h11");
		activities.add("h12");
		activities.add("h13");
		activities.add("h14");
		activities.add("h15");
		activities.add("h21");
		activities.add("h22");
		activities.add("h23");
		activities.add("h24");
		activities.add("h25");
		activities.add("h31");
		activities.add("h32");
		activities.add("h33");
		activities.add("h34");
		activities.add("h35");
		activities.add("h");
		activityCluster.put("home", activities);
		
		activities = new ArrayList<String>();
		activities.add("w1");
		activities.add("w2");
		activities.add("w3");
		activities.add("w4");
		activities.add("w5");
		activities.add("w");
		activityCluster.put("work", activities);
		
		activities = new ArrayList<String>();
		activities.add("e1");
		activities.add("e2");
		activities.add("e21");
		activities.add("e22");
		activities.add("e23");
		activities.add("e24");
		activities.add("e25");
		activities.add("e3");
		activityCluster.put("edu", activities);
		
		activities = new ArrayList<String>();
		activities.add("l1");
		activities.add("l2");
		activities.add("l3");
		activities.add("l4");
		activities.add("l5");
		activityCluster.put("leisure", activities);	
		
		activities = new ArrayList<String>();
		activities.add("s1");
		activities.add("S2");
		activities.add("s3");
		activities.add("s4");
		activities.add("s5");
		activityCluster.put("shopping", activities);	
		
		
		
		Set<Feature> features = new ShapeFileReader().readFileAndInitialize(dir.getOutputPath() + "/cordon.shp");
		
		Map<String, Geometry> zones =  new HashMap<String, Geometry>();
		for(Feature f: features){
			zones.put((String)f.getAttribute(2), (Geometry) f.getAttribute(0));
		}
//		
		
		
		GetStuckEventsAndPlans writeStuck = new GetStuckEventsAndPlans(sc);
		PtAccessibility ptAcces = new PtAccessibility(sc, cluster, 36, activityCluster, targetCoordinateSystem);
		
		TTtripAnalysis tripAna = new TTtripAnalysis(ptModes, networkModes, sc.getPopulation());
		tripAna.addZones(zones);

		BoardingAlightingCountAnalyzer boardingAlightingCountAnalyzes = 
					new BoardingAlightingCountAnalyzer(sc, 3600, targetCoordinateSystem);
		boardingAlightingCountAnalyzes.setWriteHeatMaps(true, Integer.valueOf(args[3]));
		TransitVehicleVolumeAnalyzer ptVehVolAnalyzer = new TransitVehicleVolumeAnalyzer(sc, 3600., targetCoordinateSystem);
		TransitSchedule2Shp shp = new TransitSchedule2Shp(sc, targetCoordinateSystem);
		ActivityToModeAnalysis atm = new ActivityToModeAnalysis(sc, null, 3600, targetCoordinateSystem);
		PtPaxVolumesAnalyzer ptVolAna = new PtPaxVolumesAnalyzer(sc, 3600., targetCoordinateSystem);
		
		
		
		VspAnalyzer analyzer = new VspAnalyzer(dir.getOutputPath(), 
								dir.getIterationFilename(Integer.parseInt(args[2]), Controler.FILENAME_EVENTS_XML));
		analyzer.addAnalysisModule(writeStuck);
		analyzer.addAnalysisModule(ptAcces);
//		analyzer.addAnalysisModule(waitAna);
		analyzer.addAnalysisModule(tripAna);
		analyzer.addAnalysisModule(boardingAlightingCountAnalyzes);
		analyzer.addAnalysisModule(ptVehVolAnalyzer);
		analyzer.addAnalysisModule(shp);
		analyzer.addAnalysisModule(atm);
		analyzer.addAnalysisModule(ptVolAna);
		analyzer.addAnalysisModule(new NmbmPtCount());
		analyzer.addAnalysisModule(new PtAgentStuckReasons(sc));
		

		analyzer.run();
	}
	
	
	
}





