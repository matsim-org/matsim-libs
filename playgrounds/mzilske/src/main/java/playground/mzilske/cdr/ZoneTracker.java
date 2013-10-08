package playground.mzilske.cdr;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;




public class ZoneTracker implements LinkEnterEventHandler {
	
	public interface LinkToZoneResolver {
		
		public Id resolveLinkToZone(Id linkId);

		public Id chooseLinkInZone(String zoneId);
		
	}
	
	private EventsManager eventsManager;
	
	private Map<Id, Id> personInZone = new HashMap<Id, Id>();

	private LinkToZoneResolver linkToZoneResolver;

	public ZoneTracker(EventsManager eventsManager, LinkToZoneResolver linkToZoneResolver, Map<Id, Id> initialPersonInZone) {
		super();
		this.eventsManager = eventsManager;
		this.linkToZoneResolver = linkToZoneResolver;
		this.personInZone.putAll(initialPersonInZone);
	}

	@Override
	public void reset(int iteration) {
		// Not resetting delegate EventsManager here. Not my job.
	}
	
	public Id getZoneForPerson(Id personId) {
		return personInZone.get(personId);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id oldZoneId = personInZone.get(event.getPersonId());
		Id newZoneId = linkToZoneResolver.resolveLinkToZone(event.getLinkId());
		if (oldZoneId != null) {
			if (! oldZoneId.equals(newZoneId)) {
				this.eventsManager.processEvent(new ZoneLeaveEvent(event.getTime(), event.getPersonId(), oldZoneId));
			}
			if (newZoneId != null ) {
				this.eventsManager.processEvent(new ZoneEnterEvent(event.getTime(), event.getPersonId(), newZoneId));
				personInZone.put(event.getPersonId(), newZoneId);
			} else {
				personInZone.remove(event.getPersonId());
			}
		} else {
			if (newZoneId != null ) {
				this.eventsManager.processEvent(new ZoneEnterEvent(event.getTime(), event.getPersonId(), newZoneId));
				personInZone.put(event.getPersonId(), newZoneId);
			}
		}
		
	}

}
