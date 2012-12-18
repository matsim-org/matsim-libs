package playground.droeder.southAfrica.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.vsp.analysis.VspAnalyzer;
import playground.vsp.analysis.modules.AbstractAnalyisModule;
import playground.vsp.analysis.modules.act2mode.ActivityToModeAnalysis;
import playground.vsp.analysis.modules.boardingAlightingCount.BoardingAlightingCountAnalyzer;
import playground.vsp.analysis.modules.ptAccessibility.PtAccessibility;
import playground.vsp.analysis.modules.ptPaxVolumes.PtPaxVolumesAnalyzer;
import playground.vsp.analysis.modules.ptPaxVolumes.PtPaxVolumesHandler;
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
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().scenario().setUseTransit(true);
		
		
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
		
		
		Set<String> ptModes = new HashSet<String>(){{
			add("taxi");
			add("bus");
			add("pt");
		}};
		
		Set<String> networkModes = new HashSet<String>(){{
			add("car");
			add("ride");
		}};
		Set<Feature> features = new ShapeFileReader().readFileAndInitialize(dir.getOutputPath() + "/cordon.shp");
		
		Map<String, Geometry> zones =  new HashMap<String, Geometry>();
		for(Feature f: features){
			zones.put((String)f.getAttribute(2), (Geometry) f.getAttribute(0));
		}
//		
		
		
		GetStuckEventsAndPlans writeStuck = new GetStuckEventsAndPlans(sc);
		PtAccessibility ptAcces = new PtAccessibility(sc, cluster, 9, activityCluster);
		
		TTtripAnalysis tripAna = new TTtripAnalysis(ptModes, networkModes, sc.getPopulation());
		tripAna.addZones(zones);

		BoardingAlightingCountAnalyzer boardingAlightingCountAnalyzes = 
					new BoardingAlightingCountAnalyzer(sc, 3600);
		boardingAlightingCountAnalyzes.setWriteHeatMaps(true, Integer.valueOf(args[3]));
		TransitVehicleVolumeAnalyzer ptVehVolAnalyzer = new TransitVehicleVolumeAnalyzer(sc, 3600.);
		TransitSchedule2Shp shp = new TransitSchedule2Shp(sc);
		ActivityToModeAnalysis atm = new ActivityToModeAnalysis(sc, null, 3600);
		PtPaxVolumesAnalyzer ptVolAna = new PtPaxVolumesAnalyzer(sc, 3600.);
		
		
		
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
		analyzer.addAnalysisModule(new MyPtCount());
		

		analyzer.run();
	}
	
	
	
}

class MyPtCount extends AbstractAnalyisModule{

	PtPaxVolumesHandler handler;
	private ArrayList<Id> links;
	/**
	 * @param name
	 */
	public MyPtCount() {
		super(MyPtCount.class.getSimpleName());
		this.handler = new PtPaxVolumesHandler(3600.); 
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new ArrayList<EventHandler>();
		handler.add(this.handler);
		return handler;
	}

	@Override
	public void preProcessData() {
		this.links = new ArrayList<Id>();
		links.add(new IdImpl("90409-90411-90413-90415-90417-90419"));
		links.add(new IdImpl("90420-90418-90416-90414-90412-90410"));
		links.add(new IdImpl("20706-20707"));
		links.add(new IdImpl("72219-72220-72221"));
		links.add(new IdImpl("72241-72242-72243-72244"));
		links.add(new IdImpl("20726-20727-20728"));
		links.add(new IdImpl("24360-24361-24362-24363-24364"));
		links.add(new IdImpl("218-219-220-221-222"));
		links.add(new IdImpl("34580-34581-34582-34583-34584"));
		links.add(new IdImpl("73503-73504"));
		links.add(new IdImpl("53096-53097-53098"));
		links.add(new IdImpl("78332-78333-78334"));
		links.add(new IdImpl("18607-18605-18603-18601-18599-18597-18595-18593-18591-18589-18587-18585-18583-18581-18579-18577"));
		links.add(new IdImpl("18576-18578-18580-18582-18584-18586-18588-18590-18592-18594-18596-18598-18600-18602-18604"));
	}

	@Override
	public void postProcessData() {
		
	}

	@Override
	public void writeResults(String outputFolder) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder + "ptPaxVolumes.csv");
		try {
			//header
			writer.write("LinkId;total;");
			for(int i = 0; i < this.handler.getMaxInterval() + 1; i++){
					writer.write(String.valueOf(i) + ";");
			}
			writer.newLine();
			//content
			for(Id id: this.links){
				writer.write(id.toString() + ";");
				writer.write(this.handler.getPaxCountForLinkId(id) + ";");
				for(int i = 0; i < this.handler.getMaxInterval() + 1; i++){
					writer.write(this.handler.getPaxCountForLinkId(id, i) + ";");
				}
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}



