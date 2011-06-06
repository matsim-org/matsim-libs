package playground.ciarif.flexibletransports.scoring;

import java.util.TreeSet;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import playground.ciarif.flexibletransports.config.FtConfigGroup;
import playground.ciarif.flexibletransports.data.MyTransportMode;
import playground.ciarif.flexibletransports.router.FtCarSharingRoute;
import playground.ciarif.flexibletransports.router.FtPtRoute;
import playground.ciarif.flexibletransports.router.PlansCalcRouteFT;
import playground.meisterk.kti.router.PlansCalcRouteKti;

public class LegScoringFunction extends org.matsim.core.scoring.charyparNagel.LegScoringFunction
{
  private final FtConfigGroup ftConfigGroup;
  private final PlansCalcRouteConfigGroup plansCalcRouteConfigGroup;
  private static final Logger log = Logger.getLogger(LegScoringFunction.class);

  public LegScoringFunction(PlanImpl plan, CharyparNagelScoringParameters params, Config config, FtConfigGroup ftConfigGroup)
  {
    super(plan, params);
    this.ftConfigGroup = ftConfigGroup;
    this.plansCalcRouteConfigGroup = config.plansCalcRoute();
  }

  @Override
  protected double calcLegScore(double departureTime, double arrivalTime, Leg leg)
  {
    double tmpScore = 0.0D;
    double travelTime = arrivalTime - departureTime;

    double dist = 0.0D;
    ActivityImpl actPrev = (ActivityImpl)((PlanImpl)this.plan).getPreviousActivity(leg);
    ActivityImpl actNext = (ActivityImpl)((PlanImpl)this.plan).getNextActivity(leg);

    if (TransportMode.car.equals(leg.getMode()))
    {
      tmpScore += this.ftConfigGroup.getConstCar();

      if (this.params.marginalUtilityOfDistanceCar_m != 0.0D) {
        Route route = leg.getRoute();
        dist = route.getDistance();
        tmpScore += this.params.marginalUtilityOfDistanceCar_m * this.ftConfigGroup.getDistanceCostCar() / 1000.0D * dist;
      }
      tmpScore += travelTime * this.params.marginalUtilityOfTraveling_s;
    }
    else if (MyTransportMode.carsharing.equals(leg.getMode()))
    {
      dist = ((FtCarSharingRoute)leg.getRoute()).calcAccessDistance(actPrev) + ((FtCarSharingRoute)leg.getRoute()).calcEgressDistance(actNext);

      travelTime = PlansCalcRouteFT.getAccessEgressTime(dist, this.plansCalcRouteConfigGroup);

      tmpScore += getWalkScore(dist, travelTime);

      if (this.params.marginalUtilityOfDistanceCar_m != 0.0D)
      {
        dist = ((FtCarSharingRoute)leg.getRoute()).calcCarDistance(actNext);
        tmpScore += this.params.marginalUtilityOfDistanceCar_m * this.ftConfigGroup.getDistanceCostCar() / 1000.0D * dist;
      }
      travelTime = arrivalTime - departureTime;
      tmpScore += travelTime * this.params.marginalUtilityOfTraveling_s;
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
        dist = leg.getRoute().getDistance();
        tmpScore += getPtScore(dist, travelTime);
      //}

    }
    else if (TransportMode.walk.equals(leg.getMode()))
    {
      if (this.params.marginalUtilityOfDistanceWalk_m != 0.0D) {
        dist = leg.getRoute().getDistance();
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
        dist = 1.5D * leg.getRoute().getDistance();
      }
      travelTime *= 3.0D;

      tmpScore += getRideScore(dist, travelTime);
    }
    else
    {
      if (this.params.marginalUtilityOfDistanceCar_m != 0.0D) {
        dist = leg.getRoute().getDistance();
      }

      tmpScore += travelTime * this.params.marginalUtilityOfTraveling_s + this.params.marginalUtilityOfDistanceCar_m * dist;
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

    score += travelTime * this.params.marginalUtilityOfTravelingWalk_s + this.params.marginalUtilityOfDistanceWalk_m * distance;

    return score;
  }

  private double getPtScore(double distance, double travelTime)
  {
    double score = 0.0D;

    double distanceCost = 0.0D;
    TreeSet travelCards = ((PersonImpl)this.plan.getPerson()).getTravelcards();
    if (travelCards == null)
      distanceCost = this.ftConfigGroup.getDistanceCostPtNoTravelCard();
    else if (travelCards.contains("unknown"))
      distanceCost = this.ftConfigGroup.getDistanceCostPtUnknownTravelCard();
    else {
      throw new RuntimeException("Person " + this.plan.getPerson().getId() + " has an invalid travelcard. This should never happen.");
    }
    score += this.params.marginalUtilityOfDistancePt_m * distanceCost / 1000.0D * distance;
    score += travelTime * this.params.marginalUtilityOfTravelingPT_s;
    score += score += this.ftConfigGroup.getConstPt();

    return score;
  }
}
