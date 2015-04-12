package org.matsim.contrib.pseudosimulation.distributed;

/**
 * Created by fouriep on 10/21/14.
 */
public enum CommunicationsMode {
    TRANSMIT_SCENARIO,
    TRANSMIT_TRAVEL_TIMES,
    TRANSMIT_SCORES,
    POOL_PERSONS,
    DISTRIBUTE_PERSONS,
    DUMP_PLANS,
    TRANSMIT_PLANS_TO_MASTER,
    TRANSMIT_PERFORMANCE,
    CONTINUE,
    WAIT,
    DIE
}
