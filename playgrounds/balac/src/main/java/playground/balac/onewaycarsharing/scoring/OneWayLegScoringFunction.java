package playground.balac.onewaycarsharing.scoring;

import java.util.TreeSet;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.balac.onewaycarsharing.config.OneWayCSConfigGroup;
import playground.balac.onewaycarsharing.data.MyTransportMode;


public class OneWayLegScoringFunction  extends org.matsim.core.scoring.functions.CharyparNagelLegScoring{

	private final OneWayCSConfigGroup ftConfigGroup;
	private PlanImpl plan;
	private Network network;
	
	private double totalTime = 0.0;
	private double cs_start = 0.0;
	private double cs_end = 0.0;
	private Leg currentLeg;
	private boolean cs = false;
	private double departureTime = 0.0;
	public OneWayLegScoringFunction(PlanImpl plan, CharyparNagelScoringParameters params, Config config, OneWayCSConfigGroup ftConfigGroup, Network network)
	{
		super(params, network);
		this.plan = plan;
		this.network = network;
		this.ftConfigGroup = ftConfigGroup;
		currentLeg = null;
		//this.plansCalcRouteConfigGroup = config.plansCalcRoute();
	}
	@Override
	public void reset() {
		super.reset();
		totalTime = 0.0;
		cs_start = 0.0;
		cs_end = 0.0;
		cs = false;
		departureTime = 0.0;
		currentLeg = null;
	}
	
	@Override
	public void finish() {		
		
		score += totalTime * (-6.0D/3600);   //price for renting a carsharing car		
		
		//score -= 2;    
	}	
	
	private void calculateCarsharingTravelTime(double departureTime, double arrivalTime, Leg leg) {	
		
			if (!cs) {
				cs_start = arrivalTime;
				cs = true;
			}
			else {
				cs_end = departureTime;
				if ((cs_end - cs_start) > 0.0) {
					totalTime += (cs_end - cs_start);
					
				}
				cs = false;
			}
		
	}
	
