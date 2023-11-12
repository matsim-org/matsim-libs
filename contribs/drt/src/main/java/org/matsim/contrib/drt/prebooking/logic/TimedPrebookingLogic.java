package org.matsim.contrib.drt.prebooking.logic;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.drt.prebooking.PrebookingManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;

/**
 * This class helps to define a PrebookingLogic where it is known in advance
 * when requests will be submitted. In essence, we list all prebooked requests
 * at the beginning of the simulation and define a specific time at which they
 * should be submitted. See AttributePrebookingLogic or
 * FixedSharePrebookingLogic as examples.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public abstract class TimedPrebookingLogic implements MobsimInitializedListener, MobsimBeforeSimStepListener {
	protected final PrebookingManager prebookingManager;
	protected final PrebookingQueue queue = new PrebookingQueue();

	protected TimedPrebookingLogic(PrebookingManager prebookingManager) {
		this.prebookingManager = prebookingManager;
	}

	@Override
	public void notifyMobsimInitialized(@SuppressWarnings("rawtypes") MobsimInitializedEvent e) {
		queue.clear();
		scheduleRequests();
	}

	protected abstract void scheduleRequests();

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent event) {
		queue.getScheduledItems(event.getSimulationTime()).forEach(item -> {
			prebookingManager.prebook(item.agent(), item.leg(), item.departuretime());
		});
	}

	protected class PrebookingQueue {
		private PriorityQueue<ScheduledItem> queue = new PriorityQueue<>();
		private int sequence = 0;

		public void schedule(double submissionTime, MobsimAgent agent, Leg leg, double departureTime) {
			queue.add(new ScheduledItem(submissionTime, agent, leg, departureTime, sequence++));
		}

		public Collection<ScheduledItem> getScheduledItems(double time) {
			List<ScheduledItem> batch = new LinkedList<>();

			while (!queue.isEmpty() && queue.peek().submissionTime <= time) {
				batch.add(queue.poll());
			}

			return batch;
		}

		public void clear() {
			queue.clear();
			sequence = 0;
		}

	}

	private record ScheduledItem(double submissionTime, MobsimAgent agent, Leg leg, double departuretime, int sequence)
			implements Comparable<ScheduledItem> {
		@Override
		public int compareTo(ScheduledItem o) {
			// sort by submissionTime, but otherwise keep the order of submission
			int comparison = Double.compare(submissionTime, o.submissionTime);

			if (comparison != 0) {
				return comparison;
			}

			return Integer.compare(sequence, o.sequence);
		}
	}
}
