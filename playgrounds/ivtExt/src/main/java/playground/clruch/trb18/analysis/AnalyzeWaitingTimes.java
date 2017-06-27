package playground.clruch.trb18.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import playground.sebhoerl.av_paper.BinCalculator;
import playground.sebhoerl.avtaxi.framework.AVModule;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Just a quick test to see if there are meaningful results coming from the first simulations ...
 */
public class AnalyzeWaitingTimes implements PersonDepartureEventHandler, PersonArrivalEventHandler {
    static public void main(String[] args) throws IOException {
        BinCalculator binCalculator = BinCalculator.createByInterval(0.0, 30.0 * 3600.0, 300.0);

        EventsManager eventsManager = EventsUtils.createEventsManager();

        AnalyzeWaitingTimes analyze = new AnalyzeWaitingTimes(binCalculator);
        eventsManager.addHandler(analyze);

        for (long size : new long[] { 100, 200, 300, 400, 500, 600, 700, 800, 900 }) {
            analyze.resetData();
            new MatsimEventsReader(eventsManager).readFile("output_" + size + "/output_events.xml.gz");
            (new ObjectMapper()).writeValue(new File("analysis_" + size + ".json"), analyze.getTravelTimes());
        }
    }

    final private BinCalculator binCalculator;
    final private List<List<Double>> travelTimes = new LinkedList<>();
    final private Map<Id<Person>, PersonDepartureEvent> departures = new HashMap<>();

    public AnalyzeWaitingTimes(BinCalculator binCalculator) {
        this.binCalculator = binCalculator;
        resetData();
    }

    @Override
    public void handleEvent(PersonArrivalEvent arrivalEvent) {
        PersonDepartureEvent departureEvent = departures.remove(arrivalEvent.getPersonId());

        if (departureEvent != null && binCalculator.isCoveredValue(departureEvent.getTime())) {
            travelTimes.get(binCalculator.getIndex(departureEvent.getTime())).add(arrivalEvent.getTime() - departureEvent.getTime());
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (event.getLegMode().equals(AVModule.AV_MODE)) {
            departures.put(event.getPersonId(), event);
        }
    }

    @Override
    public void reset(int iteration) {}

    public void resetData() {
        travelTimes.clear();
        departures.clear();
        for (int i = 0; i < binCalculator.getBins(); i++) travelTimes.add(new LinkedList<>());
    }

    public List<List<Double>> getTravelTimes() {
        return travelTimes;
    }
}
