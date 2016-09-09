package playground.sebhoerl.avtaxi.framework;

import java.net.MalformedURLException;
import java.net.URL;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiQSimProvider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunAVTaxiScenario {
	public static void main(String[] args) throws MalformedURLException {
		String configFile = "src/main/resources/sebhoerl/avtaxi/config.xml";
		
		Config config = ConfigUtils.loadConfig(configFile, new TaxiConfigGroup());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		TaxiData taxiData = new TaxiData();
		new VehicleReader(scenario.getNetwork(), taxiData).readFile("src/main/resources/sebhoerl/avtaxi/taxis.xml");
		
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new TaxiModule(taxiData));
		controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05));
		controler.addOverridingModule(new DynQSimModule<>(AVTaxiQSimProvider.class));
		controler.addOverridingModule(new AVTaxiModule());
		
		controler.run();
	}
}
