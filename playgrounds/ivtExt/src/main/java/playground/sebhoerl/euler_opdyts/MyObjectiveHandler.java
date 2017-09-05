package playground.sebhoerl.euler_opdyts;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;

import floetteroed.utilities.math.Vector;

public class MyObjectiveHandler implements RemoteStateHandler, PersonDepartureEventHandler {
    private Map<String, Long> departures;

    public MyObjectiveHandler() {
        reset(0);
    }

    @Override
    public void reset(int iteration) {
        departures = new HashMap<>();
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        String mode = event.getLegMode();

        if (!departures.containsKey(mode)) {
            departures.put(mode, (long) 1);
        } else {
            departures.put(mode, departures.get(mode) + 1);
        }
    }

    @Override
    public Vector getState() {
        long sum = 0;

        for (Long partial : departures.values()) {
            sum += partial;
        }

        return new Vector(new double[] { (double) departures.get("car") / (double) sum });
    }
}
