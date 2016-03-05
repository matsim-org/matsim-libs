package org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq;

import java.util.*;

import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

public final class PassingVehicleQ extends AbstractQueue<QVehicle> implements VehicleQ<QVehicle> {

	public PassingVehicleQ() {} // to find calls 
	
	private final Queue<QVehicle> delegate = new PriorityQueue<>(11, new Comparator<QVehicle>() {

		@Override
		public int compare(QVehicle arg0, QVehicle arg1) {
			return Double.compare(arg0.getEarliestLinkExitTime(), arg1.getEarliestLinkExitTime());
		}

	});

	@Override
	public boolean offer(QVehicle e) {
		return delegate.offer(e);
	}

	@Override
	public QVehicle peek() {
		return delegate.peek();
	}

	@Override
	public QVehicle poll() {
		return delegate.poll();
	}

	@Override
	public void addFirst(QVehicle qveh) {
		qveh.setEarliestLinkExitTime(Double.NEGATIVE_INFINITY);
		this.add(qveh) ; // uses the AbstractQueue.add, which in turn uses the PassingVehicleQ.offer.
	}

	@Override
	public Iterator<QVehicle> iterator() {
		return delegate.iterator();
	}

	@Override
	public int size() {
		return delegate.size();
	}

}