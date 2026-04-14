package org.matsim.contrib.perceivedsafety;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

/**
 * an interface to make PerceivedSafety scoring bindable.
 * @author simei94
 */
public interface AdditionalPerceivedSafetyLinkScore {
    /**
     * method to calculate link based perceived safety scores (for network modes).
     */
    double computeLinkBasedScore(Link link, Id<Vehicle> vehicleId);
    /**
     * method to calculate teleportation based perceived safety scores (for teleported modes).
     */
    double computeTeleportationBasedScore(double distance, String mode);
    /**
     * method to calculate link based perceived safety scores (for network modes).
     */
    double computePerceivedSafetyValueOnLink(Link link, String mode, int threshold);
}
