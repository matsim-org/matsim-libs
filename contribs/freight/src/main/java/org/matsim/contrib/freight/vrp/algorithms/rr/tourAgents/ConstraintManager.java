/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;

import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;


public class ConstraintManager implements TourActivityRecorder {
	
	private Vehicle vehicle;
	
	private Tour tour;
	
	private Pickup pickup;
	
	private Delivery delivery;
	
	private boolean continueWithNextIndex = false;
	
	private boolean breakInsertionProcedure = false;

	boolean fine = true;

	private int outerLoadRecorder = 0;
	
	private int currentLoad = 0;
	
	private int currentPickupIndex = 0;
	
	public ConstraintManager(Vehicle vehicle, Tour tour, Pickup pickup, Delivery delivery) {
		this.vehicle = vehicle;
		this.tour = tour;
		this.pickup = pickup;
		this.delivery = delivery;
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourActivityRecorder#initialiseRecorder(org.matsim.contrib.freight.vrp.basics.Vehicle, org.matsim.contrib.freight.vrp.basics.Tour, org.matsim.contrib.freight.vrp.basics.Pickup, org.matsim.contrib.freight.vrp.basics.Delivery)
	 */
	@Override
	public void initialiseRecorder(Vehicle v, Tour t, Pickup pickup2insert, Delivery delivery2insert){
		reset();
		this.vehicle = v;
		this.tour = t;
		this.pickup = pickup2insert;
		this.delivery = delivery2insert;
	}

		
	/* (non-Javadoc)
	 * @see org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourActivityRecorder#reset()
	 */
	@Override
	public void reset(){
		fine = true;
	}

	private boolean feasable(int pickupIndex) {
		TourActivity prevAct = getActivity(pickupIndex-1);
		TourActivity nextAct = getActivity(pickupIndex);
		if(pickup.getLatestOperationStartTime() < prevAct.getEarliestOperationStartTime()){
			return false;
		}
		else if(pickup.getEarliestOperationStartTime() > nextAct.getLatestOperationStartTime()){
			return false;
		}
		return true;		
	}


	/* (non-Javadoc)
	 * @see org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourActivityRecorder#informInsertionProcedureStarts()
	 */
	@Override
	public void insertionProcedureStarts() {
		if(tour.costs.totalLoad + pickup.getCapacityDemand() > vehicle.getCapacity()){
			breakInsertionProcedure = true;
		}
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourActivityRecorder#pickupInsertionAt(int)
	 */
	@Override
	public void pickupInsertionAt(int pickupIndex) {
		resetContinueAndBreak();
		recordOuterLoad(pickupIndex);
		currentPickupIndex = pickupIndex;
		if(outerLoadRecorder + pickup.getCapacityDemand() > vehicle.getCapacity()){
			continueWithNextIndex = true;
		}
		if(!feasable(pickupIndex)){
			continueWithNextIndex = true;
		}
		if(!sequenceFeasable(pickupIndex)){
			breakInsertionProcedure = true;
		}
		currentLoad = outerLoadRecorder + pickup.getCapacityDemand();
	}

	private boolean sequenceFeasable(int pickupIndex) {
		if(getActivity(pickupIndex-1) instanceof Delivery){
			return false;
		}
		return true;
	}


	private void resetContinueAndBreak() {
		continueWithNextIndex = false;
		breakInsertionProcedure = false;
	}


	private void recordOuterLoad(int pickupIndex) {
		if(getActivity(pickupIndex-1) instanceof JobActivity){
			outerLoadRecorder += ((JobActivity)getActivity(pickupIndex-1)).getCapacityDemand();
		}		
	}

	private TourActivity getActivity(int i) {
		return tour.getActivities().get(i);
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourActivityRecorder#deliveryInsertionAt(int)
	 */
	@Override
	public void deliveryInsertionAt(int deliveryIndex) {
		resetContinueAndBreak();
		if(deliveryIndex != currentPickupIndex){
			if(getActivity(deliveryIndex-1) instanceof JobActivity){
				currentLoad += ((JobActivity)getActivity(deliveryIndex-1)).getCapacityDemand();
			}
		}
		if(currentLoad > vehicle.getCapacity()){
			breakInsertionProcedure = true;
		}
		if(!delSequenceFeasable(deliveryIndex)){
			continueWithNextIndex = true;
		}
		
	}

	private boolean delSequenceFeasable(int deliveryIndex) {
		if(getActivity(deliveryIndex) instanceof Pickup){
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourActivityRecorder#canWeContinueWithNextIndex()
	 */
	@Override
	public boolean continueWithNextIndex() {
		return continueWithNextIndex;
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourActivityRecorder#canWeBreakInsertionProcedure()
	 */
	@Override
	public boolean finishInsertionProcedure() {
		return breakInsertionProcedure;
	}

}
