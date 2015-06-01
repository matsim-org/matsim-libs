package playground.dhosse.cl;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.agarwalamit.munich.analysis.ActDurationDiffDistribution;
import playground.vsp.analysis.modules.legModeDistanceDistribution.LegModeDistanceDistribution;

public class RunCL {
	
	public static void main(String args[]){
		
		String svnWorkingDir = "C:/Users/Daniel/Documents/work/shared-svn/studies/countries/cl/";
		String workingDirInputFiles = svnWorkingDir + "Kai_und_Daniel/";
		String boundariesInputDir = workingDirInputFiles + "exportedBoundaries/";
		String databaseFilesDir = workingDirInputFiles + "exportedFilesFromDatabase/";
		String visualizationsDir = workingDirInputFiles + "Visualisierungen/";
		String matsimInputDir = workingDirInputFiles + "inputFiles/";
		
		String transitFilesDir = svnWorkingDir + "/santiago_pt_demand_matrix/pt_stops_schedule_2013/";
		
//		OTFVis.playConfig(matsimInputDir + "config.xml");
//		OTFVis.playMVI(matsimInputDir + "output/ITERS/it.0/0.otfvis.mvi");
		
//		Config config = ConfigUtils.createConfig();
//		ConfigUtils.loadConfig(config, matsimInputDir + "config.xml");
//		Scenario scenario = ScenarioUtils.loadScenario(config);
		
//		double[] box = NetworkUtils.getBoundingBox(scenario.getNetwork().getNodes().values());
//		System.out.println("(" + box[0] + "," + box[1] + "),(" + box[2] + "," + box[3] + ")");
//		Controler controler = new Controler(scenario);
//		controler.setOverwriteFiles(true);
//		controler.run();
		
//		OTFVis.playScenario(scenario);
		
//		new NetConverter().plans2Shape(scenario.getPopulation(), visualizationsDir + "activities.shp");
		
//		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new NetworkReaderMatsimV1(scenario).parse(matsimInputDir + "secondary.xml.gz");
		
		//coordinate conversion from EPSG:3857 to EPSG:32719
//		new NetConverter().convertCoordinates(scenario.getNetwork(), matsimInputDir + "santiago_tiny_19S.xml");
		
		//for conversion of links into shape file
//		System.out.println(scenario.getNetwork().getLinks().size());
//		System.out.println(config.network().getInputFile());
//		new NetConverter().convertNet2Shape(scenario.getNetwork(), "EPSG:32719", visualizationsDir + "santiago_secondary.shp");
		
		//for conversion of raw datas into matsim plans
		CSVToPlans converter = new CSVToPlans(matsimInputDir + "plans.xml.gz", 
				boundariesInputDir + "Boundaries_20150428_085038.shp");
		converter.run(databaseFilesDir + "Persona.csv",
				databaseFilesDir + "Export_Viaje.csv",
				databaseFilesDir + "Etapa.csv");
		
//		new NetConverter().convertTransitSchedule(transitFilesDir + "transitSchedule_tertiary.xml.gz");
		
//		String path = "C:/Users/Daniel/Documents/work/shared-svn/studies/countries/cl/santiago_pt_demand_matrix/network_dhosse/";
//		new NetConverter().createNetwork(path + "santiago_tertiary.osm", path + "santiago_tertiary.xml.gz");
		
//		OTFVis.playNetwork(path + "santiago_primary.xml.gz");
		
//		LegModeDistanceDistribution lmdd = new LegModeDistanceDistribution();
//		lmdd.init(scenario);
//		lmdd.preProcessData();
//		lmdd.postProcessData();
//		lmdd.writeResults(matsimInputDir);
		
//		new NetConverter().convertCounts2Shape(inputDirCSV + "puntos.csv", outputDirShp + "puntos.shp");
		
	}

}
