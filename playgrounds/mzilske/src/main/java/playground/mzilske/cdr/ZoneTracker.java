package playground.mzilske.cdr;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class ZoneTracker implements LinkEnterEventHandler {

    public interface LinkToZoneResolver {

        public Id<Zone> resolveLinkToZone(Id<Link> linkId);

        public Id<Link> chooseLinkInZone(String zoneId);

    }

    public final static class Zone {
    }


    private Map<Id<Person>, Id<Zone>> personInZone = new HashMap<>();

    private LinkToZoneResolver linkToZoneResolver;

    @Inject
    ZoneTracker(Scenario scenario, LinkToZoneResolver linkToZoneResolver) {
        this.linkToZoneResolver = linkToZoneResolver;
        Map<Id<Person>, Id<ZoneTracker.Zone>> initialPersonInZone = new HashMap<>();
        for (Person p : scenario.getPopulation().getPersons().values()) {
            Id<Link> linkId = ((Activity) p.getSelectedPlan().getPlanElements().get(0)).getLinkId();
            initialPersonInZone.put(p.getId(), this.linkToZoneResolver.resolveLinkToZone(linkId));
        }
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
        Id<Zone> oldZoneId = personInZone.get(event.getDriverId());
        Id<Zone> newZoneId = linkToZoneResolver.resolveLinkToZone(event.getLinkId());
        if (oldZoneId != null) {
            if (!oldZoneId.equals(newZoneId)) {
                // this.eventsManager.processEvent(new ZoneLeaveEvent(event.getTime(), event.getDriverId(), oldZoneId));
            }
            if (newZoneId != null) {
                // this.eventsManager.processEvent(new ZoneEnterEvent(event.getTime(), event.getDriverId(), newZoneId));
                personInZone.put(event.getDriverId(), newZoneId);
            } else {
                personInZone.remove(event.getDriverId());
            }
        } else {
            if (newZoneId != null) {
                // this.eventsManager.processEvent(new ZoneEnterEvent(event.getTime(), event.getDriverId(), newZoneId));
                personInZone.put(event.getDriverId(), newZoneId);
            }
        }

    }

}
