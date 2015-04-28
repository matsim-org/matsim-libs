package playground.dhosse.cl;

import java.util.Locale;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;

public class RunCL {
	
	public static void main(String args[]){
		
		String inputDir = "C:/Users/Daniel/Desktop/work/cl/santiago_pt_demand_matrix/network/";
		String inputDirCSV = "C:/Users/Daniel/Desktop/work/cl/Kai_und_Daniel/";
		String outputDir = "C:/Users/Daniel/Desktop/work/cl/Kai_und_Daniel/network_transformed/";
		String outputDirShp = "C:/Users/Daniel/Desktop/work/cl/Kai_und_Daniel/Visualisierungen/";
		String dirForMATSimConfigFile = "C:/Users/Daniel/Desktop/work/cl/Kai_und_Daniel/inputFiles/";
		String dirForMATSimInputFiles = "C:/Users/Daniel/Desktop/work/cl/Kai_und_Daniel/inputFiles/input/";
		
//		Config config = ConfigUtils.createConfig();
//		ConfigUtils.loadConfig(config, dirForMATSimConfigFile + "config.xml");
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		Controler controler = new Controler(scenario);
//		controler.run();
		
//		new NetConverter().plans2Shape(scenario.getPopulation(), outputDirShp + "activities.shp");
		
//		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new NetworkReaderMatsimV1(scenario).parse(outputDir + "santiago_tertiary_19S.xml.gz");
		
		//coordinate conversion from EPSG:3857 to EPSG:32719
//		new NetConverter().convertCoordinates(scenario.getNetwork(), outputDir + "santiago_tiny_19S.xml");
		
		//for conversion of links into shape file
//		new NetConverter().convertNet2Shape(scenario.getNetwork(), outputDirShp + "santiago_tertiary.shp");
		
		//for conversion of raw datas into matsim plans
//		CSVToPlans converter = new CSVToPlans(dirForMATSimInputFiles + "plans.xml", 
//				inputDirCSV + "exported_boundaries/Boundaries_20150428_085038.shp");
//		converter.run(inputDirCSV + "Export_Viaje.csv", inputDirCSV + "comunas.csv");
//		converter.finalize();
		
//		new NetConverter().convertCounts2Shape(inputDirCSV + "puntos.csv", outputDirShp + "puntos.shp");
		
	}

}
