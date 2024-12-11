package org.matsim.contrib.ev.charging;

import org.matsim.contrib.ev.charging.ChargingLogic.ChargingVehicle;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;

/**
 * This interface is supposed to decide if a vehicle can be plugged right now or
 * if it needs to go to / remain in the queue. While the condition whether
 * enough of empty plugs are available is *always* checked, the presented method
 * allows to define a more complex logic beyond that.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public interface ChargingPriority {
    /**
     * The vehicle can start charging if the method returns true, otherwise it stays
     * in the queue.
     */
    boolean requestPlugNext(ChargingVehicle cv, double now);

    public interface Factory {
        ChargingPriority create(ChargerSpecification charger);
    }

    /**
     * The default charging priority: first-in first-out.
     */
    static public final Factory FIFO = charger -> (ev, now) -> true;
}