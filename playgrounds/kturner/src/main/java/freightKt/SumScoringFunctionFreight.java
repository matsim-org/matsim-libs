package freightKt;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;

public class SumScoringFunctionFreight extends SumScoringFunction implements ScoringFunction {
	
	public interface FixCostScoring extends BasicScoring  {	//New KT
		void addFixCost(final Carrier carrier);
	}
	
	private static Logger log = Logger.getLogger(SumScoringFunctionFreight.class);
	
	private ArrayList<BasicScoring> basicScoringFunctions = new ArrayList<BasicScoring>();
	private ArrayList<ActivityScoring> activityScoringFunctions = new ArrayList<ActivityScoring>();
	private ArrayList<MoneyScoring> moneyScoringFunctions = new ArrayList<MoneyScoring>();
	private ArrayList<LegScoring> legScoringFunctions = new ArrayList<LegScoring>();
	private ArrayList<AgentStuckScoring> agentStuckScoringFunctions = new ArrayList<AgentStuckScoring>();
	private ArrayList<ArbitraryEventScoring> arbtraryEventScoringFunctions = new ArrayList<ArbitraryEventScoring>() ;
	private ArrayList<FixCostScoring> fixCostScoringFunctions = new ArrayList<FixCostScoring>() ; //New KT
	
	public SumScoringFunctionFreight() {
		super();
	}
	
	public void addFixCost(Carrier carrier) {	//New KT
		for (FixCostScoring fixCostScoringFunction : fixCostScoringFunctions) {
			fixCostScoringFunction.addFixCost(carrier);
		}
	}
	
	/**
	 * Add the score of all functions.
	 */
	@Override
	public double getScore() {
		double score = 0.0;
		for (BasicScoring basicScoringFunction : basicScoringFunctions) {
            double contribution = basicScoringFunction.getScore();
			if (log.isTraceEnabled()) {
				log.trace("Contribution of scoring function: " + basicScoringFunction.getClass().getName() + " is: " + contribution);
			}
            score += contribution;
		}
		return score;
	}
	
	@Override
	public void addScoringFunction(BasicScoring scoringFunction) {
		basicScoringFunctions.add(scoringFunction);

		if (scoringFunction instanceof ActivityScoring) {
			activityScoringFunctions.add((ActivityScoring) scoringFunction);
		}

		if (scoringFunction instanceof AgentStuckScoring) {
			agentStuckScoringFunctions.add((AgentStuckScoring) scoringFunction);
		}

		if (scoringFunction instanceof LegScoring) {
			legScoringFunctions.add((LegScoring) scoringFunction);
		}

		if (scoringFunction instanceof MoneyScoring) {
			moneyScoringFunctions.add((MoneyScoring) scoringFunction);
		}
		
		if (scoringFunction instanceof ArbitraryEventScoring ) {
			this.arbtraryEventScoringFunctions.add((ArbitraryEventScoring) scoringFunction) ;
		}
		
		if (scoringFunction instanceof FixCostScoring ) {			//New KT
			this.fixCostScoringFunctions.add((FixCostScoring) scoringFunction) ;
		}
		

	}

}
