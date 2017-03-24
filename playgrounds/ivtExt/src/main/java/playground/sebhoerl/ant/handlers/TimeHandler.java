package playground.sebhoerl.ant.handlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import playground.sebhoerl.ant.DataFrame;
import playground.sebhoerl.av_paper.BinCalculator;
import playground.sebhoerl.avtaxi.framework.AVModule;

import java.util.HashMap;
import java.util.Map;

public class TimeHandler extends AbstractHandler implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, PersonArrivalEventHandler {
    private final Map<Id<Person>, Double> departures = new HashMap<>();
    private final Map<Id<Person>, Double> enterVehicleTimes = new HashMap<>();

    public TimeHandler(DataFrame data) {
        super(data);
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (event.getLegMode().equals(AVModule.AV_MODE)) {
            departures.put(event.getPersonId(), event.getTime());
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (event.getVehicleId().toString().startsWith("av_") && data.isRelevantOperator(event.getVehicleId().toString())) {
            Double departure = departures.get(event.getPersonId());

            if (departure != null) {
                data.waitingTimes.get(data.binCalculator.getIndex(departure)).add(event.getTime() - departure);
                enterVehicleTimes.put(event.getPersonId(), event.getTime());

                for (BinCalculator.BinEntry entry : data.binCalculator.getBinEntriesNormalized(departure, event.getTime())) {
                    data.waitingCount.set(entry.getIndex(), data.waitingCount.get(entry.getIndex()) + entry.getWeight());
                }
            }
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (event.getLegMode().equals(AVModule.AV_MODE)) {
            Double departure = departures.remove(event.getPersonId());
            Double enterVehicleTime = enterVehicleTimes.remove(event.getPersonId());

            if (departure != null && enterVehicleTime != null) {
                data.travelTimes.get(data.binCalculator.getIndex(departure)).add(event.getTime() - enterVehicleTime);
            }
        }
    }

    @Override
    protected void finish() {}
}
