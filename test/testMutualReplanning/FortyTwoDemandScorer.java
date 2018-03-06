package testMutualReplanning;

import demand.demandObject.DemandObject;
import demand.scoring.DemandScorer;

public class FortyTwoDemandScorer implements DemandScorer {

	@Override
	public double scoreCurrentPlan(DemandObject demandObject) {
		return 42;
	}

	@Override
	public void setDemandObject(DemandObject demandObject) {
		// TODO Auto-generated method stub
		
	}

}
