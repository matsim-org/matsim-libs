package playground.mzilske.cdr;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class BerlinRunUncongested implements Runnable {
	
	final static String BERLIN_PATH = "/Users/michaelzilske/shared-svn/studies/countries/de/berlin/";
	
	public static void main(String[] args) {
		BerlinRunUncongested berlinRun = new BerlinRunUncongested();
		berlinRun.run();
	}
	
	@Override
	public void run() {
		Config config = new Config();
		config.addCoreModules();
		new ConfigReader(config).parse(this.getClass().getResourceAsStream("2kW.15.xml"));
		config.plans().setInputFile(BERLIN_PATH + "plans/baseplan_car_only.xml.gz");  // 18377 persons
		config.network().setInputFile(BERLIN_PATH + "network/bb_4.xml.gz"); // only till secondary roads (4), dumped from OSM 20090603, contains 35336 nodes and 61920 links
		config.counts().setCountsFileName(BERLIN_PATH + "counts/iv_counts/vmz_di-do.xml");
        config.controler().setOutputDirectory("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/regimes/uncongested/output-berlin");
		config.controler().setMobsim(MobsimType.qsim.toString());
        config.controler().setLastIteration(0);
		config.qsim().setFlowCapFactor(100);
		config.qsim().setStorageCapFactor(100);
		config.qsim().setRemoveStuckVehicles(false);
		config.planCalcScore().setWriteExperiencedPlans(true);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		

		
		final Controler controller = new Controler(scenario);
		controller.run();
		

	}
}