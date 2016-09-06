package org.matsim.core.population.io;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class PopulationV6IOTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testEmptyAttributesIO() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig() );

		final Person person = population.getFactory().createPerson(Id.createPersonId( "Donald Trump"));
		population.addPerson( person );

		final String file = utils.getOutputDirectory()+"/population.xml";
		new PopulationWriter( population ).writeV6( file );

		// just check everything works without attributes (dtd validation etc)
		final Scenario readScenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( readScenario ).readFile( file );
	}

	@Test
	public void testAttributesIO() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig() );

		final Person person = population.getFactory().createPerson(Id.createPersonId( "Donald Trump"));
		population.addPerson( person );

		person.getAttributes().putAttribute( "brain" , false );
		person.getAttributes().putAttribute( "party" , "republican" );

		final String file = utils.getOutputDirectory()+"/population.xml";
		new PopulationWriter( population ).writeV6( file );

		final Scenario readScenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( readScenario ).readFile( file );

		final Person readPerson = readScenario.getPopulation().getPersons().get( Id.createPersonId( "Donald Trump" ) );

		Assert.assertEquals( "Unexpected boolean attribute in " + readPerson.getAttributes(),
				person.getAttributes().getAttribute( "brain" ) ,
				readPerson.getAttributes().getAttribute( "brain" ) );

		Assert.assertEquals( "Unexpected String attribute in " + readPerson.getAttributes(),
				person.getAttributes().getAttribute( "party" ) ,
				readPerson.getAttributes().getAttribute( "party" ) );
	}
}
