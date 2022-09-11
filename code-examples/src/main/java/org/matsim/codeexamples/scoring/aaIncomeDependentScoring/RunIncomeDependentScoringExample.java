package org.matsim.codeexamples.scoring.aaIncomeDependentScoring;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author thibautd
 */
class RunIncomeDependentScoringExample{
	private static final Logger log = LogManager.getLogger( RunIncomeDependentScoringExample.class ) ;
	
	static final String NET_INCOME_PER_MONTH="netIncomePerMonth" ;

	private static class IncomeDependentScoringParametersForPerson implements ScoringParametersForPerson {
		@Inject private Scenario scenario;
		@Override public ScoringParameters getScoringParameters( Person person ){

			final var builder = new ScoringParameters.Builder( scenario, person );
			// (this builder is potentially rather expensive when there are many persons and many activity types (e.g. with different durations).  I think that this can be fixed.
			// Thus keeping it here in this illustrative example.  kai, jun'22)

			final var income = (Double) person.getAttributes().getAttribute( NET_INCOME_PER_MONTH );
			builder.setMarginalUtilityOfMoney( 2500. / income );
			// (from 500 + rnd * 4000 we know that the average income will be 500. + 2000. = 2500.  Otherwise, this needs to be obtained in a different way.  kai, jun'22)

			return builder.build();
		}
	}

	public static void main(String [] args) {
		final Config config;
		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" ) );
			config.controler().setOutputDirectory( "output/ownMoneyScoring/" );
			config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		} else {
			config = ConfigUtils.loadConfig( args );
		}

		PlanCalcScoreConfigGroup.ModeParams params = new PlanCalcScoreConfigGroup.ModeParams( "car" );
		params.setDailyMonetaryConstant( 10. );
		config.planCalcScore().addModeParams( params );

		// ---

		final Scenario scenario = ScenarioUtils.loadScenario(config) ;

		// give each person an individual income:
		for (Person person : scenario.getPopulation().getPersons().values() ) {
			final double income = 500. + MatsimRandom.getRandom().nextDouble() * 4000.;
			person.getAttributes().putAttribute(NET_INCOME_PER_MONTH, income ) ;
		}

		// ---

		final Controler controler = new Controler( scenario );

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				this.bind( ScoringParametersForPerson.class ).to( IncomeDependentScoringParametersForPerson.class );
			}
		} );

		/*
		The following is the "old" version.  Disadvantage is that it replaces the scoring function ... and thus gets in the way of contribs
		that modify the scoring function (such as the bicycle contrib).  kai, jun'22

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				ScoringFunctionFactory instance = new ScoringFunctionFactory(){
					@Inject private ScoringParametersForPerson params;
					@Inject private Network network ;
					@Override public ScoringFunction createNewScoringFunction(Person person) {
						final ScoringParameters parameters = params.getScoringParameters( person );
						SumScoringFunction sumScoringFunction = new SumScoringFunction() ;
						sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(parameters));
						sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(parameters, network));
						sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(parameters));

						double income = (double) person.getAttributes().getAttribute( NET_INCOME_PER_MONTH );
						double margUtlOfMoney = 2000. / income;;
						log.warn( "margUtlOfMoney=" + margUtlOfMoney ) ;
						sumScoringFunction.addScoringFunction( new CharyparNagelMoneyScoring( margUtlOfMoney ) ) ;

						return sumScoringFunction ;
					}
				} ;
				this.bindScoringFunctionFactory().toInstance(instance) ;
			}
		});
		*/
		controler.run();
	}

//	private static Map<String,Map<String,ActivityUtilityParameters>> createActivityParametersBySubpopulation( PlanCalcScoreConfigGroup scoringConfig ){
//		// yyyy TODO move to matsim core.  kai, jun'22
//
//		final Map<String,Map<String,ActivityUtilityParameters>> actParamsBySubpop = new LinkedHashMap<>();
//		for( Map.Entry<String, PlanCalcScoreConfigGroup.ScoringParameterSet> entry : scoringConfig.getScoringParametersPerSubpopulation().entrySet() ){
//			Map<String, ActivityUtilityParameters> newParams = new TreeMap<>();
//			for ( PlanCalcScoreConfigGroup.ActivityParams params : entry.getValue().getActivityParams()) {
//				ActivityUtilityParameters.Builder factory = new ActivityUtilityParameters.Builder(params) ;
//				newParams.put(params.getActivityType(), factory.build() ) ;
//			}
//			actParamsBySubpop.put( entry.getKey(), newParams );
//		}
//		return actParamsBySubpop;
//	}

}
