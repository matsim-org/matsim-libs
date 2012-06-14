package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;

import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;


/**
 * Records tour-activities and makes sure that tour-activities are inserted the way the implementor of this class wants them to be inserted.
 * 
 * By implementing this, the recorder might be able to ensure following things:
 * - activity sequences: e.g. a delivery must follow two pickups or only pickup-delivery-pickup-delivery is allowed
 * - load restrictions: can make sure that the capacity is not exceeded by a pickup activity
 * - can pre-check whether an insertion of tour-activity is feasible according to time-window-constraints
 * 
 * @author stefan
 *
 */
public interface TourActivityRecorder {

	public abstract void initialiseRecorder(Vehicle v, Tour t, Pickup pickup2insert, Delivery delivery2insert);

	public abstract void reset();

	public abstract void insertionProcedureStarts();

	public abstract void pickupInsertionAt(int pickupIndex);

	public abstract void deliveryInsertionAt(int deliveryIndex);

	public abstract boolean continueWithNextIndex();

	public abstract boolean finishInsertionProcedure();

}