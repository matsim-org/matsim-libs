package org.matsim.contrib.freight.vrp.basics;

public interface TourState {

	public TourStateSnapshot getTourStateSnapshot();

	public void setTourStateSnapshot(TourStateSnapshot snapshot);

}
