package playground.kai.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.core.scenario.ScenarioUtils;

class KNBerlinControler {
	
	public static void main ( String[] args ) {
		Logger.getLogger("blabla").warn("here") ;
		
		// ### prepare the config:
		Config config = ConfigUtils.loadConfig( "/Users/nagel/kairuns-a100/config.xml" ) ;
		
		// paths:
//		config.network().setInputFile("/Users/nagel/");
		config.controler().setOutputDirectory("/Users/nagel/kairuns/berlin-output/");
		
		config.controler().setLastIteration(100); 
		config.controler().setWriteSnapshotsInterval(0);
		config.controler().setWritePlansInterval(100);
		config.controler().setWriteEventsInterval(10);
		
		config.global().setNumberOfThreads(1);
		
		double sampleFactor = 0.02 ;
		config.controler().setMobsim(MobsimType.JDEQSim.toString());
		config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.END_TIME, "36:00:00") ;
		config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.FLOW_CAPACITY_FACTOR, Double.toString(sampleFactor) ) ;
		config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.SQUEEZE_TIME, "5" ) ;
		config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.STORAGE_CAPACITY_FACTOR, Double.toString( Math.pow(sampleFactor, -0.25)) ) ;
		
		config.timeAllocationMutator().setMutationRange(7200.);

		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
//		config.vspExperimental().setScoreMSAStartsAtIteration( (int)(0.8*config.controler().getLastIteration()) );

		config.plans().setRemovingUnneccessaryPlanAttributes(true) ;
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );

		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.abort );
		config.addConfigConsistencyChecker(new VspConfigConsistencyCheckerImpl());
		config.checkConsistency();
		
		// prepare the scenario
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		// prepare the control(l)er:
		Controler controler = new Controler( scenario ) ;
		controler.setOverwriteFiles(true) ;
		controler.addControlerListener(new KaiAnalysisListener()) ;
//		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
//		controler.setMobsimFactory(new OldMobsimFactory()) ;

		// run everything:
		controler.run();
	
	}
	
}