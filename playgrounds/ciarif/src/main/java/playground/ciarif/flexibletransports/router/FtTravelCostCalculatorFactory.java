package playground.ciarif.flexibletransports.router;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.PersonalizableTravelDisutility;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelTime;
import playground.ciarif.flexibletransports.config.FtConfigGroup;

public class FtTravelCostCalculatorFactory
  implements TravelDisutilityFactory
{
  private FtConfigGroup ftConfigGroup = null;

  public FtTravelCostCalculatorFactory(FtConfigGroup ftConfigGroup)
  {
    super();
    this.ftConfigGroup = ftConfigGroup;
  }


//@Override
public PersonalizableTravelDisutility createTravelDisutility(
		PersonalizableTravelTime timeCalculator,
		PlanCalcScoreConfigGroup cnScoringGroup) {
	// TODO Auto-generated method stub
	  return new FtTravelTimeDistanceCostCalculator(timeCalculator, cnScoringGroup, this.ftConfigGroup);
	  
}
}
