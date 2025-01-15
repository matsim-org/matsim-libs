package org.matsim.freight.carriers;

import org.matsim.api.core.v01.Id;
import org.matsim.utils.objectattributes.attributable.Attributable;

/**
 * A job that a {@link Carrier} can do.
 * <p>
 * In a first step this is more or less a marker interface.
 * <p>
 * In the next steps it will be extended, as follows
 * 1) existing common methods of {@link CarrierShipment} and {@link
 * CarrierService} where moved up here
 * 2) some similar, but differently named methods of {@link
 * CarrierShipment} and {@link CarrierService} were renamed to the same name and moved up here
 * ...
 * future) It maybe gets generalized in way, that we only have one job definition with 1 or 2
 * location(s). This then defines, if jsprit takes the job as a service or as a shipment.
 */
public interface CarrierJob extends Attributable {
	Id<? extends CarrierJob> getId();
	int getCapacityDemand();
}
