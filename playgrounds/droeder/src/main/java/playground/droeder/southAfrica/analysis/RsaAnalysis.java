package playground.droeder.southAfrica.analysis;

import java.io.File;
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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.vsp.analysis.modules.ptAccessibility.PtAccesibilityMain;
import playground.vsp.analysis.modules.ptTripAnalysis.traveltime.TTtripAnalysis;

import com.vividsolutions.jts.geom.Geometry;



public class RsaAnalysis {

	/**
	 * 
	 * @param args OutputDir RunId iteration
	 */
	public static void main(String[] args) {
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().scenario().setUseTransit(true);
		
		
		OutputDirectoryHierarchy dir = new OutputDirectoryHierarchy(args[0] + "/" + args[1] + "/", 
				args[1], true, true);
//		new TransitScheduleReader(sc).readFile(dir.getOutputFilename("0.transitSchedule.xml.gz"));
//		new MatsimNetworkReader(sc).readFile(dir.getOutputFilename(Controler.FILENAME_NETWORK));
//		new MatsimPopulationReader(sc).readFile(dir.getOutputFilename(Controler.FILENAME_POPULATION));
		
		new TransitScheduleReader(sc).readFile(dir.getIterationFilename(Integer.parseInt(args[2]), "transitSchedule.xml.gz"));
		new MatsimNetworkReader(sc).readFile(dir.getOutputFilename(Controler.FILENAME_NETWORK));
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
		
		PtAccesibilityMain ptAcces = new PtAccesibilityMain(sc, cluster, activityCluster);
		ptAcces.preProcessData();
		ptAcces.postProcessData();
		new File(dir.getOutputPath() + "/ptAcces/").mkdirs();
		ptAcces.writeResults(dir.getOutputPath() + "/ptAcces/");
		
		
		Set<String> ptModes = new HashSet<String>(){{
			add("taxi");
			add("bus");
			add("pt");
		}};
		
		Set<String> networkModes = new HashSet<String>(){{
			add("car");
			add("ride");
		}};
		
		
		TTtripAnalysis tripAna = new TTtripAnalysis(ptModes, networkModes, sc.getPopulation());
		Set<Feature> features = new ShapeFileReader().readFileAndInitialize(dir.getOutputPath() + "/possibleRoutes.shp");
		
		Map<String, Geometry> zones =  new HashMap<String, Geometry>();
		for(Feature f: features){
			zones.put(String.valueOf(f.getAttribute(1)), (Geometry) f.getAttribute(0));
		}
		
		tripAna.addZones(zones);
		tripAna.preProcessData();
		
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(tripAna.getEventHandler().get(0));
		
		new MatsimEventsReader(manager).readFile(dir.getIterationFilename(Integer.parseInt(args[2]), Controler.FILENAME_EVENTS_XML));
		tripAna.writeResults(dir.getOutputPath() + "/tripAna/");
		
	}
	
}


