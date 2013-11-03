package playground.balac.twowaycarsharing.scoring;

import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.RouteUtils;

import playground.balac.twowaycarsharing.config.FtConfigGroup;
import playground.balac.twowaycarsharing.data.MyTransportMode;



public class LegScoringFunction extends org.matsim.core.scoring.functions.CharyparNagelLegScoring
{
	private final FtConfigGroup ftConfigGroup;
	private final PlansCalcRouteConfigGroup plansCalcRouteConfigGroup;
	private PlanImpl plan;
	private Network network;
	
	
	private double cs_start = 0.0;
	private double cs_end = 0.0;
	private boolean carsharing = false;
	private Leg currentLeg;
	private static final Logger log = Logger.getLogger(LegScoringFunction.class);
	private double totalTime = 0.0;
	public LegScoringFunction(PlanImpl plan, CharyparNagelScoringParameters params, Config config, FtConfigGroup ftConfigGroup, Network network)
	{
		super(params, network);
		this.plan = plan;
		this.network = network;
		this.ftConfigGroup = ftConfigGroup;
		this.plansCalcRouteConfigGroup = config.plansCalcRoute();
	}
	@Override
	public void reset() {
		super.reset();
		totalTime = 0.0;
		cs_start = 0.0;
		cs_end = 0.0;
		carsharing = false;
	}
	
	@Override
	public void finish() {
		
			score += (totalTime) * (-2.5D/3600);   //fee for renting the car per hour    
			if (totalTime > 0 && totalTime < 1800) {  //this is used to rpevent agents from renting a car for less than 0.5h
				
				score -= 50;
			}
		
	}
	@Override
	public void startLeg(double time, Leg leg) {
		// TODO Auto-generated method stub
		
		if (!leg.getMode().equals( "carsharing" )) {
			currentLeg = leg;
			super.startLeg(time, leg);
			return;
		}
		currentLeg = leg;
		super.startLeg(time, leg);
		if (leg.getMode().equals( "carsharing" ) && carsharing == false) {
			
			cs_start = time;
			cs_end = 0.0;
			carsharing = true;
		}
	}

	@Override
	public void endLeg(double time) {
		// TODO Auto-generated method stub	
			if (!currentLeg.getMode().equals( "carsharing" )) {
				
				if (currentLeg.getMode().equals( "carsharingwalk" ) && carsharing) {
					if (cs_end - cs_start > 0)
						totalTime += (cs_end - cs_start);
					carsharing = false;
				}
				
				super.endLeg(time); return; 
			}			
			
			cs_end = time;
			super.endLeg(time);
	}
	
	
	@Override
	protected double calcLegScore(double departureTime, double arrivalTime, Leg leg)
	{
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
		else if (MyTransportMode.carsharing.equals(leg.getMode()))
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
		else if (MyTransportMode.carsharingwalk.equals(leg.getMode())) {
			
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
