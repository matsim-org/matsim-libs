package playground.christoph.evacuation.mobsim;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.withinday.mobsim.DuringActivityReplanningModule;
import playground.christoph.withinday.replanning.parallel.ParallelDuringActivityReplanner;

public class EvacuationDuringActivityReplanningModule extends DuringActivityReplanningModule {

	private double evacuationTime = EvacuationConfig.evacuationTime;
	
	public EvacuationDuringActivityReplanningModule(ParallelDuringActivityReplanner parallelDuringActivityReplanner) {
		super(parallelDuringActivityReplanner);
	}

	@Override
	public void doReplanning(double time) {
		if (time != evacuationTime) return;
		else super.doReplanning(time);
	}
}
