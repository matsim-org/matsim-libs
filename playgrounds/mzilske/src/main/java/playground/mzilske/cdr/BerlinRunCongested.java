package playground.mzilske.cdr;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

public class BerlinRunCongested implements Runnable {
	
	final static String BERLIN_PATH = "/Users/michaelzilske/shared-svn/studies/countries/de/berlin/";
	
	public static void main(String[] args) {
		BerlinRunCongested berlinRun = new BerlinRunCongested();
		berlinRun.run();
	}
	
	@Override
	public void run() {
		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).parse(this.getClass().getResourceAsStream("2kW.15.xml"));
		config.plans().setInputFile(BERLIN_PATH + "plans/baseplan_car_only.xml.gz");  // 18377 persons
		config.network().setInputFile(BERLIN_PATH + "network/bb_5_v_scaled_simple.xml.gz"); // only till secondary roads (4), dumped from OSM 20090603, contains 35336 nodes and 61920 links
		config.counts().setCountsFileName(BERLIN_PATH + "counts/counts4bb_5_v_notscaled_simple.xml");
        config.counts().setOutputFormat("txt");
		config.controler().setOutputDirectory("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/regimes/congested/temp");
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.planCalcScore().setWriteExperiencedPlans(true);
		config.qsim().setRemoveStuckVehicles(false);
        config.qsim().setNumberOfThreads(8);
        config.parallelEventHandling().setNumberOfThreads(2);
        config.global().setNumberOfThreads(8);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		config.controler().setMobsim(MobsimType.JDEQSim.toString());

		
		final Controler controller = new Controler(scenario);
//        controller.setMobsimFactory(new MobsimFactory() {
//            @Override
//            public Mobsim createMobsim(Scenario scenario, EventsManager eventsManager) {
//                eventsManager.initProcessing();
//                QSim qsim = new QSim(scenario, eventsManager);
//                JDEQSimModule.configure(qsim);
//                PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), new DefaultAgentFactory(qsim), qsim);
//                qsim.addAgentSource(agentSource);
//                return qsim;
//            }
//        });
		controller.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controller.run();
		

	}
}