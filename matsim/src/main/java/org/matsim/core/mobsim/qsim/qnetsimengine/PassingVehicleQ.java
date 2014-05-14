package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.*;

final class PassingVehicleQ extends AbstractQueue<QVehicle> implements VehicleQ<QVehicle> {

	public PassingVehicleQ() {} // to find calls 
	
	private final Queue<QVehicle> delegate = new PriorityQueue<QVehicle>(11, new Comparator<QVehicle>() {

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
	public void addFirst(QVehicle previous) {
		// This implementation of VehicleQ does not support adding a Vehicle to the front of the Queue.
        // At the moment, this will mean that people with one-link trips will behave differently
        // (they will have to queue in), and transit will probably behave unplausibly (it
        // will have to queue in each time for multiple stops on the same link). michaz 2014
        delegate.offer(previous);
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