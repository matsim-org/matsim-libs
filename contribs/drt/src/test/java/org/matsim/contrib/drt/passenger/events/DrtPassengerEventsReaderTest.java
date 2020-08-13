package org.matsim.contrib.drt.passenger.events;


import com.sun.jdi.connect.Transport;
import org.assertj.core.data.Offset;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.examples.onetaxi.RunOneTaxiExample;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsReadersTest;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class DrtPassengerEventsReaderTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void DrtPassengerEventsReaderTest() throws SAXException, ParserConfigurationException, IOException {
		EventsManager events = EventsUtils.createEventsManager();
		DrtPassengerEventsTestHandler handler = new DrtPassengerEventsTestHandler();
		events.addHandler(handler);
		events.initProcessing();
		DrtPassengerEventsReader reader = new DrtPassengerEventsReader(events);

		//these events are the output events of RunOneSharedTaxiExampleIT
		reader.readFile(utils.getClassInputDirectory() + "events.xml.gz");
		events.finishProcessing();
		Assert.assertEquals("number of read drt events", 20, handler.eventCounter);
	}


	static class DrtPassengerEventsTestHandler implements DrtRequestSubmittedEventHandler, PassengerRequestSubmittedEventHandler, PassengerRequestScheduledEventHandler, PassengerRequestRejectedEventHandler{

		int passengerCounter = 0;
		int eventCounter = 0;

		@Override
		public void handleEvent(PassengerRequestRejectedEvent event) {
			throw new RuntimeException("all PassengerRequestSubmittedEvents should be of type " + DrtRequestSubmittedEvent.class);
		}

		@Override
		public void handleEvent(PassengerRequestScheduledEvent event) {
			assertThat(event.getMode().equals(TransportMode.drt));
			assertThat(event.getPersonId().toString().equals("passenger_" + passengerCounter));
			assertThat(event.getRequestId().toString().equals("drt_" + passengerCounter));
			assertThat(event.getVehicleId().toString().equals("shared_taxi_one"));
			passengerCounter++;
			eventCounter++;
		}

		@Override
		public void handleEvent(PassengerRequestSubmittedEvent event) {
			if(! (event instanceof DrtRequestSubmittedEvent))
			throw new RuntimeException("all PassengerRequestSubmittedEvents should be of type " + DrtRequestSubmittedEvent.class);
			eventCounter++;
		}

		@Override
		public void handleEvent(DrtRequestSubmittedEvent event) {
			assertThat(event.getMode().equals(TransportMode.drt));
			assertThat(event.getPersonId().equals("passenger_" + passengerCounter));
			assertThat(event.getRequestId().equals("drt_" + passengerCounter));
			eventCounter++;
		}

		@Override
		public void reset(int iteration) {
			this.passengerCounter = 0;
		}
	}

}
