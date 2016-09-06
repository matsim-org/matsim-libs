package org.matsim.core.scenario;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

/**
 * @author thibautd
 */
public class ScenarioByConfigInjectionTest {
	private static Logger log = Logger.getLogger( ScenarioByConfigInjectionTest.class );
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testAttributeConvertersAreInjected() {
		log.info( "create test scenario" );
		final Config config = createTestScenario();
		log.info( "create injector" );
		com.google.inject.Injector injector =
				Injector.createInjector(
						config,
						new ScenarioByConfigModule(),
						new AbstractModule() {
							@Override
							public void install() {
								addAttributeConverterBinding( StupidClass.class ).to( StupidClassConverter.class );
							}
						});

		log.info( "Load test scenario via injection" );
		final Scenario scenario = injector.getInstance( Scenario.class );

		log.info( "get object attribute" );
		Object stupid = scenario.getPopulation().getPersonAttributes().getAttribute( "1" , "stupidAttribute" );

		// TODO test for ALL attribute containers...
		Assert.assertEquals(
				"Unexpected type of read in attribute",
				StupidClass.class,
				stupid.getClass() );

		log.info( "get person attribute" );
		stupid = scenario.getPopulation()
				.getPersons()
				.get( Id.createPersonId( 1 ) )
				.getAttributes()
				.getAttribute( "otherAttribute" );

		Assert.assertEquals(
				"Unexpected type of read in attribute",
				StupidClass.class,
				stupid.getClass() );

		log.info( "get activity attribute" );
		stupid = scenario.getPopulation()
				.getPersons()
				.get( Id.createPersonId( 1 ) )
				.getSelectedPlan()
				.getPlanElements()
				.get( 0 )
				.getAttributes()
				.getAttribute( "actAttribute" );

		Assert.assertEquals(
				"Unexpected type of read in attribute",
				StupidClass.class,
				stupid.getClass() );

		log.info( "get leg attribute" );
		stupid = scenario.getPopulation()
				.getPersons()
				.get( Id.createPersonId( 1 ) )
				.getSelectedPlan()
				.getPlanElements()
				.get( 1 )
				.getAttributes()
				.getAttribute( "legAttribute" );

		Assert.assertEquals(
				"Unexpected type of read in attribute",
				StupidClass.class,
				stupid.getClass() );

	}

	private Config createTestScenario() {
		final String directory = utils.getOutputDirectory();
		final Config config = ConfigUtils.createConfig();

		final String plansFile = directory+"/plans.xml";
		final String attributeFile = directory+"/att.xml";

		config.plans().setInputFile( plansFile );
		config.plans().setInputPersonAttributeFile( attributeFile );

		final Scenario sc = ScenarioUtils.createScenario( config );
		final Person person = sc.getPopulation().getFactory().createPerson(Id.createPersonId( 1 ));
		sc.getPopulation().addPerson( person );
		sc.getPopulation().getPersonAttributes().putAttribute( "1" , "stupidAttribute" , new StupidClass() );

		person.getAttributes().putAttribute( "otherAttribute" , new StupidClass() );

		final Plan plan = sc.getPopulation().getFactory().createPlan();
		person.addPlan( plan );

		final Activity activity = sc.getPopulation().getFactory().createActivityFromCoord( "type" , new Coord( 0 , 0 ) );
		plan.addActivity( activity );

		activity.getAttributes().putAttribute( "actAttribute" , new StupidClass() );

		final Leg leg = sc.getPopulation().getFactory().createLeg( "mode" );
		plan.addLeg( leg );

		leg.getAttributes().putAttribute( "legAttribute" , new StupidClass() );

		plan.addActivity( sc.getPopulation().getFactory().createActivityFromCoord( "type" , new Coord( 0 , 0 )) );

		final PopulationWriter popWriter = new PopulationWriter( sc.getPopulation() , sc.getNetwork() );
		popWriter.putAttributeConverter( StupidClass.class , new StupidClassConverter() );
		popWriter.writeV6( plansFile );
		final ObjectAttributesXmlWriter writer = new ObjectAttributesXmlWriter(sc.getPopulation().getPersonAttributes());
		writer.putAttributeConverter( StupidClass.class , new StupidClassConverter() );
		writer.writeFile( attributeFile );

		return config;
	}

	private static class StupidClass {}

	private static class StupidClassConverter implements AttributeConverter<StupidClass> {
		@Override
		public StupidClass convert(String value) {
			return new StupidClass();
		}

		@Override
		public String convertToString(Object o) {
			return "just some stupid instance";
		}
	}
}
