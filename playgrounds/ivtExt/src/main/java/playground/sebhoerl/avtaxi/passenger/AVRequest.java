package playground.sebhoerl.avtaxi.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.RequestImpl;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.routing.AVRoute;
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;

public class AVRequest /*extends RequestImpl*/ implements PassengerRequest {
    final private Link pickupLink;
    final private Link dropoffLink;
    final private MobsimPassengerAgent passengerAgent;
    final private AVOperator operator;
    final private AVDispatcher dispatcher;
    final private AVRoute route;

    private AVPickupTask pickupTask;
    private AVDropoffTask dropoffTask;
    
    private RequestImpl delegate ;

    public AVRequest(Id<Request> id, MobsimPassengerAgent passengerAgent, Link pickupLink, Link dropoffLink, double pickupTime, double submissionTime, AVRoute route, AVOperator operator, AVDispatcher dispatcher) {
//        super(id, 1.0, pickupTime, pickupTime, submissionTime);
	    delegate = new RequestImpl( id, 1.0, pickupTime, pickupTime, submissionTime ) ;

        this.passengerAgent = passengerAgent;
        this.pickupLink = pickupLink;
        this.dropoffLink = dropoffLink;
        this.operator = operator;
        this.route = route;
        this.dispatcher = dispatcher;
    }

    @Override
    public Link getFromLink() {
        return pickupLink;
    }

    @Override
    public Link getToLink() {
        return dropoffLink;
    }

    @Override
    public MobsimPassengerAgent getPassenger() {
        return passengerAgent;
    }

    public AVPickupTask getPickupTask() {
        return pickupTask;
    }

    public void setPickupTask(AVPickupTask pickupTask) {
        this.pickupTask = pickupTask;
    }

    public AVDropoffTask getDropoffTask() {
        return dropoffTask;
    }

    public void setDropoffTask(AVDropoffTask dropoffTask) {
        this.dropoffTask = dropoffTask;
    }

    public AVOperator getOperator() {
        return operator;
    }

    public AVDispatcher getDispatcher() {
        return dispatcher;
    }

    public AVRoute getRoute() {
        return route;
    }

/**
 * @return
 * @see java.lang.Object#hashCode()
 */
public int hashCode() {
	return delegate.hashCode();
}

/**
 * @return
 * @see org.matsim.contrib.dvrp.data.RequestImpl#getId()
 */
public Id<Request> getId() {
	return delegate.getId();
}

/**
 * @return
 * @see org.matsim.contrib.dvrp.data.RequestImpl#getQuantity()
 */
public double getQuantity() {
	return delegate.getQuantity();
}

/**
 * @return
 * @see org.matsim.contrib.dvrp.data.RequestImpl#getEarliestStartTime()
 */
public double getEarliestStartTime() {
	return delegate.getEarliestStartTime();
}

/**
 * @return
 * @see org.matsim.contrib.dvrp.data.RequestImpl#getLatestStartTime()
 */
public double getLatestStartTime() {
	return delegate.getLatestStartTime();
}

/**
 * @return
 * @see org.matsim.contrib.dvrp.data.RequestImpl#getSubmissionTime()
 */
public double getSubmissionTime() {
	return delegate.getSubmissionTime();
}

/**
 * @return
 * @see org.matsim.contrib.dvrp.data.RequestImpl#isRejected()
 */
public boolean isRejected() {
	return delegate.isRejected();
}

/**
 * @param rejected
 * @see org.matsim.contrib.dvrp.data.RequestImpl#setRejected(boolean)
 */
public void setRejected(boolean rejected) {
	delegate.setRejected(rejected);
}

/**
 * @return
 * @see org.matsim.contrib.dvrp.data.RequestImpl#toString()
 */
public String toString() {
	return delegate.toString();
}

/**
 * @param obj
 * @return
 * @see java.lang.Object#equals(java.lang.Object)
 */
public boolean equals(Object obj) {
	return delegate.equals(obj);
}
}
