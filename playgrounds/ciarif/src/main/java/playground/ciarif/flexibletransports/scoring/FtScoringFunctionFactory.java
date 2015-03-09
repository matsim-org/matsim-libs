package playground.ciarif.flexibletransports.scoring;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.core.config.Config;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.facilities.ActivityFacilities;

import playground.ciarif.flexibletransports.config.FtConfigGroup;
import playground.meisterk.kti.scoring.ActivityScoringFunction;

public class FtScoringFunctionFactory extends CharyparNagelScoringFunctionFactory
{
  private final Config config;
  private final Network network;
  private final FtConfigGroup ftConfigGroup;
  private final TreeMap<Id, FacilityPenalty> facilityPenalties;
  private final ActivityFacilities facilities;

  public FtScoringFunctionFactory(Config config, FtConfigGroup ftConfigGroup, TreeMap<Id, FacilityPenalty> facilityPenalties, ActivityFacilities facilities, Network network)
  {
    super(config.planCalcScore(), network);
    this.network = network;
    this.config = config;
    this.ftConfigGroup = ftConfigGroup;
    this.facilityPenalties = facilityPenalties;
    this.facilities = facilities;
  }

  public ScoringFunction createNewScoringFunction(Person person)
  {
    ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

    scoringFunctionAccumulator.addScoringFunction(
      new ActivityScoringFunction(person.getSelectedPlan(), 
      new CharyparNagelScoringParameters(config.planCalcScore()), 
      this.facilityPenalties, 
      this.facilities));
    scoringFunctionAccumulator.addScoringFunction(
      new LegScoringFunction((PlanImpl)person, 
      new CharyparNagelScoringParameters(config.planCalcScore()), 
      this.config, 
      this.ftConfigGroup, network));
    scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
    scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(new CharyparNagelScoringParameters(config.planCalcScore())));

    return scoringFunctionAccumulator;
  }
}
