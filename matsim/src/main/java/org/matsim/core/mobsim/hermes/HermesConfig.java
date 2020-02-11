package org.matsim.core.mobsim.hermes;

public class HermesConfig {
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
    public static final boolean SBB_SCENARIO;
    
    static {
    	if (System.getProperty("scenario") != null && System.getProperty("scenario").equals("sbb")) {
    		SBB_SCENARIO = true;
    	} else {
    		SBB_SCENARIO = false;
    	}
    }
}
