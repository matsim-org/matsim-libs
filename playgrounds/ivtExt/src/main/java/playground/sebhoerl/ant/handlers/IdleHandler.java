package playground.sebhoerl.ant.handlers;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;

import playground.sebhoerl.ant.DataFrame;
import playground.sebhoerl.av_paper.BinCalculator;

public class IdleHandler extends AbstractHandler implements ActivityStartEventHandler, ActivityEndEventHandler {
    final private Map<Id<Person>, Double> startTimes = new HashMap<>();

    public IdleHandler(DataFrame data) {
        super(data);
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (event.getActType().equals("AVStay")) {
            startTimes.put(event.getPersonId(), event.getTime());
        }
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (event.getActType().equals("AVStay")) {
            Double startTime = startTimes.remove(event.getPersonId());

            //if (startTime != null) {
                for (BinCalculator.BinEntry entry : data.binCalculator.getBinEntriesNormalized(startTime, event.getTime())) {
                    data.idleAVs.set(entry.getIndex(), data.idleAVs.get(entry.getIndex()) + entry.getWeight());
                }
            //}
        }
    }

    @Override
    protected void finish() {}
}
