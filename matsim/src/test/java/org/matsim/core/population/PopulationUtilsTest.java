package org.matsim.core.population;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author thibautd
 */
public class PopulationUtilsTest {

	@Test
	public void testPlanAttributesCopy() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig() );

		final Person person = population.getFactory().createPerson(Id.createPersonId( "Donald Trump"));

		final Plan plan = population.getFactory().createPlan();
		person.addPlan( plan );

		final Activity act = population.getFactory().createActivityFromCoord( "speech" , new Coord( 0 , 0 ) );
		plan.addActivity( act );

		act.getAttributes().putAttribute( "makes sense" , false );
		act.getAttributes().putAttribute( "length" , 1895L );

		final Leg leg = population.getFactory().createLeg( "SUV" );
		plan.addLeg( leg );
		plan.addActivity( population.getFactory().createActivityFromLinkId( "tweet" , Id.createLinkId( 2 )));

		leg.getAttributes().putAttribute( "mpg" , 0.000001d );


		final Plan planCopy = population.getFactory().createPlan();
		PopulationUtils.copyFromTo( plan , planCopy );

		Assert.assertEquals( "unexpected plan length",
				plan.getPlanElements().size(),
				planCopy.getPlanElements().size() );

		final Activity activityCopy = (Activity) planCopy.getPlanElements().get( 0 );

		Assert.assertEquals( "unexpected attribute",
				act.getAttributes().getAttribute( "makes sense" ),
				activityCopy.getAttributes().getAttribute( "makes sense" ) );

		Assert.assertEquals( "unexpected attribute",
				act.getAttributes().getAttribute( "length" ),
				activityCopy.getAttributes().getAttribute( "length" ) );

		final Leg legCopy = (Leg) planCopy.getPlanElements().get( 1 );

		Assert.assertEquals( "unexpected attribute",
				leg.getAttributes().getAttribute( "mpg" ),
				legCopy.getAttributes().getAttribute( "mpg" ) );
	}

}
