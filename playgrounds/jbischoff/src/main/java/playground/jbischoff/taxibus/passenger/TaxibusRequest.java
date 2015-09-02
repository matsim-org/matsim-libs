/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.jbischoff.taxibus.passenger;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.RequestImpl;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

import playground.jbischoff.taxibus.scheduler.TaxibusDriveWithPassengerTask;
import playground.jbischoff.taxibus.scheduler.TaxibusTask;
import playground.jbischoff.taxibus.scheduler.TaxibusTask.TaxibusTaskType;
import playground.jbischoff.taxibus.scheduler.TaxibusTaskWithRequests;

/**
 * @author  jbischoff
 * (might not be needed)
 */
public class TaxibusRequest extends RequestImpl   implements PassengerRequest, Comparable<TaxibusRequest>
 {
	
    public enum TaxibusRequestStatus
    {
        //INACTIVE, // invisible to the dispatcher (ARTIFICIAL STATE!)
        UNPLANNED, // submitted by the CUSTOMER and received by the DISPATCHER
        PLANNED, // planned - included into one of the routes

        //we have started serving the request but we may still divert the cab
        DISPATCHED,

        //we have to carry out the request 
        // - difference between taxi and taxibus seems to minimal, but the actual tasks are different, because Pickuptasks for certain requests may be with customers on board already
        PICKUP,
        RIDE, 
        DROPOFF,

        PERFORMED, //
        //REJECTED, // rejected by the DISPATCHER
        //CANCELLED, // canceled by the CUSTOMER
        ;
    };
	
    private final MobsimPassengerAgent passenger;
    private final Link fromLink;
    private final Link toLink;
	private TaxibusTaskWithRequests pickupTask;
	private TaxibusTaskWithRequests dropoffTask;

	
	private ArrayList<TaxibusDriveWithPassengerTask> driveWithPassengerTasks = new ArrayList<>();

    public TaxibusRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink, Link toLink,
            double t0, double submissionTime)
    {
        super(id, 1, t0, t0, submissionTime);
        this.passenger = passenger;
        this.fromLink = fromLink;
        this.toLink = toLink;
    }

	@Override
	public Link getFromLink() {
		return fromLink;
	}

	@Override
	public Link getToLink() {
		return toLink;
	}

	@Override
	public MobsimPassengerAgent getPassenger() {
		return passenger;
	}

	public void setPickupTask(TaxibusTaskWithRequests pickupTask) {
        this.pickupTask = pickupTask;
		
	}

	public void addDriveWithPassengerTask(TaxibusDriveWithPassengerTask task) {
		this.driveWithPassengerTasks.add(task);
	}

	public TaxibusTaskWithRequests getDropoffTask() {
		return dropoffTask;
	}

	public void setDropoffTask(TaxibusTaskWithRequests dropoffTask) {
		this.dropoffTask = dropoffTask;
	}

	public TaxibusTaskWithRequests getPickupTask() {
		return pickupTask;
	}

	public ArrayList<TaxibusDriveWithPassengerTask> getDriveWithPassengerTask() {
		return driveWithPassengerTasks;
	}

	public TaxibusRequestStatus getStatus() {
		    {
		        if (pickupTask == null) {
		            return TaxibusRequestStatus.UNPLANNED;
		        }

		        switch (pickupTask.getStatus()) {
		            case PLANNED:
		                TaxibusTask currentTask = (TaxibusTask)pickupTask.getSchedule().getCurrentTask();
		                if (currentTask.getTaxibusTaskType() == TaxibusTaskType.DRIVE_EMPTY && //
		                        pickupTask.getTaskIdx() == currentTask.getTaskIdx() + 1) {
		                    return TaxibusRequestStatus.DISPATCHED;
		                }

		                return TaxibusRequestStatus.PLANNED;

		            case STARTED:
		                return TaxibusRequestStatus.PICKUP;
		                
		            case PERFORMED://continue
		        }

		        if (!driveWithPassengerTasks.isEmpty())
		        {
		        	for (TaxibusDriveWithPassengerTask t : driveWithPassengerTasks){
		        		
		        		if (t.getStatus().equals(TaskStatus.STARTED)){
		        			
		        			return TaxibusRequestStatus.RIDE;
		        		}
		        	}
		        }
		        

		        switch (dropoffTask.getStatus()) {
		            case STARTED:
		                return TaxibusRequestStatus.DROPOFF;

		            case PERFORMED:
		                return TaxibusRequestStatus.PERFORMED;
		                
		            case PLANNED://illegal
		        }

		        throw new IllegalStateException("Unreachable code");
		    }
		
	}

	@Override
	public int compareTo(TaxibusRequest o) {

		return Double.valueOf(this.getT0()).compareTo(o.getT0());
	}
	
	
	
	
	

}
