package demand.scoring;

import demand.demandObject.DemandObject;
import lsp.scoring.Scorer;

public interface DemandScorer extends Scorer{

	public double scoreCurrentPlan(DemandObject demandObject);
	
}
