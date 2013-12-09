package playground.mzilske.cadyts;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsPlanChanger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class CadytsTrial {
	

	public static void main(String[] args) {	
		Config config = ConfigUtils.createConfig() ;
		config.global().setRandomSeed(4711) ;
		config.network().setInputFile("input/equil-cadyts/network.xml") ;
		config.plans().setInputFile("input/equil-cadyts//plans100.xml") ;
		
		config.controler().setLastIteration(200) ;
		config.controler().setOutputDirectory("output") ;
		config.controler().setWriteEventsInterval(1) ;
		config.controler().setMobsim(MobsimType.qsim.toString()) ;
		config.qsim().setStuckTime(10.) ;
		config.qsim().setRemoveStuckVehicles(false) ; 
		Set<String> modes = new HashSet<String>() ;
		modes.add("car");
		config.transit().setTransitModes(modes) ;
		{
			ActivityParams params = new ActivityParams("h") ;
			config.planCalcScore().addActivityParams(params ) ;
			params.setTypicalDuration(12*60*60.) ;
		}{
			ActivityParams params = new ActivityParams("w") ;
			config.planCalcScore().addActivityParams(params ) ;
			params.setTypicalDuration(8*60*60.) ;
		}
		
		config.counts().setCountsFileName("input/equil-cadyts/counts100.xml");

		StrategySettings stratSets = new StrategySettings(new IdImpl(1));
		stratSets.setModuleName("ccc") ;
		stratSets.setProbability(1.) ;
		config.strategy().addStrategySettings(stratSets) ;
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		final Controler controler = new Controler(scenario);
		final CadytsContext context = new CadytsContext( config ) ;
		controler.addControlerListener(context) ;
		controler.setOverwriteFiles(true);
		controler.addPlanStrategyFactory("ccc", new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager events2) {
				CadytsPlanChanger planSelector = new CadytsPlanChanger(scenario2,context);
				planSelector.setCadytsWeight(10000000);
				return new PlanStrategyImpl(planSelector);
			}} ) ;
		controler.setCreateGraphs(false);
		controler.setDumpDataAtEnd(true);
		controler.run();
	}


}
