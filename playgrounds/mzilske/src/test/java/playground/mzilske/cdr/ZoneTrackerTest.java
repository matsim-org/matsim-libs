package playground.mzilske.cdr;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import playground.mzilske.cdr.ZoneTracker.LinkToZoneResolver;
import playground.mzilske.cdr.ZoneTracker.Zone;

public class ZoneTrackerTest {
	
	
	public class TestHandler implements ZoneEnterEventHandler, ZoneLeaveEventHandler {

		@Override
		public void reset(int iteration) {
			
		}

		@Override
		public void handleEvent(ZoneLeaveEvent event) {
			System.out.println(event);
		}

		@Override
		public void handleEvent(ZoneEnterEvent event) {
			System.out.println(event);
		}

	}

	@Test
	public void worksWhenLinksAreZones() {
		EventsManager linkEvents = EventsUtils.createEventsManager();
		EventsManager zoneEvents = EventsUtils.createEventsManager();
		
		Id<Person> person = Id.create("p1", Person.class);
		Id<Vehicle> vehicle = Id.create("v1", Vehicle.class);
		Id<Link> link1 = Id.create("l1", Link.class);

		ZoneTracker testee = new ZoneTracker(ScenarioUtils.createScenario(ConfigUtils.createConfig()), new LinkIsZone());
		zoneEvents.addHandler(new TestHandler());
		
		linkEvents.addHandler(testee);
		
		linkEvents.processEvent(new LinkLeaveEvent(1.0, vehicle, link1));
		linkEvents.processEvent(new LinkEnterEvent(1.0, vehicle, Id.create("l2", Link.class)));
		linkEvents.processEvent(new LinkLeaveEvent(1.0, vehicle, Id.create("l2", Link.class)));
		linkEvents.processEvent(new LinkEnterEvent(1.0, vehicle, Id.create("l3", Link.class)));
		linkEvents.processEvent(new LinkLeaveEvent(1.0, vehicle, Id.create("l3", Link.class)));
		linkEvents.processEvent(new LinkEnterEvent(1.0, vehicle, Id.create("l4", Link.class)));
		linkEvents.finishProcessing();
	}
	
	@Test
	public void worksWhenOnlyTwoLinksAreFenced() {
		EventsManager linkEvents = EventsUtils.createEventsManager();
		EventsManager zoneEvents = EventsUtils.createEventsManager();
		
		Id<Person> person = Id.create("p1", Person.class);
		Id<Vehicle> vehicle = Id.create("v1", Vehicle.class);
		final Id<Link> link1 = Id.create("l1", Link.class);
		final Id<Link> link4 = Id.create("l4", Link.class);

		ZoneTracker testee = new ZoneTracker(ScenarioUtils.createScenario(ConfigUtils.createConfig()), new LinkToZoneResolver() {

			@Override
			public Id<Zone> resolveLinkToZone(Id<Link> linkId) {
				if (linkId.equals(link4) || linkId.equals(link1)) {
					return Id.create(linkId, Zone.class);
				} else {
					return null;
				}
			}

			@Override
			public Id<Link> chooseLinkInZone(String zoneId) {
				return null;
			}
			
		});
		zoneEvents.addHandler(new TestHandler());
		
		linkEvents.addHandler(testee);
		
		linkEvents.processEvent(new LinkLeaveEvent(1.0, vehicle, link1));
		linkEvents.processEvent(new LinkEnterEvent(1.0, vehicle, Id.create("l2", Link.class)));
		linkEvents.processEvent(new LinkLeaveEvent(2.0, vehicle, Id.create("l2", Link.class)));
		linkEvents.processEvent(new LinkEnterEvent(2.0, vehicle, Id.create("l3", Link.class)));
		linkEvents.processEvent(new LinkLeaveEvent(3.0, vehicle, Id.create("l3", Link.class)));
		linkEvents.processEvent(new LinkEnterEvent(3.0, vehicle, link4));
		linkEvents.finishProcessing();
	}

}
