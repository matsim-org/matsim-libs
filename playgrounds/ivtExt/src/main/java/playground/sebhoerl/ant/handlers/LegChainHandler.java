package playground.sebhoerl.ant.handlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import playground.sebhoerl.ant.DataFrame;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LegChainHandler extends AbstractHandler implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler, PersonEntersVehicleEventHandler {
    final private Map<Id<Person>, List<String>> ongoingChains = new HashMap<>();
    final private Map<Id<Person>, String> lastLeg = new HashMap<>();

    public LegChainHandler(DataFrame data) {
        super(data);
    }

    @Override
    protected void finish() {}

    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (!data.isOrdinaryPerson(event.getPersonId())) return;

        if (event.getActType().equals("home")) {
            ongoingChains.put(event.getPersonId(), new LinkedList<>());
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (event.getActType().equals("pt interaction")) {
            return;
        }

        List<String> chain = ongoingChains.get(event.getPersonId());
        String mode = lastLeg.get(event.getPersonId());

        if (chain != null && mode != null) {
            chain.add(mode);

            if (chain.size() > 1 && event.getActType().equals("home")) {
                ongoingChains.remove(event.getPersonId());

                String full = String.join(":", chain);
                if (!data.chainCounts.containsKey(full)) {
                    data.chainCounts.put(full, (long) 0);
                }

                data.chainCounts.put(full, data.chainCounts.get(full) + 1);
            }
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (!data.isOrdinaryPerson(event.getPersonId())) return;
        if (event.getLegMode().equals("transit_walk")) return;
        lastLeg.put(event.getPersonId(), event.getLegMode());
    }


    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (data.isOrdinaryPerson(event.getPersonId()) && data.isAV(event.getVehicleId().toString()) && !data.isRelevantOperator(event.getVehicleId().toString())) {
            ongoingChains.remove(event.getPersonId());
        }
    }
}
