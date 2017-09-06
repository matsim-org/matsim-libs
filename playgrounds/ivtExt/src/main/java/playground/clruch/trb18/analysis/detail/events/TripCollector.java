package playground.clruch.trb18.analysis.detail.events;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.StageActivityTypes;

import playground.clruch.trb18.analysis.detail.Utils;

public class TripCollector implements LegCollector.Handler, ActivityCollector.Handler {
    final private StageActivityTypes stageActivityTypes;
    final private Map<Id<Person>, Trip> currentTrips = new HashMap<>();
    final private Collection<Handler> handlers = new HashSet<>();

    public interface Handler {
        void handleTrip(Trip trip);
    }

    public void addHandler(Handler handler) {
        this.handlers.add(handler);
    }

    public TripCollector(StageActivityTypes stageActivityTypes) {
        this.stageActivityTypes = stageActivityTypes;
    }

    private Trip getCurrentTrip(Id<Person> personId) {
        if (!currentTrips.containsKey(personId)) {
            return createNewTrip(personId);
        }

        return currentTrips.get(personId);
    }

    private Trip createNewTrip(Id<Person> personId) {
        currentTrips.put(personId, new Trip());
        currentTrips.get(personId).agentId = personId;
        return currentTrips.get(personId);
    }

    @Override
    public void handleActivity(ActivityCollector.Activity activity) {
        if (Utils.isValidAgent(activity.agentId)) {
            Trip currentTrip = getCurrentTrip(activity.agentId);

            if (stageActivityTypes.isStageActivity(activity.type)) {
                currentTrip.stageActivities.add(activity);
                if (currentTrip.originActivity == null) throw new RuntimeException();
            } else {
                if (currentTrip.originActivity == null) {
                    currentTrip.originActivity = activity;
                } else {
                    currentTrip.destinationActivity = activity;

                    for (Handler handler : handlers) {
                        if (currentTrip.legs.size() > 0) {
                            handler.handleTrip(currentTrip);
                        }
                    }

                    Trip newTrip = createNewTrip(activity.agentId);
                    newTrip.originActivity = activity;
                }
            }
        }
    }

    @Override
    public void handleLeg(LegCollector.Leg leg) {
        if (Utils.isValidAgent(leg.agentId)) {
            Trip currentTrip = getCurrentTrip(leg.agentId);
            currentTrip.legs.add(leg);

            if (currentTrip.originActivity == null) {
                System.err.println(currentTrip.agentId);
                throw new RuntimeException();
            }
        }
    }

    public class Trip {
        public Id<Person> agentId;

        public ActivityCollector.Activity originActivity;
        public ActivityCollector.Activity destinationActivity;

        public List<ActivityCollector.Activity> stageActivities = new LinkedList<>();
        public List<LegCollector.Leg> legs = new LinkedList<>();
    }
}
