
package org.matsim.freight.receiver.collaboration;

import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.receiver.Receiver;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This class implements a coalition between carriers and receivers.
 *
 * @author wlbean
 */
final class MutableCoalition implements Coalition {
	//private final Logger log = LogManager.getLogger(MutableCoalition.class);
	private double coalitionCost = 0.0;
	private final Attributes attributes = new AttributesImpl();
	private final ArrayList<Receiver> receiverMembers = new ArrayList<>();
	private final ArrayList<Carrier> carrierMembers = new ArrayList<>();


	/* package-private */ MutableCoalition(){

	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}


	@Override
	public double getCoalitionCost() {
		return this.coalitionCost;
	}


	@Override
	public void setCoalitionCost(double cost) {
		this.coalitionCost = cost;

	}

	@Override
	public void addReceiverCoalitionMember(Receiver receiver) {
		receiverMembers.add(receiver);
	}


	@Override
	public void addCarrierCoalitionMember(Carrier carrier) {
		carrierMembers.add(carrier);
	}

	@Override
	public void removeReceiverCoalitionMember(Receiver receiver) {
		receiverMembers.remove(receiver);
	}

	@Override
	public void removeCarrierCoalitionMember(Carrier carrier) {
		carrierMembers.remove(carrier);
	}

	@Override
	public Collection<Carrier> getCarrierCoalitionMembers() {
		return this.carrierMembers;
	}

	@Override
	public Collection<Receiver> getReceiverCoalitionMembers() {
		return this.receiverMembers;
	}




}
