package org.matsim.core.population.io;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class PopulationV6IOTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testCoord3dIO() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig() );

		final Person person = population.getFactory().createPerson(Id.createPersonId( "Donald Trump"));
		population.addPerson( person );

		final Plan plan = population.getFactory().createPlan();
		person.addPlan( plan );
		plan.addActivity(population.getFactory().createActivityFromCoord( "speech" , new Coord( 0 , 0 ) ));
		plan.addActivity(population.getFactory().createActivityFromCoord( "tweet" , new Coord( 0 , 0 , -100 ) ));

		final String file = utils.getOutputDirectory()+"/population.xml";
		new PopulationWriter( population ).writeV6( file );

		final Scenario readScenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( readScenario ).readFile( file );

		final Person readPerson = readScenario.getPopulation().getPersons().get( Id.createPersonId( "Donald Trump" ) );
		final Activity readSpeach = (Activity) readPerson.getSelectedPlan().getPlanElements().get( 0 );
		final Activity readTweet = (Activity) readPerson.getSelectedPlan().getPlanElements().get( 1 );

		Assert.assertFalse( "did not expect Z value in "+readSpeach.getCoord() ,
				readSpeach.getCoord().hasZ() );

		Assert.assertTrue( "did expect T value in "+readTweet.getCoord() ,
				readTweet.getCoord().hasZ() );

		Assert.assertEquals( "unexpected Z value in "+readTweet.getCoord(),
				-100,
				readTweet.getCoord().getZ(),
				MatsimTestUtils.EPSILON );
	}

	@Test
	public void testEmptyPersonAttributesIO() {
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
	public void testPersonAttributesIO() {
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

	@Test
	public void testActivityAttributesIO() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig() );

		final Person person = population.getFactory().createPerson(Id.createPersonId( "Donald Trump"));
		population.addPerson( person );

		final Plan plan = population.getFactory().createPlan();
		person.addPlan( plan );
		final Activity act = population.getFactory().createActivityFromCoord( "speech" , new Coord( 0 , 0 ) );
		plan.addActivity( act );

		act.getAttributes().putAttribute( "makes sense" , false );
		act.getAttributes().putAttribute( "length" , 1895L );

		final String file = utils.getOutputDirectory()+"/population.xml";
		new PopulationWriter( population ).writeV6( file );

		final Scenario readScenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( readScenario ).readFile( file );

		final Person readPerson = readScenario.getPopulation().getPersons().get( Id.createPersonId( "Donald Trump" ) );
		final Activity readAct = (Activity) readPerson.getSelectedPlan().getPlanElements().get( 0 );

		Assert.assertEquals( "Unexpected boolean attribute in " + readAct.getAttributes(),
				act.getAttributes().getAttribute( "makes sense" ) ,
				readAct.getAttributes().getAttribute( "makes sense" ) );

		Assert.assertEquals( "Unexpected Long attribute in " + readAct.getAttributes(),
				act.getAttributes().getAttribute( "length" ) ,
				readAct.getAttributes().getAttribute( "length" ) );
	}

	@Test
	public void testLegAttributesIO() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig() );

		final Person person = population.getFactory().createPerson(Id.createPersonId( "Donald Trump"));
		population.addPerson( person );

		final Plan plan = population.getFactory().createPlan();
		person.addPlan( plan );
		final Leg leg = population.getFactory().createLeg( "SUV" );
		plan.addActivity( population.getFactory().createActivityFromLinkId( "speech" , Id.createLinkId( 1 )));
		plan.addLeg( leg );
		plan.addActivity( population.getFactory().createActivityFromLinkId( "tweet" , Id.createLinkId( 2 )));

		leg.getAttributes().putAttribute( "mpg" , 0.000001d );

		final String file = utils.getOutputDirectory()+"/population.xml";
		new PopulationWriter( population ).writeV6( file );

		final Scenario readScenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( readScenario ).readFile( file );

		final Person readPerson = readScenario.getPopulation().getPersons().get( Id.createPersonId( "Donald Trump" ) );
		final Leg readLeg = (Leg) readPerson.getSelectedPlan().getPlanElements().get( 1 );

		Assert.assertEquals( "Unexpected Double attribute in " + readLeg.getAttributes(),
				leg.getAttributes().getAttribute( "mpg" ) ,
				readLeg.getAttributes().getAttribute( "mpg" ) );
	}

	@Test
	public void testRouteAttributesIO() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());

		final Person person = population.getFactory().createPerson(Id.createPersonId("Donald Trump"));
		population.addPerson(person);

		final Plan plan = population.getFactory().createPlan();
		person.addPlan(plan);
		final Leg leg = population.getFactory().createLeg("SUV");
		plan.addActivity(population.getFactory().createActivityFromLinkId("speech", Id.createLinkId(1)));
		plan.addLeg(leg);
		plan.addActivity(population.getFactory().createActivityFromLinkId("tweet", Id.createLinkId(2)));

		Route route = new GenericRouteImpl(Id.createLinkId(1), Id.createLinkId(2));
		leg.setRoute(route);

		route.getAttributes().putAttribute("energyConsumption", 9999.);

		final String file = utils.getOutputDirectory() + "/population.xml";
		new PopulationWriter(population).writeV6(file);

		final Scenario readScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(readScenario).readFile(file);

		final Person readPerson = readScenario.getPopulation().getPersons().get(Id.createPersonId("Donald Trump"));
		final Leg readLeg = (Leg)readPerson.getSelectedPlan().getPlanElements().get(1);
		final GenericRouteImpl readRoute = (GenericRouteImpl)readLeg.getRoute();

		Assert.assertEquals("Unexpected Double attribute in " + readRoute.getAttributes(),
				route.getAttributes().getAttribute("energyConsumption"),
				readRoute.getAttributes().getAttribute("energyConsumption"));
	}


	@Test
	public void testPlanAttributesIO() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig() );

		final Person person = population.getFactory().createPerson(Id.createPersonId( "Donald Trump"));
		population.addPerson( person );

		final Plan plan = population.getFactory().createPlan();
		person.addPlan( plan );
		final Leg leg = population.getFactory().createLeg( "SUV" );
		plan.addActivity( population.getFactory().createActivityFromLinkId( "speech" , Id.createLinkId( 1 )));
		plan.addLeg( leg );
		plan.addActivity( population.getFactory().createActivityFromLinkId( "tweet" , Id.createLinkId( 2 )));

		plan.getAttributes().putAttribute( "beauty" , 0.000001d );

		final String file = utils.getOutputDirectory()+"/population.xml";
		new PopulationWriter( population ).writeV6( file );

		final Scenario readScenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( readScenario ).readFile( file );

		final Person readPerson = readScenario.getPopulation().getPersons().get( Id.createPersonId( "Donald Trump" ) );
		final Plan readPlan = readPerson.getSelectedPlan() ;

		Assert.assertEquals( 				plan.getAttributes().getAttribute( "beauty" ) ,
				readPlan.getAttributes().getAttribute( "beauty" ) );
	}

	@Test
	public void testPopulationAttributesIO() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig() );

		population.getAttributes().putAttribute( "type" , "candidates" );
		population.getAttributes().putAttribute( "number" , 2 );

		final String file = utils.getOutputDirectory()+"/population.xml";
		new PopulationWriter( population ).writeV6( file );

		final Scenario readScenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( readScenario ).readFile( file );

		Assert.assertEquals( "Unexpected numeric attribute in " + readScenario.getPopulation().getAttributes(),
				population.getAttributes().getAttribute( "number" ) ,
				readScenario.getPopulation().getAttributes().getAttribute( "number" ) );

		Assert.assertEquals( "Unexpected String attribute in " + readScenario.getPopulation().getAttributes(),
				population.getAttributes().getAttribute( "type" ) ,
				readScenario.getPopulation().getAttributes().getAttribute( "type" ) );
	}
}
