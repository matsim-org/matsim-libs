package example.lsp.initialPlans;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.controler.CarrierScoringFunctionFactory;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.NetworkRoute;
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

	private static class DriversLegScoring implements SumScoringFunction.LegScoring {

		private double score = 0.0;
		private final Network network;
		private final Carrier carrier;


		public DriversLegScoring(Carrier carrier, Network network) {
			super();
			this.network = network;
			this.carrier = carrier;
		}

		@Override public void finish() {}

		@Override public double getScore() {
			return score;
		}

		@Override public void handleLeg(Leg leg) {
			if(leg.getRoute() instanceof NetworkRoute nRoute){
				CarrierVehicle vehicle = CarrierUtils.getCarrierVehicle(carrier, nRoute.getVehicleId());
				Gbl.assertNotNull(vehicle);

				//Distance based costs / score
				{
					double distance = 0.0;
					//TODO KMT: Warum sind Start und EndLink hier enthalten? Ist das dann nicht ggf. doppelt gezählt - bei mehreren Legs in Folge?
					distance += network.getLinks().get(nRoute.getStartLinkId()).getLength();
					for (Id<Link> linkId : nRoute.getLinkIds()) {
						distance += network.getLinks().get(linkId).getLength();
					}
					distance += network.getLinks().get(nRoute.getEndLinkId()).getLength();


					double distanceCosts = distance * vehicle.getType().getCostInformation().getCostsPerMeter();
					if (!(distanceCosts >= 0.0)) throw new AssertionError("distanceCosts must be positive");
					score += (-1) * distanceCosts;
				}

				//Time-based (driving) costs /score
				{
					double timeCosts = leg.getTravelTime().seconds() * vehicle.getType().getCostInformation().getCostsPerSecond();
					if (!(timeCosts >= 0.0)) throw new AssertionError("timeCosts of leg must be positive");
					score += (-1) * timeCosts;
				}
			}
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
