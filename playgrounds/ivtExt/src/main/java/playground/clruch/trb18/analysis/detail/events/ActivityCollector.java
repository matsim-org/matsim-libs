package playground.clruch.trb18.analysis.detail.events;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import playground.clruch.trb18.analysis.detail.Utils;

public class ActivityCollector implements ActivityStartEventHandler, ActivityEndEventHandler {
    final private Map<Id<Person>, Activity> activities = new HashMap<>();
    final private Collection<Handler> handlers = new HashSet<>();

    interface Handler {
        void handleActivity(Activity activity);
    }

    public void addHandler(Handler handler) {
        this.handlers.add(handler);
    }

    public class Activity {
        public Id<Person> agentId;
        public String type;

        public double startTime;
        public double endTime;

        public Id<Link> linkId;
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (Utils.isValidAgent(event.getPersonId())) {
            Activity activity = new Activity();
            activity.agentId = event.getPersonId();
            activity.startTime = event.getTime();
            activity.type = event.getActType();
            activity.linkId = event.getLinkId();

            activities.put(event.getPersonId(), activity);
        }
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (Utils.isValidAgent(event.getPersonId())) {
            Activity activity = activities.get(event.getPersonId());

            if (activity == null) {
                activity = new Activity();
                activity.agentId = event.getPersonId();
                activity.type = event.getActType();
                activity.linkId = event.getLinkId();
            }

            activity.endTime = event.getTime();

            for (Handler handler : handlers) {
                handler.handleActivity(activity);
            }
        }
    }

    public void finish() {
        for (Activity activity : activities.values()) {
            for (Handler handler : handlers) {
                handler.handleActivity(activity);
            }
        }
    }

    @Override
    public void reset(int iteration) {

    }
}
