package org.matsim.core.scenario;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
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

		log.info( "get attribute" );
		final Object stupid = scenario.getPopulation().getPersonAttributes().getAttribute( "1" , "stupidAttribute" );

		log.info( "Test..." );
		// TODO test for ALL attribute containers...
		Assert.assertEquals(
				"Unexpected type of read in attribute",
				StupidClass.class,
				stupid.getClass() );
		log.info( "... passed!" );
	}

	private Config createTestScenario() {
		final String directory = utils.getOutputDirectory();
		final Config config = ConfigUtils.createConfig();

		final String plansFile = directory+"/plans.xml";
		final String attributeFile = directory+"/att.xml";

		config.plans().setInputFile( plansFile );
		config.plans().setInputPersonAttributeFile( attributeFile );

		final Scenario sc = ScenarioUtils.createScenario( config );
		sc.getPopulation().addPerson( sc.getPopulation().getFactory().createPerson(Id.createPersonId( 1 )));
		sc.getPopulation().getPersonAttributes().putAttribute( "1" , "stupidAttribute" , new StupidClass() );

		new PopulationWriter( sc.getPopulation() , sc.getNetwork() ).write( plansFile );
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
