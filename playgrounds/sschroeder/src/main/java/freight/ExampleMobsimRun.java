package freight;

import org.matsim.contrib.freight.carrier.CarrierConfig;
import org.matsim.contrib.freight.controler.CarrierControler;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;

public class ExampleMobsimRun {
	
	public static void main(String[] args) {
		
		
		String NETWORK_FILENAME = "input/grid10.xml";

		Config config = new Config();
		config.addCoreModules();
		config.global().setCoordinateSystem("EPSG:32632");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1);
		config.network().setInputFile(NETWORK_FILENAME);
		config.addQSimConfigGroup(new QSimConfigGroup());
		Controler controler = new Controler(config);
		controler.setWriteEventsInterval(1);
		controler.setCreateGraphs(false);
		CarrierConfig carrierConfig = new CarrierConfig();
		carrierConfig.addCoreModules();
		carrierConfig.plans().setInputFile("output/myCarrierPlans.xml");
		CarrierControler carrierControler = new CarrierControler(carrierConfig);
		carrierControler.setCarrierScoringFunctionFactory(new DistanceScoringFunctionFactoryForTests(controler.getNetwork()));		
		controler.addControlerListener(carrierControler);
		controler.setOverwriteFiles(true);
		controler.run();
	}

}
