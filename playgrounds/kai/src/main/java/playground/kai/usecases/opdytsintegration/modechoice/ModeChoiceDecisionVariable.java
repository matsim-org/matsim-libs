package playground.kai.usecases.opdytsintegration.modechoice;

import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;

import floetteroed.opdyts.DecisionVariable;

/**
 * 
 * @author Kai Nagel based on Gunnar Flötteröd
 *
 */
final class ModeChoiceDecisionVariable implements DecisionVariable {
	private static final Logger log = Logger.getLogger( ModeChoiceDecisionVariable.class ) ;

	private final Scenario scenario;
	
	private final PlanCalcScoreConfigGroup newScoreConfig;

	ModeChoiceDecisionVariable(PlanCalcScoreConfigGroup newScoreConfig, Scenario scenario) {
		this.newScoreConfig = newScoreConfig;
		this.scenario = scenario;
	}
	
	PlanCalcScoreConfigGroup getScoreConfig() {
		return newScoreConfig ;
	}

	@Override public void implementInSimulation() {
		for ( Entry<String, ScoringParameterSet> entry : newScoreConfig.getScoringParametersPerSubpopulation().entrySet() ) {
			String subPopName = entry.getKey() ;
			log.warn( "treating sub-population with name=" + subPopName );
			ScoringParameterSet newParameterSet = entry.getValue() ;
			for ( Entry<String, ModeParams> newModeEntry : newParameterSet.getModes().entrySet() ) {
				String mode = newModeEntry.getKey() ;
				if ( !TransportMode.car.equals(mode) ) { // we leave car alone
					log.warn( "treating mode with name=" + mode ) ;
					ModeParams newModeParams = newModeEntry.getValue() ;
					ModeParams scenarioModeParams = scenario.getConfig().planCalcScore().getScoringParameters( subPopName ).getModes().get(mode) ;

					scenarioModeParams.setMarginalUtilityOfTraveling( newModeParams.getMarginalUtilityOfTraveling() );

					log.warn("new mode params:" + scenarioModeParams );
				}
			}
		}
	}

}
