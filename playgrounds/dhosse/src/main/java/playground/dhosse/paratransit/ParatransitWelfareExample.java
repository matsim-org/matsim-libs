package playground.dhosse.paratransit;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.hook.PModule;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.gui.Gui;

public class ParatransitWelfareExample {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, "C:/Users/Daniel/Desktop/work/paratransit/paratransitWelfareExample/config.xml");
		config.plansCalcRoute().setBeelineDistanceFactor(1.);
		
		PConfigGroup pConfig = new PConfigGroup();
		config.addModule(pConfig);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);
		PModule hook = new PModule();
		hook.configureControler(controler);
		controler.setOverwriteFiles(true);
		controler.run();
		
	}

}
