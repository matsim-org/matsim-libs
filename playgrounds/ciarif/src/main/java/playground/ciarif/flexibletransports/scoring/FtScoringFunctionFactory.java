package playground.ciarif.flexibletransports.scoring;

import java.util.TreeMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.MoneyScoringFunction;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import playground.ciarif.flexibletransports.config.FtConfigGroup;
import playground.meisterk.kti.scoring.ActivityScoringFunction;

public class FtScoringFunctionFactory extends CharyparNagelScoringFunctionFactory
{
  private final Config config;
  private final FtConfigGroup ftConfigGroup;
  private final TreeMap<Id, FacilityPenalty> facilityPenalties;
  private final ActivityFacilities facilities;

  public FtScoringFunctionFactory(Config config, FtConfigGroup ftConfigGroup, TreeMap<Id, FacilityPenalty> facilityPenalties, ActivityFacilities facilities)
  {
    super(config.charyparNagelScoring());
    this.config = config;
    this.ftConfigGroup = ftConfigGroup;
    this.facilityPenalties = facilityPenalties;
    this.facilities = facilities;
  }

  public ScoringFunction createNewScoringFunction(Plan plan)
  {
    ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

    scoringFunctionAccumulator.addScoringFunction(
      new ActivityScoringFunction(plan, 
      super.getParams(), 
      this.facilityPenalties, 
      this.facilities));
    scoringFunctionAccumulator.addScoringFunction(
      new LegScoringFunction((PlanImpl)plan, 
      super.getParams(), 
      this.config, 
      this.ftConfigGroup));
    scoringFunctionAccumulator.addScoringFunction(new MoneyScoringFunction(super.getParams()));
    scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(super.getParams()));

    return scoringFunctionAccumulator;
  }
}
