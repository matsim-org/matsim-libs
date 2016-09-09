package playground.kai.usecases.opdytsintegration.modechoice;

import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
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
			log.info( "treating sub-population with name=" + subPopName );
			ScoringParameterSet pSet = entry.getValue() ;
			for ( Entry<String, ModeParams> modeEntry : pSet.getModes().entrySet() ) {
				String modeName = modeEntry.getKey() ;
				log.info( "treating mode with name=" + modeName ) ;
				ModeParams outputModeParams = scenario.getConfig().planCalcScore().getScoringParameters( subPopName ).getModes().get(modeName) ;
				ModeParams modeParams = modeEntry.getValue() ;

				outputModeParams.setConstant( modeParams.getConstant() );
				outputModeParams.setMarginalUtilityOfTraveling( modeParams.getMarginalUtilityOfTraveling() );
				outputModeParams.setMarginalUtilityOfDistance( modeParams.getMarginalUtilityOfDistance() );
				// yyyy this is a bit dangerous since it implies that one knows which of these are relevant and which not.
				// Caused by passing the full dummyConfig into here rather than something specifically designed. 
				// But this is what would keep it flexible for general scoring params calibration, so need to hedge against it.  kai, sep'16
				
			}
		}
	}

}
