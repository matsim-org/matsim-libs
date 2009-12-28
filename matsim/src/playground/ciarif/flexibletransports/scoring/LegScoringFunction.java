package playground.ciarif.flexibletransports.scoring;

import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.scoring.CharyparNagelScoringParameters;

import playground.ciarif.flexibletransports.config.FtConfigGroup;
import playground.meisterk.kti.router.KtiPtRoute;
import playground.meisterk.kti.router.PlansCalcRouteKti;

public class LegScoringFunction extends org.matsim.core.scoring.charyparNagel.LegScoringFunction{
	
	private final FtConfigGroup ftConfigGroup;
	private final PlansCalcRouteConfigGroup plansCalcRouteConfigGroup;

	private final static Logger log = Logger.getLogger(LegScoringFunction.class);

	public LegScoringFunction(PlanImpl plan,
			CharyparNagelScoringParameters params,
			Config config,
			FtConfigGroup ftConfigGroup) {
		super(plan, params);
		this.ftConfigGroup = ftConfigGroup;
		this.plansCalcRouteConfigGroup = config.plansCalcRoute();
	}

	@Override
	protected double calcLegScore(double departureTime, double arrivalTime,
			LegImpl leg) {

		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in seconds

		double dist = 0.0; // distance in meters

		if (TransportMode.car.equals(leg.getMode())) {
			
			tmpScore += this.ftConfigGroup.getConstCar();
			
			if (this.params.marginalUtilityOfDistanceCar != 0.0) {
				RouteWRefs route = leg.getRoute();
				dist = route.getDistance();
				tmpScore += this.params.marginalUtilityOfDistanceCar * ftConfigGroup.getDistanceCostCar()/1000d * dist;
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling;
			
		} else if (TransportMode.pt.equals(leg.getMode())) {

			KtiPtRoute ktiPtRoute = (KtiPtRoute) leg.getRoute();
			
			if (ktiPtRoute.getFromStop() != null) {

//				String nanoMsg = "Scoring kti pt:\t";
				
//				long nanos = System.nanoTime();
				dist = ((KtiPtRoute) leg.getRoute()).calcAccessEgressDistance(((PlanImpl) this.plan).getPreviousActivity(leg), ((PlanImpl) this.plan).getNextActivity(leg));
//				nanos = System.nanoTime() - nanos;
//				nanoMsg += Long.toString(nanos) + "\t";
				
//				nanos = System.nanoTime();
				travelTime = PlansCalcRouteKti.getAccessEgressTime(dist, this.plansCalcRouteConfigGroup);
//				nanos = System.nanoTime() - nanos;
//				nanoMsg += Long.toString(nanos) + "\t";

				tmpScore += this.getWalkScore(dist, travelTime);
				
//				nanos = System.nanoTime();
				dist = ((KtiPtRoute) leg.getRoute()).calcInVehicleDistance();
//				nanos = System.nanoTime() - nanos;
//				nanoMsg += Long.toString(nanos) + "\t";

//				nanos = System.nanoTime();
				travelTime = ((KtiPtRoute) leg.getRoute()).getInVehicleTime();
//				nanos = System.nanoTime() - nanos;
//				nanoMsg += Long.toString(nanos) + "\t";

				tmpScore += this.getPtScore(dist, travelTime);
//				log.info(nanoMsg);

			} else {

				dist = leg.getRoute().getDistance();
				tmpScore += this.getPtScore(dist, travelTime);

			}

		} else if (TransportMode.walk.equals(leg.getMode())) {
			
			if (this.params.marginalUtilityOfDistanceWalk != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			tmpScore += this.getWalkScore(dist, travelTime);
			
		} else if (TransportMode.bike.equals(leg.getMode())) {
			
			tmpScore += this.ftConfigGroup.getConstBike();
			
			tmpScore += travelTime * this.ftConfigGroup.getTravelingBike() / 3600d;
			
		} else if (TransportMode.ride.equals(leg.getMode())) {
			
			if (this.ftConfigGroup.getMarginalUtilityOfDistanceRide()!= 0.0) {
				dist = 1.2*leg.getRoute().getDistance();
			}
			travelTime= travelTime*1.2;
			tmpScore += this.getRideScore(dist, travelTime);
			
			
		} else {
			
			if (this.params.marginalUtilityOfDistanceCar != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			// use the same values as for "car"
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling + this.params.marginalUtilityOfDistanceCar * dist;
			
		}

		return tmpScore;
	}

	private double getRideScore(double distance, double travelTime) {
		double score = 0.0;
		
		score += this.ftConfigGroup.getConstRide();
		
		score += this.ftConfigGroup.getMarginalUtilityOfDistanceRide() * this.ftConfigGroup.getDistanceCostRide() / 1000d * distance;
		
		score += travelTime * this.ftConfigGroup.getTravelingRide() / 3600d;// TODO Auto-generated method stub
		
		return score;
	}

	private double getWalkScore(double distance, double travelTime) {
		
		double score = 0.0;
		
		score += travelTime * this.params.marginalUtilityOfTravelingWalk + this.params.marginalUtilityOfDistanceWalk * distance;
		
		return score;
		
	}

	private double getPtScore(double distance, double travelTime) {

		double score = 0.0;

		double distanceCost = 0.0;
		TreeSet<String> travelCards = ((PersonImpl) this.plan.getPerson()).getTravelcards();
		if (travelCards == null) {
			distanceCost = this.ftConfigGroup.getDistanceCostPtNoTravelCard();
		} else if (travelCards.contains("unknown")) {
			distanceCost = this.ftConfigGroup.getDistanceCostPtUnknownTravelCard();
		} else {
			throw new RuntimeException("Person " + this.plan.getPerson().getId() + " has an invalid travelcard. This should never happen.");
		}
		score += this.params.marginalUtilityOfDistancePt * distanceCost / 1000d * distance;
		score += travelTime * this.params.marginalUtilityOfTravelingPT;
		score += score += this.ftConfigGroup.getConstPt();

		return score;
		
	}
	
	
}
