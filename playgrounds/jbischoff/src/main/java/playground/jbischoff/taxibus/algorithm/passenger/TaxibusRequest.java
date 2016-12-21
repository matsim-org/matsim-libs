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

package playground.jbischoff.taxibus.algorithm.passenger;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.RequestImpl;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

import playground.jbischoff.drt.scheduler.tasks.DrtDriveWithPassengerTask;
import playground.jbischoff.drt.scheduler.tasks.DrtTask;
import playground.jbischoff.drt.scheduler.tasks.DrtTaskWithRequests;
import playground.jbischoff.drt.scheduler.tasks.DrtTask.DrtTaskType;

/**
 * @author  jbischoff
 * (might not be needed)
 */
public class TaxibusRequest extends RequestImpl   implements PassengerRequest
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
	private DrtTaskWithRequests pickupTask = null;
	private DrtTaskWithRequests dropoffTask = null;;

	
	private ArrayList<DrtDriveWithPassengerTask> driveWithPassengerTasks = new ArrayList<>();

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

	public void setPickupTask(DrtTaskWithRequests pickupTask) {
        this.pickupTask = pickupTask;
		
	}

	public void addDriveWithPassengerTask(DrtDriveWithPassengerTask task) {
		this.driveWithPassengerTasks.add(task);
	}

	public DrtTaskWithRequests getDropoffTask() {
		return dropoffTask;
	}

	public void setDropoffTask(DrtTaskWithRequests dropoffTask) {
		this.dropoffTask = dropoffTask;
	}

	public DrtTaskWithRequests getPickupTask() {
		return pickupTask;
	}

	public ArrayList<DrtDriveWithPassengerTask> getDriveWithPassengerTask() {
		return driveWithPassengerTasks;
	}

	public TaxibusRequestStatus getStatus() {
		    {
		        if (pickupTask == null) {
		            return TaxibusRequestStatus.UNPLANNED;
		        }

		        switch (pickupTask.getStatus()) {
		            case PLANNED:
		                DrtTask currentTask = (DrtTask)pickupTask.getSchedule().getCurrentTask();
		                if (currentTask.getDrtTaskType() == DrtTaskType.DRIVE_EMPTY && //
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
		        	for (DrtDriveWithPassengerTask t : driveWithPassengerTasks){
		        		
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
		                
		            case PLANNED://not illegal here
		            	return TaxibusRequestStatus.PLANNED;
		        }

		        throw new IllegalStateException("Unreachable code");
		    }
		
	}

	
	
	
	
	

}
