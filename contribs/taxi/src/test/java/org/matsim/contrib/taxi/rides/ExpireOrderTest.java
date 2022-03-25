package org.matsim.contrib.taxi.rides;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.benchmark.RunTaxiBenchmark;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;

import java.net.URL;
import java.util.List;

public class ExpireOrderTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	boolean eventMatches(Event actual, Event partial) {
	  if (actual.getEventType() != partial.getEventType()) {
	    return false;
	  }
	}
	void expectEvents(List<Event> actual, List<Event> expected) {

	}

	@Test
	public void testExpireOrder() {
		final Logger logger = Logger.getLogger(ExpireOrderTest.class);

		logger.warn("mielec => " + ExamplesUtils.getTestScenarioURL("mielec"));
		logger.warn("taxi-rides-test-base => " + ExamplesUtils.getTestScenarioURL("taxi-rides-test-base"));

		// TODO: create test scenario. Should be reserved for automated tests only.
		//       Grid network, dynamic vehicles, dynamic orders.
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("taxi-rides-test-base"), "config.xml");

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeTaxiConfigGroup(), new DvrpConfigGroup());
		TaxiConfigGroup taxiCfg = TaxiConfigGroup.getSingleModeTaxiConfig(config);
		taxiCfg.setBreakSimulationIfNotAllRequestsServed(false);
		taxiCfg.setMaxSearchDuration(65.0); // order should expire in 65 seconds
		taxiCfg.setRequestAcceptanceDelay(0.0);
		RuleBasedTaxiOptimizerParams ruleParams = ((RuleBasedTaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams());
		ruleParams.setReoptimizationTimeStep(1);

		// NOTE: These are already set in config.xml
		//config.plans().setInputFile("population_1.xml");
		//taxiCfg.setTaxisFile("vehicles_1.xml");

		config.controler().setOutputDirectory("abcdef");

		Controler controler = RunTaxiBenchmark.createControler(config, 1);

		EventsCollector collector = new EventsCollector();
		controler.getEvents().addHandler(collector);


		controler.getEvents().addHandler(new BasicEventHandler() {
			@Override
			public void handleEvent(Event event) {
				logger.info("handleEvent: " + event);
				switch (event.getEventType()) {
					case PassengerRequestScheduledEvent.EVENT_TYPE: {
						PassengerRequestScheduledEvent ev = (PassengerRequestScheduledEvent) event;
						Assert.assertEquals(ev.getPersonId().toString(), "passenger_1");
						break;
					}
					case PassengerDroppedOffEvent.EVENT_TYPE: {
						PassengerDroppedOffEvent ev = (PassengerDroppedOffEvent) event;
						Assert.assertEquals(ev.getPersonId().toString(), "passenger_1");
						break;
					}
					case PassengerRequestRejectedEvent.EVENT_TYPE: {
						PassengerRequestRejectedEvent ev = (PassengerRequestRejectedEvent) event;
						Assert.assertEquals(ev.getPersonId().toString(), "passenger_2");
						break;
					}
				}
			}
		});
		controler.run();

		List<Event> allEvents = collector.getEvents();
		logger.warn("AllEvents: #" + allEvents.size());
		for (Event ev : allEvents) {
			logger.warn(" - " + ev);
		}
	}
}
