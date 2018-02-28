package testMutualReplanning;

import demand.demandObject.DemandObject;
import demand.scoring.DemandScorer;
import testDemandObjectsWithLotsizes.LotSizeShipment;

public class LotSizeScorer implements DemandScorer{

	
	
	
	
	@Override
	public double scoreCurrentPlan(DemandObject demandObject) {
		if(demandObject.getSelectedPlan().getShipment() instanceof LotSizeShipment) {
			
		}
		return 0;
	}

}
