package playground.sebhoerl.ant.handlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import playground.sebhoerl.ant.DataFrame;
import playground.sebhoerl.av_paper.BinCalculator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CountsHandler extends AbstractHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler {
    final private Map<Id<Person>, Double> departures = new HashMap();
    final private Map<Id<Person>, PersonArrivalEvent> ptArrivals = new HashMap<>();

    public CountsHandler(DataFrame data) {
        super(data);
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (!data.isOrdinaryPerson(event.getPersonId())) return;
        if (!data.modes.contains(event.getLegMode())) return;

        departures.put(event.getPersonId(), event.getTime());

        if (data.binCalculator.isCoveredValue(event.getTime())) {
            List<Integer> modeBin = data.departureCount.get(event.getLegMode());
            int index = data.binCalculator.getIndex(event.getTime());
            modeBin.set(index, modeBin.get(index) + 1);
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (!data.isOrdinaryPerson(event.getPersonId())) return;
        if (!data.modes.contains(event.getLegMode())) return;

        // PT trips are only finished when the next activity is started (to account for line switches)
        if (event.getLegMode().equals("pt") && !ptArrivals.containsKey(event)) {
            ptArrivals.put(event.getPersonId(), event);
        } else {
            ptArrivals.remove(event.getPersonId());
        }

        if (data.binCalculator.isCoveredValue(event.getTime())) {
            List<Integer> modeBin = data.arrivalCount.get(event.getLegMode());
            int index = data.binCalculator.getIndex(event.getTime());
            modeBin.set(index, modeBin.get(index) + 1);
        }

        Double departureTime = departures.remove(event.getPersonId());

        if (departureTime != null) {
            List<Double> travellerModeBin = data.travellerCount.get(event.getLegMode());

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
}
