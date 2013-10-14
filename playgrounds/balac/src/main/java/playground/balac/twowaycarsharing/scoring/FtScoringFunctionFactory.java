package playground.balac.twowaycarsharing.scoring;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCActivityScoringFunction;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCActivityWOFacilitiesScoringFunction;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCScoringFunctionFactory;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.balac.twowaycarsharing.config.FtConfigGroup;
import playground.meisterk.kti.scoring.ActivityScoringFunction;

public class FtScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory
{
  private final Config config;
  private final Network network;
  private final FtConfigGroup ftConfigGroup;
  private final ActivityFacilities facilities;

  	private final Controler controler;
	private DestinationChoiceBestResponseContext lcContext;
  
  public FtScoringFunctionFactory(Config config, Controler controler,FtConfigGroup ftConfigGroup, ActivityFacilities facilities, Network network, DestinationChoiceBestResponseContext lcContext)
  {
    super(config.planCalcScore(), network);
    this.network = network;
    this.config = config;
    this.ftConfigGroup = ftConfigGroup;
    this.facilities = facilities;
    this.controler = controler;
    this.lcContext = lcContext;
  }
  
  private boolean usingConfigParamsForScoring = true ;
	public void setUsingConfigParamsForScoring( boolean val ) {
		usingConfigParamsForScoring = val ;
	}
	
  public ScoringFunction createNewScoringFunction(Plan plan) {
	  
	  ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
    
	  scoringFunctionAccumulator.addScoringFunction(
      new LegScoringFunction((PlanImpl)plan, 
      new CharyparNagelScoringParameters(config.planCalcScore()), 
      this.config, 
      this.ftConfigGroup, network));
	  scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
	  scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
	  scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
	  //scoringFunctionAccumulator.addScoringFunction(new CarsharingScoringFunction());
	  return scoringFunctionAccumulator;
  }
}
