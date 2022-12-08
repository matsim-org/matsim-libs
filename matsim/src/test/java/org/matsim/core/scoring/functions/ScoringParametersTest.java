package org.matsim.core.scoring.functions;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class ScoringParametersTest{
	private static final Logger log = Logger.getLogger( ScoringParametersTest.class );
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test public void testUnmodifiableCache() {
		try{
			Config config = ConfigUtils.createConfig();
			PlanCalcScoreConfigGroup config1 = config.planCalcScore();
			{
				PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams( "dummy" );
				params.setTypicalDuration( 3600. );
				config1.addActivityParams( params );
			}

			Scenario scenario = ScenarioUtils.createScenario( config );

			Person person = scenario.getPopulation().getFactory().createPerson( Id.createPersonId( "dummyPerson" ) );

			var builder = new ScoringParameters.Builder( scenario, person );
			{
				PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams("dummy");
				params.setTypicalDuration( 1800. );

				builder.setActivityParameters( "dummy", new ActivityUtilityParameters.Builder( params ).build() );
			}
			builder.build();
		} catch ( Exception ee ) {
			ee.printStackTrace();
			Assert.fail();
		}
		Assert.fail("I am not sure if it is correct that the above passes.  Thus failing the test for the time being.");
	}

}
