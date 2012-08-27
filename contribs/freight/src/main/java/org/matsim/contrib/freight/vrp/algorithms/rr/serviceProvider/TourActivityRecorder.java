package org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider;

import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
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
abstract class TourActivityRecorder {

	abstract void initialiseRecorder(Vehicle v, TourImpl t, Pickup pickup2insert, Delivery delivery2insert);

	abstract void reset();

	abstract void insertionProcedureStarts();

	abstract void pickupInsertionAt(int pickupIndex);

	abstract void deliveryInsertionAt(int deliveryIndex);

	abstract boolean continueWithNextIndex();

	abstract boolean finishInsertionProcedure();

}