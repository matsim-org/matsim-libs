package playground.dhosse.cl;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;

public class RunCL {
	
	public static void main(String args[]){
		
//		String inputDir = "C:/Users/Daniel/Desktop/work/cl/santiago_pt_demand_matrix/network/";
//		String inputDirCSV = "C:/Users/Daniel/Desktop/work/cl/Kai_und_Daniel/";
//		String outputDir = "C:/Users/Daniel/Desktop/work/cl/Kai_und_Daniel/network_transformed/";
//		String outputDirShp = "C:/Users/Daniel/Desktop/work/cl/Kai_und_Daniel/Visualisierungen/";
		
//		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new NetworkReaderMatsimV1(scenario).parse(outputDir + "santiago_tertiary_19S.xml.gz");
		
		//coordinate conversion from EPSG:3857 to EPSG:32719
//		new NetConverter().convertCoordinates(scenario.getNetwork(), outputDir + "santiago_tiny_19S.xml");
		
		//for conversion of links into shape file
//		new NetConverter().convertNet2Shape(scenario.getNetwork(), outputDirShp + "santiago_tertiary.shp");
		
		//for conversion of raw datas into matsim plans
//		new CSVToPlans(inputDirCSV + "Viaje_Coord.csv").run(inputDirCSV + "plans.xml");
		
//		new NetConverter().convertCounts2Shape(inputDirCSV + "puntos.csv", outputDirShp + "puntos.shp");
		
	}

}
