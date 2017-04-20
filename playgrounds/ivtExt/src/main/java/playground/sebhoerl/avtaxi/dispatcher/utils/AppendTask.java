package playground.sebhoerl.avtaxi.dispatcher.utils;

import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.LeastCostPathFuture;

/**
 * the class stored the specifics of a AV task used in
 * {@link SingleRideAppender}
 */
class AppendTask {
    final public AVRequest request;
    final public AVVehicle vehicle;
    final public double time;
    final public LeastCostPathFuture pickup;
    final public LeastCostPathFuture dropoff;

    public AppendTask(AVRequest request, AVVehicle vehicle, double time, //
	    LeastCostPathFuture pickup, LeastCostPathFuture dropoff) {
	this.request = request;
	this.vehicle = vehicle;
	this.pickup = pickup;
	this.dropoff = dropoff;
	this.time = time;
    }
}