package org.matsim.codeexamples.scoring.ownMoneyScoring;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

/**
 * @author thibautd
 */
class RunOwnMoneyScoringExample {
	private static final Logger log = LogManager.getLogger(RunOwnMoneyScoringExample.class) ;
	
	static final String NET_INCOME_PER_MONTH="netIncomePerMonth" ;

	public static void main(String [] args) {
		final Config config;
		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" ) );
			config.controler().setOutputDirectory( "output/ownMoneyScoring/" );
			config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		} else {
			config = ConfigUtils.loadConfig( args );
		}

		// ---

		final Scenario scenario = ScenarioUtils.loadScenario(config) ;

		// give each person an individual income:
		for (Person person : scenario.getPopulation().getPersons().values() ) {
			person.getAttributes().putAttribute(NET_INCOME_PER_MONTH, 500. + MatsimRandom.getRandom().nextDouble() * 4000. ) ;
		}

		// ---

		final Controler controler = new Controler( scenario );
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
		controler.run();
	}
}
