package playground.dziemke.cadyts.examples;

import java.util.Collections;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;

public class EquilController03b {
	private final static Logger log = Logger.getLogger(EquilController03b.class);

	public static void main(final String[] args) {
		final Config config = ConfigUtils.createConfig();
		
		// global
		config.global().setRandomSeed(4711);
		config.global().setCoordinateSystem("Atlantis");
		
		// network
		String inputNetworkFile = "D:/Workspace/container/examples/equil/input/network.xml";
		config.network().setInputFile(inputNetworkFile);
		
		// plans
		String inputPlansFile = "D:/Workspace/container/examples/equil/input/plans100.xml";
		config.plans().setInputFile(inputPlansFile);
		
		//simulation
		config.addSimulationConfigGroup(new SimulationConfigGroup());
		config.simulation().setStartTime(0);
		config.simulation().setEndTime(0);
		config.simulation().setSnapshotPeriod(60);
		//config.simulation().setFlowCapFactor(0.01);
		//config.simulation().setStorageCapFactor(0.02);
		
		// counts
		String countsFileName = "D:/Workspace/container/examples/equil/input/counts100.xml";
		config.counts().setCountsFileName(countsFileName);
		//config.counts().setCountsScaleFactor(100);
		config.counts().setOutputFormat("all");
		
		// controller
		//String runId = "run_xy";
		String outputDirectory = "D:/Workspace/container/examples/equil/output/03b/";
		//config.controler().setRunId(runId);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(50);
		//Set<EventsFileFormat> eventsFileFormats = Collections.unmodifiableSet(EnumSet.of(EventsFileFormat.xml));
		//config.controler().setEventsFileFormats(eventsFileFormats);
		config.controler().setMobsim("queueSimulation");
		//config.controler().setWritePlansInterval(50);
		//config.controler().setWriteEventsInterval(50);
		Set<String> snapshotFormat = Collections.emptySet();
		config.controler().setSnapshotFormat(snapshotFormat);
		
		// planCalcScore
		ActivityParams homeActivity = new ActivityParams("h");
		homeActivity.setPriority(1);
		homeActivity.setTypicalDuration(12*60*60);
		homeActivity.setMinimalDuration(8*60*60);
		config.planCalcScore().addActivityParams(homeActivity);
				
		ActivityParams workActivity = new ActivityParams("w");
		workActivity.setPriority(1);
		workActivity.setTypicalDuration(8*60*60);
		workActivity.setMinimalDuration(6*60*60);
		workActivity.setOpeningTime(7*60*60);
		workActivity.setLatestStartTime(9*60*60);
		//workActivity.setEarliestEndTime();
		workActivity.setClosingTime(18*60*60);
		config.planCalcScore().addActivityParams(workActivity);
				
		// strategy
		StrategySettings strategySettings1 = new StrategySettings(new IdImpl(1));
		strategySettings1.setModuleName("ChangeExpBeta");
		strategySettings1.setProbability(0.9);
		config.strategy().addStrategySettings(strategySettings1);
		
		StrategySettings strategySettings2 = new StrategySettings(new IdImpl(2));
		strategySettings2.setModuleName("ReRoute");
		strategySettings2.setProbability(0.1);
		config.strategy().addStrategySettings(strategySettings2);
				
		config.strategy().setMaxAgentPlanMemorySize(5);

		// start controller 
		final Controler controler = new Controler(config);
		controler.run() ;
	}
}