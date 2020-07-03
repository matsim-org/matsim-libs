package org.matsim.core.mobsim.hermes;

import javax.validation.constraints.Positive;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.misc.Time;

import java.util.Map;

public class HermesConfigGroup extends ReflectiveConfigGroup {
    public static final String NAME = "hermes";
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
    private static final String FLOW_CAPACITY_FACTOR = "flowCapacityFactor";
    private static final String STORAGE_CAPACITY_FACTOR = "storageCapacityFactor";
    private static final String STUCKTIMEPARAM = "stuckTime";
    private static final String STUCKTIMEPARAMDESC = "time in seconds.  Time after which the frontmost vehicle on a link is called `stuck' if it does not move.";
    public static final boolean DEBUG_REALMS = false;
    public static final boolean DEBUG_EVENTS = false;
    public static final boolean CONCURRENT_EVENT_PROCESSING = true;

    @Positive
    private double storageCapacityFactor = 1.0;

    @Positive
    private double flowCapacityFactor = 1.0;

    @Positive
    private int stuckTime = 15;

    public HermesConfigGroup() {
        super(NAME);
    }

    public int getEndTime() {
        return SIM_STEPS;
    }

    @StringGetter(STUCKTIMEPARAM)
    public int getStuckTime() {
        return stuckTime;
    }

    @StringSetter(STUCKTIMEPARAM)
    public void setStuckTime(int stuckTime) {
        this.stuckTime = stuckTime;
    }

    @StringSetter(FLOW_CAPACITY_FACTOR)
    public void setFlowCapacityFactor(double flowCapacityFactor) {
        this.flowCapacityFactor = flowCapacityFactor;
    }

    @StringGetter(FLOW_CAPACITY_FACTOR)
    public double getFlowCapacityFactor() {
        return this.flowCapacityFactor;
    }

    @StringSetter(STORAGE_CAPACITY_FACTOR)
    public void setStorageCapacityFactor(double storageCapacityFactor) {
        this.storageCapacityFactor = storageCapacityFactor;
    }

    @StringGetter(STORAGE_CAPACITY_FACTOR)
    public double getStorageCapacityFactor() {
        return storageCapacityFactor;
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
        comments.put(STUCKTIMEPARAM,STUCKTIMEPARAMDESC);
        return comments;
    }



    @Override
    protected void checkConsistency(Config config) {
        super.checkConsistency(config);
        if (config.parallelEventHandling().getOneThreadPerHandler()!=true && config.controler().getMobsim().equals("hermes")){
            throw new RuntimeException("Hermes should be run with one thread per handler.");
        }
    }
}
