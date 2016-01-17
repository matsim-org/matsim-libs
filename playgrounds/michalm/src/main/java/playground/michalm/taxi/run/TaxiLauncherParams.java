package playground.michalm.taxi.run;

import org.apache.commons.configuration.Configuration;


class TaxiLauncherParams
{
    static final String NET_FILE = "netFile";
    static final String PLANS_FILE = "plansFile";

    static final String TAXI_CUSTOMERS_FILE = "taxiCustomersFile";
    static final String TAXIS_FILE = "taxisFile";
    static final String RANKS_FILE = "ranksFile";

    static final String EVENTS_FILE = "eventsFile";
    static final String CHANGE_EVENTS_FILE = "changeEventsFile";

    static final String ONLINE_VEHICLE_TRACKER = "onlineVehicleTracker";
    static final String PREBOOK_TRIPS_BEFORE_SIMULATION = "prebookTripsBeforeSimulation";

    static final String STORAGE_CAP_FACTOR = "storageCapFactor";
    static final String FLOW_CAP_FACTOR = "flowCapFactor";

    static final String OTF_VIS = "otfVis";
    static final String DEBUG_MODE = "debugMode";

    static final String EVENTS_OUT_FILE = "eventsOutFile";
    static final String TAXI_STATS_FILE = "taxiStatsFile";
    static final String DETAILED_TAXI_STATS_DIR = "detailedTaxiStatsDir";
    static final String E_TAXI_STATS_FILE = "eTaxiStatsFile";
    static final String MULTI_RUN_STATS_DIR = "multiRunStats";

    final String netFile;
    final String plansFile;

    final String taxiCustomersFile;
    final String taxisFile;
    final String ranksFile;

    final String eventsFile;
    final String changeEventsFile;

    final boolean onlineVehicleTracker;
    final boolean prebookTripsBeforeSimulation;
    final double storageCapFactor;
    final double flowCapFactor;

    final boolean otfVis;
    final boolean debugMode;

    final String eventsOutFile;
    final String taxiStatsFile;
    final String detailedTaxiStatsDir;
    final String eTaxiStatsFile;
    final String multiRunStatsDir;


    TaxiLauncherParams(Configuration config)
    {
        netFile = config.getString(NET_FILE);
        plansFile = config.getString(PLANS_FILE);

        taxiCustomersFile = config.getString(TAXI_CUSTOMERS_FILE, null);
        taxisFile = config.getString(TAXIS_FILE);
        ranksFile = config.getString(RANKS_FILE, null);

        eventsFile = config.getString(EVENTS_FILE);
        changeEventsFile = config.getString(CHANGE_EVENTS_FILE);

        //these params influence the simulation
        onlineVehicleTracker = config.getBoolean(ONLINE_VEHICLE_TRACKER);
        prebookTripsBeforeSimulation = config.getBoolean(PREBOOK_TRIPS_BEFORE_SIMULATION, false);
        storageCapFactor = config.getDouble(STORAGE_CAP_FACTOR, 1.);
        flowCapFactor = config.getDouble(FLOW_CAP_FACTOR, 1.);

        otfVis = config.getBoolean(OTF_VIS, false);
        debugMode = config.getBoolean(DEBUG_MODE, false);

        eventsOutFile = config.getString(EVENTS_OUT_FILE, null);
        taxiStatsFile = config.getString(TAXI_STATS_FILE, null);
        detailedTaxiStatsDir = config.getString(DETAILED_TAXI_STATS_DIR, null);
        eTaxiStatsFile = config.getString(E_TAXI_STATS_FILE, null);
        multiRunStatsDir = config.getString(MULTI_RUN_STATS_DIR, null);
    }
}