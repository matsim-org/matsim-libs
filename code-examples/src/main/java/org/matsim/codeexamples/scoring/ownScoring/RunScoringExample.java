package org.matsim.codeexamples.scoring.ownScoring;

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
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.net.URL;

import static org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists;

class RunScoringExample {
	private static final Logger log = LogManager.getLogger(RunScoringExample.class) ;
	
	public static void main( String [] args ) {
		
		final URL url = ExamplesUtils.getTestScenarioURL("pt-simple");
		Config config = ConfigUtils.loadConfig( IOUtils.extendUrl( url, "config.xml" ) );
		
		config.controler().setOverwriteFileSetting( deleteDirectoryIfExists );
		
		config.planCalcScore().setWriteExperiencedPlans( true );
		
		// ---
		
		final Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		// ---
		
		Controler controler = new Controler( scenario ) ;
		
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {
				this.bindScoringFunctionFactory().to( MyScoringFunctionFactory.class ) ;
			}
		} );
		
		// ---
		
		controler.run() ;
		
		
		
	}
	
	private static class MyScoringFunctionFactory implements ScoringFunctionFactory {
		@Inject private Network network;
		@Inject private ScoringParametersForPerson pparams ;
		@Inject MainModeIdentifier mainModeIdentifier ;
		@Override public ScoringFunction createNewScoringFunction( final Person person ) {
			final ScoringParameters params = pparams.getScoringParameters( person );

			SumScoringFunction ssf = new SumScoringFunction() ;
//			ssf.addScoringFunction(new CharyparNagelLegScoring(params, network ) ) ;
			ssf.addScoringFunction(new CharyparNagelActivityScoring( params ) );
			ssf.addScoringFunction(new CharyparNagelMoneyScoring(params));
			ssf.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

//			ssf.addScoringFunction( new SumScoringFunction.TripScoring() {
//				private double score = 0. ;
//				@Override
//				public void handleTrip( final TripStructureUtils.Trip trip ) {
//					log.info("") ;
//					log.info( "trip=" + trip ) ;
//					log.info( "mainMode=" + mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ) ;
//					log.info("") ;
//
//					if ( TransportMode.pt.equals( mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ) ) {
//						score -= 10. ;
//					}
//				}
//
//				@Override
//				public void finish() {
//
//				}
//
//				@Override
//				public double getScore() {
//					return score ;
//				}
//			} );
			return ssf ;
		}
	}
}
