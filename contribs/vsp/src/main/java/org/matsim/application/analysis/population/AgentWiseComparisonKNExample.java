package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.matsim.application.analysis.population.AgentWiseComparisonKNUtils.setAnalysisPopulation;

class AgentWiseComparisonKNExample{
	private static final Logger log = LogManager.getLogger( AgentWiseComparisonKNExample.class );
	static void main() throws IOException{

		Config config = ConfigUtils.createConfig();

		modifyConfig( config );

		MutableScenario scenario = ScenarioUtils.createMutableScenario( config );

		Network network = scenario.getNetwork();
		NetworkFactory nf = network.getFactory();

		Node node1 = nf.createNode( Id.createNodeId( "node1" ), new Coord( 0., 0.) );
		Node node2 = nf.createNode( Id.createNodeId( "node2" ), new Coord( 100.*1000., 0.) );

		network.addNode( node1 );
		network.addNode( node2 );

		Link link11 = nf.createLink( Id.createLinkId( "link11" ), node1, node1 );
		Link link12 = nf.createLink( Id.createLinkId( "link12" ), node1, node2 );
		Link link22 = nf.createLink( Id.createLinkId( "link22" ), node2, node2 );

		network.addLink( link11 );
		network.addLink( link12 );
		network.addLink( link22 );

		Population basePopulation = scenario.getPopulation();

		PopulationFactory pf = basePopulation.getFactory();
		{
			Person person = pf.createPerson( Id.createPersonId( "person1" ) );
			basePopulation.addPerson( person );
			PersonUtils.setIncome( person, 2000. );
			PopulationUtils.putSubpopulation( person, "person" );

			Plan plan = pf.createPlan();
			person.addPlan( plan );

//			AddVttsEtcToActivities.setMUSE_h( plan, 6 );

			insertHWHPlan( plan, pf, TransportMode.pt, 7200, link11, link22 );

			setAnalysisPopulation( person, "true");
		}
		AgentWiseComparisonKNUtils.computeAndSetMarginalUtilitiesOfMoney( basePopulation );

		VttsCalculationBasedOnKn ccc = new VttsCalculationBasedOnKn();
		ccc.baseScenario = scenario;

		{
			com.google.inject.Injector injector = new Injector.InjectorBuilder( scenario )
													  .addStandardModules()
													  .addOverridingModule( new AbstractModule(){@Override public void install(){ bind( ScoringParametersForPerson.class ).to( IncomeDependentUtilityOfMoneyPersonScoringParameters.class ); }} )
													  .build();
			ccc.scoringFunctionFactory = injector.getInstance( ScoringFunctionFactory.class );

/*
//			ccc.tripRouter = injector.getInstance( TripRouter.class );
			// wenn man es so aufsetzt, dass das Bsp den TripRouter verwendet, dann verliert man ziemlich viel Kontrolle.  Ohne
			// TripRouter kann man allerdings die RoH nicht berechnen.  Das ist allerdings nicht so schlimm, weil das 1/2 ohnehin
			// bestenfalls als Approximation über viele Trips gilt; dieses "Example" sollte sich daher auf verbleibenden Verkehr
			// konzentrieren.  kai, dec'25

//			injector.getInstance( PrepareForMobsim.class ).run();
*/
		}

		@NotNull Table baseTable = ccc.generatePersonTableFromPopulation( basePopulation, config, null );

		System.out.println( baseTable );

		// ###

		MutableScenario policyScenario = ScenarioUtils.createMutableScenario( config );
		policyScenario.setNetwork( network );

		Population policyPopulation = policyScenario.getPopulation();
		for( Person basePerson : basePopulation.getPersons().values() ){
			Person policyPerson = pf.createPerson( basePerson.getId() );
			policyPopulation.addPerson( policyPerson );

			AttributesUtils.copyAttributesFromTo( basePerson, policyPerson );

			Plan policyPlan = pf.createPlan();
			policyPerson.addPlan( policyPlan );

			insertHWHPlan( policyPlan, pf, TransportMode.pt, 3600, link11, link22 );
		}

/*
		{
			com.google.inject.Injector injector = new Injector.InjectorBuilder( policyScenario )
													  .addStandardModules()
													  .addOverridingModule( new AbstractModule(){@Override public void install(){ bind( ScoringParametersForPerson.class ).to( IncomeDependentUtilityOfMoneyPersonScoringParameters.class ); }} )
													  .build();
 //			ccc.tripRouter2 = injector.getInstance( TripRouter.class );
			// wenn man es so aufsetzt, dass das Bsp den TripRouter verwendet, dann verliert man ziemlich viel Kontrolle.  Ohne
			// TripRouter kann man allerdings die RoH nicht berechnen.  Das ist allerdings nicht so schlimm, weil das 1/2 ohnehin
			// bestenfalls als Approximation über viele Trips gilt; dieses "Example" sollte sich daher auf verbleibenden Verkehr
			// konzentrieren.  kai, dec'25

 //			injector.getInstance( PrepareForMobsim.class ).run();
		}
*/


		Table policyTable = ccc.generatePersonTableFromPopulation( policyPopulation, config, basePopulation );

		Path path = Paths.get("outputFromAgentWiseComparisonExample" );



	}
	private static void modifyConfig( Config config ){
		config.controller().setOutputDirectory( "outputOfComparisonExample" );
		config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

		config.qsim().setFlowCapFactor( 0.01 ); // so we get more digits on the results

/*
		config.routing().setNetworkModes( Collections.emptySet() );
		config.routing().clearTeleportedModeParams();

		config.routing().addTeleportedModeParams( new TeleportedModeParams().setMode( TransportMode.car ).setTeleportedModeSpeed( 100./3.6 ).setBeelineDistanceFactor( 1.3 ) );
		config.routing().addTeleportedModeParams( new TeleportedModeParams().setMode( TransportMode.pt ).setTeleportedModeSpeed( 50./3.6 ).setBeelineDistanceFactor( 1.3 ) );
*/

		config.scoring().addActivityParams( new ActivityParams( "home_12" ).setTypicalDuration( 12. * 3600 ) );
		config.scoring().addActivityParams( new ActivityParams( "work_12" ).setTypicalDuration( 12. * 3600 ) );

		for( ModeParams modeParams : config.scoring().getModes().values() ){
			modeParams.setMarginalUtilityOfTraveling( 0. );
		}
	}
	private static void insertHWHPlan( Plan plan, PopulationFactory pf, String mode, double tTime, Link link11, Link link22 ){

		Activity home1 = pf.createActivityFromLinkId( "home_12", link11.getId() );
		plan.addActivity( home1 );
		home1.setEndTime( 8. * 3600 );

		Leg leg1 = pf.createLeg( mode );
		{
			plan.addLeg( leg1 );
			leg1.setTravelTime( tTime );

			Route route = pf.getRouteFactories().createRoute( GenericRouteImpl.class, link11.getId(), link22.getId() );
			leg1.setRoute( route );
			route.setDistance( 100. );
		}
		Activity work = pf.createActivityFromLinkId( "work_12", link22.getId() );
		plan.addActivity( work );
		work.setStartTime( home1.getEndTime().seconds() + leg1.getTravelTime().seconds() );
		work.setEndTime( 20. * 3600 );

		Leg leg2 = pf.createLeg( mode );
		{
			plan.addLeg( leg2 );
			leg2.setTravelTime( tTime );

			Route route = pf.getRouteFactories().createRoute( GenericRouteImpl.class, link22.getId(), link11.getId() );
			leg2.setRoute( route );
			route.setDistance( 100. );
		}
		Activity home2 = pf.createActivityFromLinkId( home1.getType(), home1.getLinkId() );
		plan.addActivity( home2 );
		home2.setStartTime( work.getEndTime().seconds() + leg2.getTravelTime().seconds() );
	}

}
