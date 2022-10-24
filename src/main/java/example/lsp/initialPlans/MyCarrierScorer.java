package example.lsp.initialPlans;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.controler.CarrierScoringFunctionFactory;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;

import javax.inject.Inject;

/**
 * @author Kai Martins-Turner (kturner)
 */
class MyCarrierScorer implements CarrierScoringFunctionFactory {

	@Inject
	private Network network;

	public ScoringFunction createScoringFunction(Carrier carrier) {
		SumScoringFunction sf = new SumScoringFunction();
		DriversLegScoring driverLegScoring = new DriversLegScoring(carrier, this.network);
		VehicleEmploymentScoring vehicleEmploymentScoring = new VehicleEmploymentScoring(carrier);
		DriversActivityScoring actScoring = new DriversActivityScoring();
		sf.addScoringFunction(driverLegScoring);
		sf.addScoringFunction(vehicleEmploymentScoring);
		sf.addScoringFunction(actScoring);
		return sf;
	}


	private static class DriversLegScoring implements SumScoringFunction.BasicScoring {
		public DriversLegScoring(Carrier carrier, Network network) {
		}

		@Override public void finish() {
		}

		@Override public double getScore() {
			return -10;
		}
	}

	private static class VehicleEmploymentScoring implements SumScoringFunction.BasicScoring{
		public VehicleEmploymentScoring(Carrier carrier) {
		}

		@Override public void finish() {
		}

		@Override public double getScore() {
			return -100;
		}
	}

	private static class DriversActivityScoring implements SumScoringFunction.BasicScoring{
		@Override public void finish() {
		}

		@Override public double getScore() {
			return -.1;
		}
	}
}
