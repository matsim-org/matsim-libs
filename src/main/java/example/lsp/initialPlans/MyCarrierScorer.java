package example.lsp.initialPlans;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.ScheduledTour;
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
		VehicleEmploymentScoring vehicleEmploymentScoring = new VehicleEmploymentScoring(carrier);
		DriversLegScoring driverLegScoring = new DriversLegScoring(carrier, this.network);
		DriversActivityScoring actScoring = new DriversActivityScoring();
		sf.addScoringFunction(driverLegScoring);
		sf.addScoringFunction(vehicleEmploymentScoring);
		sf.addScoringFunction(actScoring);
		return sf;
	}


	/**
	 * Score the (daily) fixed costs of each vehicle that is used.
	 * Summing up for all tours. The values are taken from the vehicleCostInformation in the vehicleType.
	 */
	private static class VehicleEmploymentScoring implements SumScoringFunction.BasicScoring{

		private final Carrier carrier;

		public VehicleEmploymentScoring(Carrier carrier) {
			super();
			this.carrier = carrier;
		}

		@Override public void finish() {}

		@Override public double getScore() {
			double score = 0.;
			CarrierPlan selectedPlan = carrier.getSelectedPlan();
			if(selectedPlan == null) return 0.;
			for(ScheduledTour tour : selectedPlan.getScheduledTours()){
				if(!tour.getTour().getTourElements().isEmpty()){
					score += (-1)*tour.getVehicle().getType().getCostInformation().getFixedCosts();
				}
			}
			return score;
		}
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

	private static class DriversActivityScoring implements SumScoringFunction.BasicScoring{
		@Override public void finish() {
		}

		@Override public double getScore() {
			return -.1;
		}
	}
}
