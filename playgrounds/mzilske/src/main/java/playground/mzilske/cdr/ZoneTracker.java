package playground.mzilske.cdr;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;




public class ZoneTracker implements LinkEnterEventHandler {
	
	public interface LinkToZoneResolver {
		
		public Id<Zone> resolveLinkToZone(Id<Link> linkId);

		public Id<Link> chooseLinkInZone(String zoneId);
		
	}
	
	public final static class Zone {
	}
	
	private EventsManager eventsManager;
	
	private Map<Id<Person>, Id<Zone>> personInZone = new HashMap<>();

	private LinkToZoneResolver linkToZoneResolver;

	public ZoneTracker(EventsManager eventsManager, LinkToZoneResolver linkToZoneResolver, Map<Id<Person>, Id<Zone>> initialPersonInZone) {
		super();
		this.eventsManager = eventsManager;
		this.linkToZoneResolver = linkToZoneResolver;
		this.personInZone.putAll(initialPersonInZone);
	}

	@Override
	public void reset(int iteration) {
		// Not resetting delegate EventsManager here. Not my job.
	}
	
	public Id<Zone> getZoneForPerson(Id<Person> personId) {
		return personInZone.get(personId);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Zone> oldZoneId = personInZone.get(event.getPersonId());
		Id<Zone> newZoneId = linkToZoneResolver.resolveLinkToZone(event.getLinkId());
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
