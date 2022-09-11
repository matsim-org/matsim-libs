package org.matsim.codeexamples.population.reducePopulationToAreaOfInterest;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;

class RunReducePopulationToAreaOfInterestExample{
	// what I want to do: go throw all persons, take their selected plan, and then:
	// * route that selected plan entirely by car
	// * find out of any of the routes go through my area of interest
	// * remove the person if not
	// yyyy Someone should try this out.  kai, jul'19
	// yyyy Someone should write a test case.  kai, jul'19


	private static final Logger log = LogManager.getLogger( RunReducePopulationToAreaOfInterestExample.class ) ;

	public static void main( String[] args ){

		Config config = ConfigUtils.loadConfig( "scenarios/equil/config.xml" ) ;
		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		// trying without controler infrastructure:
		// would need a trip router for this; difficult to plug together.

		Controler controler = new Controler( scenario ) ;

		// trying with controler infrastructure:
		controler.addOverridingModule( new AbstractModule(){
			@Override
			public void install(){
				this.addControlerListenerBinding().to( MyControlerListener.class ) ;
			}
		} ) ;

		controler.run() ;

	}

	private static class MyControlerListener implements StartupListener {

		@Inject Scenario scenario ;
		@Inject TripRouter tripRouter ;
		@Inject
		TimeInterpretation timeInterpretation;

		@Override public void notifyStartup( StartupEvent event ){

			PlanRouter planRouter = new PlanRouter( tripRouter, scenario.getActivityFacilities(), timeInterpretation );

			for( Person person : scenario.getPopulation().getPersons().values() ){
				Plan plan = person.getSelectedPlan() ;
				planRouter.run(plan) ;
				for( Leg leg : TripStructureUtils.getLegs( plan ) ){
					if ( leg.getRoute() instanceof NetworkRoute ) {
						for( Id<Link> linkId : ((NetworkRoute) leg.getRoute()).getLinkIds() ) {
							// now somehow check if in area of interest, either by checking the coordinate, or by having a precomputed list of
							// linkIds of interest.
						}
					}
				}

			}



			log.error( "stopping here since I don't need anything else") ;
			System.exit(-1) ;
		}
	}
}
