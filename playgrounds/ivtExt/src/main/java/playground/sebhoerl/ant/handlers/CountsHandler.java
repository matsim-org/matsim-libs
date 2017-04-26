package playground.sebhoerl.ant.handlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import playground.sebhoerl.ant.DataFrame;
import playground.sebhoerl.av_paper.BinCalculator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CountsHandler extends AbstractHandler implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler {
    final private Map<Id<Person>, Double> departures = new HashMap();
    final private Map<Id<Person>, PersonArrivalEvent> ptArrivals = new HashMap<>();

    public CountsHandler(DataFrame data) {
        super(data);
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        String mode = event.getLegMode().equals("transit_walk") ? "pt" : event.getLegMode();

        if (!data.isOrdinaryPerson(event.getPersonId())) return;
        if (!data.modes.contains(mode)) return;

        departures.put(event.getPersonId(), event.getTime());
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        String mode = event.getLegMode().equals("transit_walk") ? "pt" : event.getLegMode();

        if (!data.isOrdinaryPerson(event.getPersonId())) return;
        if (!data.modes.contains(mode)) return;

        // PT trips are only finished when the next activity is started (to account for line switches)
        if (mode.equals("pt") && !ptArrivals.containsKey(event)) {
            ptArrivals.put(event.getPersonId(), event);
        } else {
            ptArrivals.remove(event.getPersonId());
        }

        Double departureTime = departures.remove(event.getPersonId());

        if (departureTime != null) {
            if (data.binCalculator.isCoveredValue(event.getTime())) {
                List<Integer> modeBin = data.departureCount.get(mode);
                int index = data.binCalculator.getIndex(departureTime);
                modeBin.set(index, modeBin.get(index) + 1);
            }

            if (data.binCalculator.isCoveredValue(event.getTime())) {
                List<Integer> modeBin = data.arrivalCount.get(mode);
                int index = data.binCalculator.getIndex(event.getTime());
                modeBin.set(index, modeBin.get(index) + 1);
            }

            List<Double> travellerModeBin = data.travellerCount.get(mode);

            for (BinCalculator.BinEntry entry : data.binCalculator.getBinEntriesNormalized(departureTime, event.getTime())) {
                travellerModeBin.set(entry.getIndex(), travellerModeBin.get(entry.getIndex()) + entry.getWeight());
            }
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (ptArrivals.containsKey(event.getPersonId()) && !event.getEventType().equals("pt interaction")) {
            handleEvent(ptArrivals.get(event.getPersonId()));
        }
    }

    @Override
    protected void finish() {
        for (PersonArrivalEvent arrival : ptArrivals.values()) {
            handleEvent(arrival);
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        // Remove AV trips which are not for the relevant operator

        if (data.isAV(event.getVehicleId().toString()) && !data.isRelevantOperator(event.getVehicleId().toString())) {
            departures.remove(event.getPersonId());
        }
    }
}
