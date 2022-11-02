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
//		DriversLegScoring driverLegScoring = new DriversLegScoring(carrier, this.network);
//		DriversActivityScoring actScoring = new DriversActivityScoring();
		TakeJspritScore takeJspritScore = new TakeJspritScore( carrier);
//		sf.addScoringFunction(driverLegScoring);
		sf.addScoringFunction(vehicleEmploymentScoring);
//		sf.addScoringFunction(actScoring);
		sf.addScoringFunction(takeJspritScore);
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

//	private static class DriversLegScoring implements SumScoringFunction.LegScoring {
//
//		private double score = 0.0;
//		private final Network network;
//		private final Carrier carrier;
//
//
//		public DriversLegScoring(Carrier carrier, Network network) {
//			super();
//			this.network = network;
//			this.carrier = carrier;
//		}
//
//		@Override public void finish() {}
//
//		@Override public double getScore() {
//			return score;
//		}
//
//		@Override public void handleLeg(Leg leg) {
//			if(leg.getRoute() instanceof NetworkRoute nRoute){
//				CarrierVehicle vehicle = CarrierUtils.getCarrierVehicle(carrier, nRoute.getVehicleId());
//				Gbl.assertNotNull(vehicle);
//
//				//Distance based costs / score
//				{
//					double distance = 0.0;
//					//TODO KMT: Warum sind Start und EndLink hier enthalten? Ist das dann nicht ggf. doppelt gezählt - bei mehreren Legs in Folge?
//					distance += network.getLinks().get(nRoute.getStartLinkId()).getLength();
//					for (Id<Link> linkId : nRoute.getLinkIds()) {
//						distance += network.getLinks().get(linkId).getLength();
//					}
//					distance += network.getLinks().get(nRoute.getEndLinkId()).getLength();
//
//
//					double distanceCosts = distance * vehicle.getType().getCostInformation().getCostsPerMeter();
//					if (!(distanceCosts >= 0.0)) throw new AssertionError("distanceCosts must be positive");
//					score += (-1) * distanceCosts;
//				}
//
//				//Time-based (driving) costs /score
//				{
//					double timeCosts = leg.getTravelTime().seconds() * vehicle.getType().getCostInformation().getCostsPerSecond();
//					if (!(timeCosts >= 0.0)) throw new AssertionError("timeCosts of leg must be positive");
//					score += (-1) * timeCosts;
//				}
//			}
//		}
//	}

//	private static class DriversActivityScoring implements SumScoringFunction.ActivityScoring{
//
//		private double score;
//
//		@Override public void finish() {
//		}
//
//		@Override public double getScore() {
//			return score;
//		}
//
//		@Override public void handleFirstActivity(Activity activity) {
//			handleActivity(activity); // no other handling then normal - in between - activity
//		}
//
//		@Override public void handleActivity(Activity activity) {
//			if (activity instanceof FreightActivity freightActivity) {
//				double actStartTime = freightActivity.getStartTime().seconds();
////				// Scoring for missed TimeWindows -- Commented out, because it is unclear, which value to set here
////				// and I do not have a replanning strategy, that forces e.g. a time-shift
////				TimeWindow tw = freightActivity.getTimeWindow();
////				if(actStartTime > tw.getEnd()){
////					double penalty_score = (-1)*(actStartTime - tw.getEnd()) * missedTimeWindowPenalty;
////					if (!(penalty_score <= 0.0)) throw new AssertionError("penalty score must be negative");
////					score += penalty_score;
////				}
//
//				//TODO: Unclear how to get the right timeParameter out of the vehicleType - missing link between activity and driver/agent, that could be used.
//				double actTimeCosts = (freightActivity.getEndTime().seconds() - actStartTime) * timeParameter;
//				if (!(actTimeCosts >= 0.0)) throw new AssertionError("actTimeCosts must be positive");
//				score += actTimeCosts * (-1);
//			}
//		}
//
//		@Override public void handleLastActivity(Activity activity) {
//			handleActivity(activity); // no other handling then normal - in between - activity
//		}
//	}


}
