package playground.smetzler.bike;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

public class RunBikes {
	
	public static void main(String[] args) {

//		//boddin
//		Config config = ConfigUtils.loadConfig("../../../shared-svn/studies/countries/de/berlin-bike/input/szenarios/boddin/config_bike_boddin.xml", new BikeConfigGroup());
//		//berlin
//		Config config = ConfigUtils.loadConfig("../../../shared-svn/studies/countries/de/berlin-bike/input/szenarios/berlin/config_bike_berlin.xml", new BikeConfigGroup());

		//Oslo
		Config config = ConfigUtils.loadConfig("../../../../desktop/Oslo/config_bike_oslo.xml", new BikeConfigGroup());

		
//		config.controler().setOutputDirectory("../../../runs-svn/berlin-bike/BerlinBike_0804_BVG_15000");
//		
//		config.plans().setInputFile("demand/bvg.run189.10pct.100.plans.selected_bikeonly_1percent_clean.xml.gz" );
//		
//		config.network().setInputFile("network/BerlinBikeNet_MATsim.xml");
		

		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		
		config.global().setNumberOfThreads(1);
		
		config.controler().setLastIteration(0);
		
//		calculate customized bike speed per link? makes separate network unnecessary 
//		config.qsim().setLinkDynamics( LinkDynamics.PassingQ.name() );
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new BikeModule());
		controler.run();
	}

}