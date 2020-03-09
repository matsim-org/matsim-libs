package org.matsim.core.mobsim.hermes;

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.misc.Time;

import java.util.Map;

public class HermesConfigGroup extends ReflectiveConfigGroup {
    private static final String GROUPNAME = "hermes";
    private static final String END_TIME = "endTime";

    // Maximum number of links (limited to 24 bits in the plan)
    public static final int MAX_LINK_ID = 16777216;
    // Maximum number of stops in a link (limited to 8 bits in the plan)
    public static final int MAX_STOP_IDX = 255;
    // Maximum number of stops (limited to 20bits in the plan)
    public static final int MAX_STOP_ROUTE_ID = 65536;
    // Maximum vehicle velocity (limited to 8 bits in the plan)
    public static final int MAX_VEHICLE_VELOCITY = 255;
    // Maximum number of events per agent (limited to 16 bits in the plan)
    public static final int MAX_EVENTS_AGENT = 65536;


    // Number of simulation steps
    public static int SIM_STEPS = 30*60*60;
    // Number of ticks that are added to every agent advancing links.
    public static final int LINK_ADVANCE_DELAY = 1;

    public static final boolean DEBUG_REALMS = false;
    public static final boolean DEBUG_EVENTS = false;
    public static final boolean CONCURRENT_EVENT_PROCESSING = true;


    public HermesConfigGroup() {
        super(GROUPNAME);
    }

    public int getEndTime() {
        return SIM_STEPS;
    }

    @StringSetter(END_TIME)
    public static void setEndTime(String endTime) {
        SIM_STEPS = (int) Time.parseTime(endTime);
    }

    @StringGetter(END_TIME)
    public String getEndTimeAsString() {
        return Time.writeTime(SIM_STEPS);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(END_TIME, "Simulation End Time");
        return comments;
    }
}
