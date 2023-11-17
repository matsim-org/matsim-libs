package org.matsim.contrib.drt.prebooking.logic.helpers;

import java.util.PriorityQueue;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.drt.prebooking.PrebookingManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

import com.google.common.base.Preconditions;

/**
 * This service helps to define a PrebookingLogic where at some point in time
 * (usually at the beginning of the simulaton), it is known in advance that a
 * request will happen at a specific time. Once we know that the request will be
 * sent, we can use the present class to put it in a queue, making sure the
 * request will be *booked* the the time we want.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class PrebookingQueue implements MobsimBeforeSimStepListener {
	private final PrebookingManager prebookingManager;

	private PriorityQueue<ScheduledSubmission> queue = new PriorityQueue<>();
	private Integer sequence = 0;

	private double currentTime = Double.NEGATIVE_INFINITY;

	public PrebookingQueue(PrebookingManager prebookingManager) {
		this.prebookingManager = prebookingManager;
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent event) {
		performSubmissions(event.getSimulationTime());
	}

	private void performSubmissions(double time) {
		currentTime = time;

		while (!queue.isEmpty() && queue.peek().submissionTime <= time) {
			var item = queue.poll();
			prebookingManager.prebook(item.agent(), item.leg(), item.departuretime());
		}
	}

	/**
	 * May be called by a submission logic to directly perform submission after the
	 * MobsimInitializedEvent, otherwise this is called automatically per time step
	 * to see if there are requests queued that need to be submitted.
	 */
	public void performInitialSubmissions() {
		performSubmissions(Double.NEGATIVE_INFINITY);
	}

	public void schedule(double submissionTime, MobsimAgent agent, Leg leg, double departureTime) {
		Preconditions.checkArgument(submissionTime > currentTime, "Can only submit future requests");

		synchronized (queue) { // synchronizing for queue and sequence
			queue.add(new ScheduledSubmission(submissionTime, agent, leg, departureTime, sequence++));
		}
	}

	private record ScheduledSubmission(double submissionTime, MobsimAgent agent, Leg leg, double departuretime,
			int sequence) implements Comparable<ScheduledSubmission> {
		@Override
		public int compareTo(ScheduledSubmission o) {
			// sort by submissionTime, but otherwise keep the order of submission
			int comparison = Double.compare(submissionTime, o.submissionTime);

			if (comparison != 0) {
				return comparison;
			}

			// comparing by sequence, because a PriorityQueue is *not* preserving the order
			// of two elements with the same comparison value
			return Integer.compare(sequence, o.sequence);
		}
	}
}
