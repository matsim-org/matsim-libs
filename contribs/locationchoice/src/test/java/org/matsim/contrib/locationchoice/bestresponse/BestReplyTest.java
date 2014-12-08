package org.matsim.contrib.locationchoice.bestresponse;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.DCControler;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;


public class BestReplyTest extends MatsimTestCase {
	
	private Scenario scenario;
	private DestinationChoiceBestResponseContext context;

    @Override
	protected void setUp() throws Exception {
		super.setUp();
		this.init();
	}
	
	public void testSampler() {
		DestinationSampler sampler = new DestinationSampler(
				context.getPersonsKValuesArray(), context.getFacilitiesKValuesArray(), 
				(DestinationChoiceConfigGroup) scenario.getConfig().getModule("locationchoice"));
		assertTrue(sampler.sample(context.getFacilityIndex(Id.create(1, ActivityFacility.class)), context.getPersonIndex(Id.create(1, Person.class))));
		assertTrue(!sampler.sample(context.getFacilityIndex(Id.create(1, ActivityFacility.class)), context.getPersonIndex(Id.create(2, Person.class))));
	}
	
	public void init() {
		String configFile = this.getPackageInputDirectory() + "/config.xml";
		this.scenario = ScenarioUtils.loadScenario(
				ConfigUtils.loadConfig( configFile, new DestinationChoiceConfigGroup() ));
		this.context = new DestinationChoiceBestResponseContext(this.scenario);
		this.context.init();
	}
	
	public void testRunControler() {
		String args [] = {this.getPackageInputDirectory() + "/config.xml"};
		DCControler.main(args);		
	}
}
