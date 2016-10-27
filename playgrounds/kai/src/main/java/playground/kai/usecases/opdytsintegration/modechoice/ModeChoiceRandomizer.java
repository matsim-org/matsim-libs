package playground.kai.usecases.opdytsintegration.modechoice;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;

import floetteroed.opdyts.DecisionVariableRandomizer;

final class ModeChoiceRandomizer implements DecisionVariableRandomizer<ModeChoiceDecisionVariable> {
	private static final Logger log = Logger.getLogger( ModeChoiceRandomizer.class );

	private final Scenario scenario;
	private final Random rnd ;

	ModeChoiceRandomizer(Scenario scenario) {
		this.scenario = scenario;
		this.rnd = new Random(4711) ;
		// (careful with using matsim-random since it is always the same sequence in one run)
	}

	@Override public List<ModeChoiceDecisionVariable> newRandomVariations( ModeChoiceDecisionVariable decisionVariable ) {
		final PlanCalcScoreConfigGroup oldScoringConfig = decisionVariable.getScoreConfig();
		List<ModeChoiceDecisionVariable> result = new ArrayList<>() ;
		{
			PlanCalcScoreConfigGroup newScoringConfig1 = new PlanCalcScoreConfigGroup() ;
			PlanCalcScoreConfigGroup newScoringConfig2 = new PlanCalcScoreConfigGroup() ;
			for ( String mode : oldScoringConfig.getModes().keySet() ) {
				ModeParams oldModeParams = oldScoringConfig.getModes().get(mode) ;
				if ( TransportMode.car.equals(mode ) ) { // we leave car alone
					newScoringConfig1.addModeParams( oldModeParams ) ;
				} else {
					ModeParams newModeParams1 = new ModeParams(mode) ;
					ModeParams newModeParams2 = new ModeParams(mode) ;

					final double rnd1 = 0. * rnd.nextDouble() ;
//					log.warn( "mode=" + mode + "; rnd1=" + rnd1 + "; oldConstant=" + oldModeParams.getConstant() ) ;
					newModeParams1.setConstant( oldModeParams.getConstant() + rnd1 );
					newModeParams2.setConstant( oldModeParams.getConstant() - rnd1 );
//					log.warn( "newConstant1=" + newModeParams1.getConstant() + "; newConstant2=" + newModeParams2.getConstant() ) ;

					final double rnd2 = 0. * rnd.nextDouble() ;
					newModeParams1.setMarginalUtilityOfDistance( oldModeParams.getMarginalUtilityOfDistance() + rnd2 );
					newModeParams2.setMarginalUtilityOfDistance( oldModeParams.getMarginalUtilityOfDistance() - rnd2 );
					
					final double rnd3 = 1. * rnd.nextDouble() ;
					newModeParams1.setMarginalUtilityOfTraveling( oldModeParams.getMarginalUtilityOfTraveling() + rnd3 );
					newModeParams2.setMarginalUtilityOfTraveling( oldModeParams.getMarginalUtilityOfTraveling() - rnd3 );
					
					newModeParams1.setMonetaryDistanceRate(oldModeParams.getMonetaryDistanceRate());
					newModeParams2.setMonetaryDistanceRate(oldModeParams.getMonetaryDistanceRate());

					newScoringConfig1.addModeParams(newModeParams1);
					newScoringConfig2.addModeParams(newModeParams2);

//					log.warn( "newConstant1=" + newScoringConfig1.getModes().get(mode).getConstant() + 
//							"; newConstant2 =" + newScoringConfig2.getModes().get(mode).getConstant() ) ;
				}
			}
			result.add( new ModeChoiceDecisionVariable( newScoringConfig1, this.scenario ) ) ;
			result.add( new ModeChoiceDecisionVariable( newScoringConfig2, this.scenario ) ) ;
		}
		log.warn("giving the following to opdyts:" ) ;
		for ( ModeChoiceDecisionVariable var : result ) {
			log.warn( var.toString() );
		}
//		System.exit(-1);
		return result ;
	}
}