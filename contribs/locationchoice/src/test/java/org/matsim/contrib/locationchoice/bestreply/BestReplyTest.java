package org.matsim.contrib.locationchoice.bestreply;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.bestresponse.DestinationSampler;
import org.matsim.contrib.locationchoice.bestresponse.LocationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.ComputeMaxEpsilons;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;


public class BestReplyTest extends MatsimTestCase {
	
	private Scenario scenario;
	private LocationChoiceBestResponseContext context;
	
	private static final Logger log = Logger.getLogger(BestReplyTest.class);
	
	protected void setUp() throws Exception {
		super.setUp();
		this.init();
	}
		
	public void testSampler() {
		DestinationSampler sampler = new DestinationSampler(
				context.getPersonsKValues(), context.getFacilitiesKValues(), scenario.getConfig().locationchoice());
	}
	
	public void init() {
		String configFile = this.getPackageInputDirectory() + "/config.xml";
		this.scenario = (ScenarioImpl) ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
		this.context = new LocationChoiceBestResponseContext(this.scenario);
	}
}
