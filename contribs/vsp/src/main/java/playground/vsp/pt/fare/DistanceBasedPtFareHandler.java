package playground.vsp.pt.fare;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.HashMap;
import java.util.Map;

public class DistanceBasedPtFareHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler {
    @Inject
    private EventsManager events;

    private final double baseFare;
    private final double distanceFare;
    private final Network network;

    private final Map<Id<Person>, Id<Link>> personDepartureMap = new HashMap<>();
    private final Map<Id<Person>, Id<Link>> personArrivalMap = new HashMap<>();

    public DistanceBasedPtFareHandler(DistanceBasedPtFareParams params,Network network){
        this.baseFare = params.getBaseFare();
        this.distanceFare = params.getDistanceFare();
        this.network = network;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (event.getLegMode().equals(TransportMode.pt)){
            if (!personDepartureMap.containsKey(event.getPersonId())){
                personDepartureMap.put(event.getPersonId(), event.getLinkId()); // Departure place is fixed
            }
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (event.getLegMode().equals(TransportMode.pt)){
            personArrivalMap.put(event.getPersonId(), event.getLinkId()); // Arrival place can be updated (e.g. transfer to another PT line)
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (!StageActivityTypeIdentifier.isStageActivity(event.getActType())){
            Id<Person> personId = event.getPersonId();
            if (personDepartureMap.containsKey(personId)){
                if (!personArrivalMap.containsKey(personId)){
                    throw new RuntimeException("A person has departed but not arrived. This should not happen! " +
                            "Please make sure the event is read/handled in the normal sequence");
                }
                Link fromLink = network.getLinks().get(personDepartureMap.get(personId));
                Link toLink = network.getLinks().get(personArrivalMap.get(personId));
                double distance = CoordUtils.calcEuclideanDistance(fromLink.getToNode().getCoord(), toLink.getToNode().getCoord());
                double fare = baseFare + distance * distanceFare;

                // charge fare to the person
                events.processEvent(
                        new PersonMoneyEvent(event.getTime(), event.getPersonId(), -fare,
                                DistanceBasedPtFareParams.PT_DISTANCE_BASED_FARE, TransportMode.pt, event.getPersonId().toString()));

                personDepartureMap.remove(personId);
                personArrivalMap.remove(personId);
            }
        }
    }

    @Override
    public void reset(int iteration) {
        personDepartureMap.clear();
        personArrivalMap.clear();
    }
}
