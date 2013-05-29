package playground.mzilske.cdr;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;

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
		
		IdImpl person = new IdImpl("p1");
		IdImpl vehicle = new IdImpl("v1");
		IdImpl link1 = new IdImpl("l1");
		
		Map<Id, Id> initialZones = new HashMap<Id, Id>();
		initialZones.put(person, link1);
		ZoneTracker testee = new ZoneTracker(zoneEvents, new LinkToZoneResolver() {

			@Override
			public Id resolveLinkToZone(Id linkId) {
				return linkId;
			}
			
		}, initialZones );
		zoneEvents.addHandler(new TestHandler());
		
		linkEvents.addHandler(testee);
		
		linkEvents.processEvent(new LinkLeaveEvent(1.0, person, link1, vehicle));
		linkEvents.processEvent(new LinkEnterEvent(1.0, person, new IdImpl("l2"), vehicle));
		linkEvents.processEvent(new LinkLeaveEvent(1.0, person, new IdImpl("l2"), vehicle));
		linkEvents.processEvent(new LinkEnterEvent(1.0, person, new IdImpl("l3"), vehicle));
		linkEvents.processEvent(new LinkLeaveEvent(1.0, person, new IdImpl("l3"), vehicle));
		linkEvents.processEvent(new LinkEnterEvent(1.0, person, new IdImpl("l4"), vehicle));
		linkEvents.finishProcessing();
	}
	
	@Test
	public void worksWhenOnlyTwoLinksAreFenced() {
		EventsManager linkEvents = EventsUtils.createEventsManager();
		EventsManager zoneEvents = EventsUtils.createEventsManager();
		
		IdImpl person = new IdImpl("p1");
		IdImpl vehicle = new IdImpl("v1");
		final IdImpl link1 = new IdImpl("l1");
		final IdImpl link4 = new IdImpl("l4");
		
		Map<Id, Id> initialZones = new HashMap<Id, Id>();
		initialZones.put(person, link1);
		ZoneTracker testee = new ZoneTracker(zoneEvents, new LinkToZoneResolver() {

			@Override
			public Id resolveLinkToZone(Id linkId) {
				if (linkId.equals(link4) || linkId.equals(link1)) {
					return linkId;
				} else {
					return null;
				}
			}
			
		}, initialZones );
		zoneEvents.addHandler(new TestHandler());
		
		linkEvents.addHandler(testee);
		
		linkEvents.processEvent(new LinkLeaveEvent(1.0, person, link1, vehicle));
		linkEvents.processEvent(new LinkEnterEvent(1.0, person, new IdImpl("l2"), vehicle));
		linkEvents.processEvent(new LinkLeaveEvent(2.0, person, new IdImpl("l2"), vehicle));
		linkEvents.processEvent(new LinkEnterEvent(2.0, person, new IdImpl("l3"), vehicle));
		linkEvents.processEvent(new LinkLeaveEvent(3.0, person, new IdImpl("l3"), vehicle));
		linkEvents.processEvent(new LinkEnterEvent(3.0, person, link4, vehicle));
		linkEvents.finishProcessing();
	}

}
