package org.matsim.integration.equilibration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.matsim.core.utils.io.IOUtils.extendUrl;

class CongestedReroutingIT{
	@RegisterExtension MatsimTestUtils utils = new MatsimTestUtils();

	@Test void testCongestedRerouting() {

		final String car2 = "car2";
		final String [] modes = new String[] { car2 };
		final Set<String> modesAsSet = CollectionUtils.stringArrayToSet( modes );

		// ===

		Config config = ConfigUtils.loadConfig( extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" ) ) ;

		config.plans().setInputFile( "plans2000.xml.gz" );

		config.controller().setCompressionType( ControllerConfigGroup.CompressionType.gzip );
		config.controller().setLastIteration( 1 );
		config.controller().setOutputDirectory( utils.getOutputDirectory() );

		config.routing().setNetworkModes( modesAsSet );

		config.qsim().setMainModes( modesAsSet );

		config.scoring().addModeParams( new ScoringConfigGroup.ModeParams( car2 ) );

		// ===

		Scenario scenario = ScenarioUtils.loadScenario( config );

		for( Link link : scenario.getNetwork().getLinks().values() ){
			link.setAllowedModes( modesAsSet );
			link.setLength( NetworkUtils.getEuclideanDistance( link.getFromNode().getCoord(), link.getToNode().getCoord()) );
		}

		for( Person person : scenario.getPopulation().getPersons().values() ){
			for( Leg leg : TripStructureUtils.getLegs( person.getSelectedPlan() ) ){
				leg.setMode( car2 );
				leg.setRoute( null );
			}
		}

		// ===

		Controller controller = ControllerUtils.createController( scenario );

		// ===

		controller.run();

		// ===

		{
			Population expected = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
			PopulationUtils.readPopulation( expected, utils.getInputDirectory() + "/output_plans.xml.gz" );

			Population actual = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
			PopulationUtils.readPopulation( actual, utils.getOutputDirectory() + "/output_plans.xml.gz" );

			for ( Id<Person> personId : expected.getPersons().keySet()) {
				double scoreReference = expected.getPersons().get(personId).getSelectedPlan().getScore();
				double scoreCurrent = actual.getPersons().get(personId).getSelectedPlan().getScore();
				assertEquals(scoreReference, scoreCurrent, 0.001, "Scores of person=" + personId + " are different");
			}
		}
		{
			String expected = utils.getInputDirectory() + "/output_events.xml.gz" ;
			String actual = utils.getOutputDirectory() + "/output_events.xml.gz" ;
			ComparisonResult result = EventsUtils.compareEventsFiles( expected, actual );
			assertEquals( ComparisonResult.FILES_ARE_EQUAL, result );
		}

	}
}
