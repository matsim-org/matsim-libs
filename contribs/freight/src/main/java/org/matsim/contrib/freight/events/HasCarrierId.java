package org.matsim.contrib.freight.events;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;

/**
 * @author Kai Martins-Turner (kturner)
 */
public interface HasCarrierId {
	String ATTRIBUTE_CARRIER_ID = "carrierId";

	Id<Carrier> getCarrierId();
}
