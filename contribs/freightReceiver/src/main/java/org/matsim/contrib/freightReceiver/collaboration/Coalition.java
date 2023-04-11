package org.matsim.contrib.freightReceiver.collaboration;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freightReceiver.Receiver;
import org.matsim.utils.objectattributes.attributable.Attributable;

import java.util.Collection;

/**
 * This is an interface to create and manage carrier-receiver coalitions.
 *
 * @author wlbean
 */

public interface Coalition extends Attributable {

	void addReceiverCoalitionMember(Receiver receiver);

	void addCarrierCoalitionMember(Carrier carrier);

	void removeReceiverCoalitionMember(Receiver receiver);

	void removeCarrierCoalitionMember(Carrier carrier);

	Collection<Carrier> getCarrierCoalitionMembers();

	Collection<Receiver> getReceiverCoalitionMembers();

	void setCoalitionCost(double cost);

	double getCoalitionCost();

}
