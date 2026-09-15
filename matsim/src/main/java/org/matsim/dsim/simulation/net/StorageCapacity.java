package org.matsim.dsim.simulation.net;

interface StorageCapacity {

	void consume(double pce);

	void release(double pce, double now);

	void update(double now);

	boolean isAvailable();

	double getMax();

	double getOccupied();
}
