package playground.ciarif.flexibletransports.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import playground.ciarif.flexibletransports.config.FtConfigGroup;

public class FtTravelTimeDistanceCostCalculator
  implements TravelMinCost, PersonalizableTravelCost
{
  protected final TravelTime timeCalculator;
  private final double travelCostFactor;
  private final double marginalUtlOfDistance;

  public FtTravelTimeDistanceCostCalculator(TravelTime timeCalculator, CharyparNagelScoringConfigGroup cnScoringGroup, FtConfigGroup ftConfigGroup)
  {
    this.timeCalculator = timeCalculator;
    this.travelCostFactor = (-cnScoringGroup.getTraveling() / 3600.0D + cnScoringGroup.getPerforming() / 3600.0D);
    this.marginalUtlOfDistance = (ftConfigGroup.getDistanceCostCar() / 1000.0D * cnScoringGroup.getMarginalUtlOfDistanceCar());
  }

  public double getLinkMinimumTravelCost(Link link) {
    return 
      (link.getLength() / link.getFreespeed() * this.travelCostFactor - (
      this.marginalUtlOfDistance * link.getLength()));
  }

  public double getLinkTravelCost(Link link, double time) {
    double travelTime = this.timeCalculator.getLinkTravelTime(link, time);
    return (travelTime * this.travelCostFactor - (this.marginalUtlOfDistance * link.getLength()));
  }

  protected double getTravelCostFactor() {
    return this.travelCostFactor;
  }

  protected double getMarginalUtlOfDistance() {
    return this.marginalUtlOfDistance;
  }

  public void setPerson(Person person)
  {
  }
}
