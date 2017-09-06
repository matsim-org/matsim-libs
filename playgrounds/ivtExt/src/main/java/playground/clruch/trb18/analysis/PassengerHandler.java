package playground.clruch.trb18.analysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;

import playground.sebhoerl.av_paper.BinCalculator;
import playground.sebhoerl.avtaxi.framework.AVModule;

public class PassengerHandler implements PersonEntersVehicleEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {
    final private DataFrame dataFrame;
    final private BinCalculator binCalculator;

    final private Map<Id<Person>, PersonDepartureEvent> departureEvents = new HashMap<>();
    final private Map<Id<Person>, PersonEntersVehicleEvent> enterEvents = new HashMap<>();

    public PassengerHandler(DataFrame dataFrame, BinCalculator binCalculator) {
        this.dataFrame = dataFrame;
        this.binCalculator = binCalculator;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (event.getLegMode().equals(AVModule.AV_MODE)) {
            departureEvents.put(event.getPersonId(), event);
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        PersonDepartureEvent departureEvent = departureEvents.get(event.getPersonId());

        if (departureEvent != null) {
            if (binCalculator.isCoveredValue(departureEvent.getTime())) {
                int index = binCalculator.getIndex(departureEvent.getTime());

                dataFrame.waitingTimes.get(index).add(event.getTime() - departureEvent.getTime());

                for (BinCalculator.BinEntry entry : binCalculator.getBinEntriesNormalized(departureEvent.getTime(), event.getTime())) {
                    int binIndex = entry.getIndex();
                    dataFrame.numberOfWaitingRequests.set(binIndex, dataFrame.numberOfWaitingRequests.get(binIndex) + entry.getWeight());
                }
            }

            enterEvents.put(event.getPersonId(), event);
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        PersonDepartureEvent departureEvent = departureEvents.remove(event.getPersonId());
        PersonEntersVehicleEvent enterEvent = enterEvents.remove(event.getPersonId());

        if (departureEvent != null && enterEvent != null) {
            if (binCalculator.isCoveredValue(departureEvent.getTime())) {
                int index = binCalculator.getIndex(departureEvent.getTime());

                dataFrame.travelTimes.get(index).add(event.getTime() - departureEvent.getTime());
                dataFrame.numberOfServedRequests.set(index, dataFrame.numberOfServedRequests.get(index) + 1);
            }

            for (BinCalculator.BinEntry entry : binCalculator.getBinEntriesNormalized(enterEvent.getTime(), event.getTime())) {
                dataFrame.withPassengerTime.set(entry.getIndex(), dataFrame.withPassengerTime.get(entry.getIndex()) + entry.getWeight() * binCalculator.getInterval());
            }
        }
    }

    public void finish() {
        dataFrame.numberOfUnservedRequests = departureEvents.size();
    }

    @Override
    public void reset(int iteration) {}
}
