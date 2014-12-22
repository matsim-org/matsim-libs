package util;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.utils.Visualiser;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class VisualizeCarrierPlans {
	
	public static void main(String[] args) {

        int iteration = 200;

        Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile("input/usecases/chessboard/network/grid9x9_cap15.xml");
		
		Carriers carriers = new Carriers();
		new CarrierPlanXmlReaderV2(carriers).read("output/0.run/ITERS/it."+iteration+"/"+iteration+".carrierPlans.xml");
//        new CarrierPlanXmlReaderV2(carriers).read("scenarios/single_cordon/2.run/ITERS/it."+iteration+"/"+iteration+".carrierPlans.xml");
//        new CarrierPlanXmlReaderV2(carriers).read("input/usecases/chessboard/freight/scenarios/multipleCarriers_withoutTW_withDepots_withPlan.xml");
		
		CarrierVehicleTypes types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).read("input/usecases/chessboard/freight/vehicleTypes_v2.xml");
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);
		
		new Visualiser(config, scenario).visualizeLive(carriers);
//		new Visualiser(config, scenario).makeMVI(carriers, "input/diss/freight/freight.mvi", 60);
//		
//		OTFVis.playMVI("input/diss/freight/freight.mvi");
	}

}
