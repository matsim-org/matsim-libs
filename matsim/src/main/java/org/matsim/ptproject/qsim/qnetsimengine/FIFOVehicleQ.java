package org.matsim.ptproject.qsim.qnetsimengine;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.LinkedList;

public class FIFOVehicleQ extends AbstractQueue<QVehicle> implements VehicleQ<QVehicle>  {
	
	LinkedList<QVehicle> vehicleQueue = new LinkedList<QVehicle>();

	@Override
	public boolean offer(QVehicle e) {
		return vehicleQueue.offer(e);
	}

	@Override
	public QVehicle peek() {
		return vehicleQueue.peek();
	}

	@Override
	public QVehicle poll() {
		return vehicleQueue.poll();
	}

	@Override
	public Iterator<QVehicle> iterator() {
		return vehicleQueue.iterator();
	}

	@Override
	public int size() {
		return vehicleQueue.size();
	}

	@Override
	public void addFirst(QVehicle e) {
		vehicleQueue.addFirst(e);
	}

}