	@Override
	protected double calcLegScore(double departureTime, double arrivalTime, Leg leg) {
		if (leg.getMode().equals("onewaycarsharingwalk"))
			calculateCarsharingTravelTime(departureTime, arrivalTime, leg);
		
		double tmpScore = 0.0D;
		double travelTime = arrivalTime - departureTime;
		
		double dist = 0.0D;
		
		if (TransportMode.car.equals(leg.getMode()))
		{
			tmpScore += this.ftConfigGroup.getConstCar();
			
			if (this.params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m != 0.0D) {
				Route r = leg.getRoute();
				if (r instanceof NetworkRoute) {
					dist =  RouteUtils.calcDistance((NetworkRoute) r, network);
				} else {
					dist = r.getDistance();
				}
				tmpScore += this.params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m * this.ftConfigGroup.getDistanceCostCar() / 1000.0D * dist;
			}
			tmpScore += travelTime * this.params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s;
		}
		else if (MyTransportMode.onewaycarsharing.equals(leg.getMode()))
		{	
			
			if (this.params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m != 0.0D)
			{
				Route r = leg.getRoute();
				if (r instanceof NetworkRoute) {
					dist =  RouteUtils.calcDistance((NetworkRoute) r, network);
				} else {
					dist = r.getDistance();
				}
				tmpScore += 1.2 * this.params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m * this.ftConfigGroup.getDistanceCostCar() / 1000.0D * dist;
			}
			travelTime = arrivalTime - departureTime;
			tmpScore += travelTime * this.params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s;
		}
		else if (MyTransportMode.onewaycarsharingwalk.equals(leg.getMode())) {
			
			Route r = leg.getRoute();
			if (r instanceof NetworkRoute) {
				dist =  RouteUtils.calcDistance((NetworkRoute) r, network);
			} else {
				dist = r.getDistance();
			}
				tmpScore += getWalkScore(dist, travelTime);
						
			
		}
		else if (TransportMode.pt.equals(leg.getMode()))
		{

			//FtPtRoute ftPtRoute = null;
			/* new FtPtRoute (leg.getRoute(),this.plansCalcRouteConfigGroup.);
      if (ftPtRoute.getFromStop() != null)
      {
        dist = ((FtPtRoute)leg.getRoute()).calcAccessEgressDistance((ActivityImpl)((PlanImpl)this.plan).getPreviousActivity(leg), (ActivityImpl)((PlanImpl)this.plan).getNextActivity(leg));
        travelTime = PlansCalcRouteFT.getAccessEgressTime(dist, this.plansCalcRouteConfigGroup);
        tmpScore += getWalkScore(dist, travelTime);
        dist = ((FtPtRoute)leg.getRoute()).calcInVehicleDistance();
        travelTime = ((FtPtRoute)leg.getRoute()).getInVehicleTime().doubleValue();
        tmpScore += getPtScore(dist, travelTime);
      }
      else
      {*/
			//dist = 1.5 * CoordUtils.calcDistance(network.getLinks().get(leg.getRoute().getStartLinkId()).getCoord(), network.getLinks().get(leg.getRoute().getEndLinkId()).getCoord());
			Route r = leg.getRoute();
			if (r instanceof NetworkRoute) {
				dist =  RouteUtils.calcDistance((NetworkRoute) r, network);
			} else {
				dist = r.getDistance();
			}
			tmpScore += getPtScore(dist, travelTime);
			//}

		}
		else if (TransportMode.walk.equals(leg.getMode()))
		{
			if (this.params.modeParams.get(TransportMode.walk).marginalUtilityOfDistance_m != 0.0D) {
				Route r = leg.getRoute();
				if (r instanceof NetworkRoute) {
					dist =  RouteUtils.calcDistance((NetworkRoute) r, network);
				} else {
					dist = r.getDistance();
				}
			}
			tmpScore += getWalkScore(dist, travelTime);
		}
		else if (TransportMode.bike.equals(leg.getMode()))
		{
			tmpScore += this.ftConfigGroup.getConstBike();

			tmpScore += travelTime * this.ftConfigGroup.getTravelingBike() / 3600.0D;
		}
		else if (TransportMode.ride.equals(leg.getMode()))
		{
			if (this.ftConfigGroup.getMarginalUtilityOfDistanceRide() != 0.0D) {
				Route r = leg.getRoute();
				if (r instanceof NetworkRoute) {
					dist =  RouteUtils.calcDistance((NetworkRoute) r, network);
				} else {
					dist = r.getDistance();
				}
				dist = 1.5D * dist;
			}
			travelTime *= 3.0D;

			tmpScore += getRideScore(dist, travelTime);
		}
		else
		{
			if (this.params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m != 0.0D) {
				Route r = leg.getRoute();
				if (r instanceof NetworkRoute) {
					dist =  RouteUtils.calcDistance((NetworkRoute) r, network);
				} else {
					dist = r.getDistance();
				}
			}

			tmpScore += travelTime * this.params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s + this.params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m * dist;
		}

		return tmpScore;
	}

	private double getRideScore(double distance, double travelTime) {
		double score = 0.0D;

		score += this.ftConfigGroup.getConstRide();

		score += this.ftConfigGroup.getMarginalUtilityOfDistanceRide() * this.ftConfigGroup.getDistanceCostRide() / 1000.0D * distance;

		score += travelTime * this.ftConfigGroup.getTravelingRide() / 3600.0D;

		return score;
	}

	private double getWalkScore(double distance, double travelTime)
	{
		double score = 0.0D;

		score += travelTime * this.params.modeParams.get(TransportMode.walk).marginalUtilityOfTraveling_s + this.params.modeParams.get(TransportMode.walk).marginalUtilityOfDistance_m * distance;

		return score;
	}

	private double getPtScore(double distance, double travelTime)
	{
		double score = 0.0D;

		double distanceCost = 0.0D;
		TreeSet travelCards = ((PersonImpl)this.plan.getPerson()).getTravelcards();
		if (travelCards == null || travelCards.contains("ch-HT-mobility"))
			distanceCost = this.ftConfigGroup.getDistanceCostPtNoTravelCard();
		else if (travelCards.contains("unknown"))
			distanceCost = this.ftConfigGroup.getDistanceCostPtUnknownTravelCard();
		else {
			throw new RuntimeException("Person " + this.plan.getPerson().getId() + " has an invalid travelcard. This should never happen.");
		}
		score += this.params.modeParams.get(TransportMode.pt).marginalUtilityOfDistance_m * distanceCost / 1000.0D * distance;
		score += travelTime * this.params.modeParams.get(TransportMode.pt).marginalUtilityOfTraveling_s;
		score += score += this.ftConfigGroup.getConstPt();

		return score;
	}
}
