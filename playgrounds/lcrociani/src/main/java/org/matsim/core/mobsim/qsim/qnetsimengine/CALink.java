package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vis.snapshotwriters.VisData;

public class CALink extends AbstractQLink{

	CALink(Link link, QNetwork network) {
		super(link, network);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Link getLink() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void recalcTimeVariantAttributes(double time) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<MobsimVehicle> getAllNonParkedVehicles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VisData getVisData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	QNode getToNode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	boolean doSimStep(double now) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	void addFromUpstream(QVehicle veh) {
		// TODO Auto-generated method stub
		
	}

	@Override
	boolean isNotOfferingVehicle() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	QVehicle popFirstVehicle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	QVehicle getFirstVehicle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	double getLastMovementTimeOfFirstVehicle() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	boolean hasGreenForToLink(Id<Link> toLinkId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	boolean isAcceptingFromUpstream() {
		// TODO Auto-generated method stub
		return false;
	}

}
