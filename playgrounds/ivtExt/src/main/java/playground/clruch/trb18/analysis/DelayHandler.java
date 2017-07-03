package playground.clruch.trb18.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.pt.PtConstants;
import playground.sebhoerl.av_paper.BinCalculator;
import playground.sebhoerl.avtaxi.framework.AVModule;

import java.util.*;

public class DelayHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {
    final private BinCalculator binCalculator;
    final private DataFrame dataFrame;

    private final Map<Id<Person>, Queue<ReferenceReader.ReferenceTrip>> referenceTravelTimes;
    private final Map<Id<Person>, Double> departureTimes = new HashMap<>();

    public DelayHandler(DataFrame dataFrame, BinCalculator binCalculator, Map<Id<Person>, Queue<ReferenceReader.ReferenceTrip>> referenceTravelTimes) {
        this.referenceTravelTimes = referenceTravelTimes;
        this.binCalculator = binCalculator;
        this.dataFrame = dataFrame;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (event.getLegMode().equals(AVModule.AV_MODE)) {
            departureTimes.put(event.getPersonId(), event.getTime());
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        Double departureTime = departureTimes.remove(event.getPersonId());
        Queue<ReferenceReader.ReferenceTrip> queue = referenceTravelTimes.get(event.getPersonId());

        if (departureTime != null && queue != null) {
            double travelTime = event.getTime() - departureTime;
            ReferenceReader.ReferenceTrip referenceTrip = queue.poll();

            if (referenceTrip != null) {
                if (binCalculator.isCoveredValue(departureTime)) {
                    int index = binCalculator.getIndex(departureTime);

                    if (referenceTrip.mode.equals("car")) {
                        dataFrame.travelTimeDelaysCar.get(index).add(travelTime - referenceTrip.travelTime);
                    } else if (referenceTrip.mode.equals("pt")) {
                        dataFrame.travelTimeDelaysPt.get(index).add(travelTime - referenceTrip.travelTime);
                    } else {
                        System.err.println("something is wrong 2..." + event.getPersonId());
                    }
                }
            } else {
                System.err.println("something is wrong..." + event.getPersonId());
            }
        }
    }

    @Override
    public void reset(int iteration) {

    }

    public void finish() {
        dataFrame.numberOfUnmeasurableDelays += departureTimes.size();
    }
}
