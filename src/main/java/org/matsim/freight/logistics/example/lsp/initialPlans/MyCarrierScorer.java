package org.matsim.freight.logistics.example.lsp.initialPlans;

import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.controler.CarrierScoringFunctionFactory;

/**
 * @author Kai Martins-Turner (kturner)
 */
class MyCarrierScorer implements CarrierScoringFunctionFactory {

	public ScoringFunction createScoringFunction(Carrier carrier) {
		SumScoringFunction sf = new SumScoringFunction();
		TakeJspritScore takeJspritScore = new TakeJspritScore( carrier);
		sf.addScoringFunction(takeJspritScore);
		return sf;
	}

	private class TakeJspritScore implements SumScoringFunction.BasicScoring {

		private final Carrier carrier;
		public TakeJspritScore(Carrier carrier) {
			super();
			this.carrier = carrier;
		}

		@Override public void finish() {}

		@Override public double getScore() {
			if (carrier.getSelectedPlan().getScore() != null){
				return carrier.getSelectedPlan().getScore();
			}
			return Double.NEGATIVE_INFINITY;
		}
	}
}
