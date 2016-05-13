package playground.smetzler.bike;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;

public class RunBikes {
	
	public static void main(String[] args) {
		//Config config = ConfigUtils.loadConfig("../../../shared-svn/studies/countries/de/berlin-bike/input/config_bike_equilCarnBike.xml", new BikeConfigGroup(), new MultiModalConfigGroup());
//		Config config = ConfigUtils.loadConfig("../../../shared-svn/studies/countries/de/berlin-bike/input/config_bike_innenringWCar.xml", new BikeConfigGroup(), new MultiModalConfigGroup());
		Config config = ConfigUtils.loadConfig("../../../shared-svn/studies/countries/de/berlin-bike/input/config_bike_equilCarnBike.xml", new BikeConfigGroup());

		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		//TODO add setInsert,...
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		
		config.global().setNumberOfThreads(1);
		
		config.controler().setLastIteration(0);
		
//		config.qsim().setLinkDynamics( LinkDynamics.PassingQ.name() );
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);
		
		
		// muss ich im default-mode trotzdem mit addOverridingModule ein modul hinzufuegen oder muss ich in der config was ausschalten?
		controler.addOverridingModule(new BikeModule());

		controler.run();
	}

}