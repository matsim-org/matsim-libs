package org.matsim.contrib.drt.stops;

import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * This interface is used to manage the logic of stop times in DRT. It is able
 * to calculate:
 * 
 * <ul>
 * <li>The end time of a to-be-created stop that will be initially populated by
 * a pickup</li>
 * <li>The updated end time of a stop when a new pickup is added</li>
 * <li>The end time of a to-be-created stop that will be initially populated by
 * a dropoff</li>
 * <li>The updated end time of a stop when a new dropoff is added</li>
 * <li>The shifted end time of a stop when its begin time is shifted</li>
 * </ul>
 * 
 * The interface is mainly used by the insertion algorithm (InsertionGenerator,
 * InsertionDetourTimeCalculator) in order to calculate how inserting new
 * requests changes the stop sequence structure, and it is used by the
 * RequestInsertionScheduler to obtain the correct timing of the created stop
 * tasks. Finally, it is used by the StayTaskEndTimeCalculator to update the
 * schedule timing when delays occur.
 * 
 * Some thoughts need to be put into new implementations, especially in the
 * *update* methods. Both for pickups and dropoffs, they have an insertionTime
 * parameter that indicates when the pickup or dropoff can actually be inserted
 * at earliest into the task. Usually, this is the beginTime of the task, but
 * there are important exceptions.
 * 
 * For pickups, this is mainly when a new request is added to an already ongoing
 * stop task. In the traditional DRT implementation, a new pickup would just be
 * added without changing the end time of the task. This then lead to some
 * requests having wait times below the predefined stop duration, because the
 * task would just end as planned. However, a more correct implementation is to
 * consider the insertionTime, which indicates the "current time" in that case,
 * and add the stop duration at this point.
 * 
 * For dropoffs, this case cannot occur because before a dropoff is inserted a
 * pickup needs to be inserted. So a dropoff can never be merged into an ongoing
 * stop task. The difficulty for dropoffs is that during insertion first a
 * pickup is inserted. This usually causes a shift of all following tasks to the
 * future and later on DRT evaluates if this leads to any constraint (pickup
 * time, dropoff time) violations. But this means that the current task that we
 * want to update has a certain beginTime, but it is not valid anymore at the
 * point of calculating the new end time. The insertionTime then indicates the
 * updated start time of the stop task, including the time loss produced by the
 * pickup. When calculating the new end time there are, hence, two contribution
 * from adding a new request, but also from shifting the task, which both need
 * to be taken into account.
 */
public interface StopTimeCalculator {
	// a new stop with a pickup is created
	double initEndTimeForPickup(DvrpVehicle vehicle, double beginTime, DrtRequest request);

	// a pickup is added to an existing stop
	double updateEndTimeForPickup(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime, DrtRequest request);

	// a new stop with a dropoff is created
	double initEndTimeForDropoff(DvrpVehicle vehicle, double beginTime, DrtRequest request);

	// a dropoff is added to an existing stop
	double updateEndTimeForDropoff(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime, DrtRequest request);

	// the begin time of an existing stop is shifted
	double shiftEndTime(DvrpVehicle vehicle, DrtStopTask stop, double beginTime);
}
