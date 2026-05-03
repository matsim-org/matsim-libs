package org.matsim.contrib.ev.strategic.reservation;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.MobsimMessageCollector;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.ev.reservation.DistributedChargerReservationManager;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlanActivity;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;
import org.matsim.contrib.ev.withinday.WithinDayEvConfigGroup;
import org.matsim.contrib.ev.withinday.WithinDayEvEngine;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.dsim.DistributedActivityHandler;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.dsim.DistributedMobsimEngine;
import org.matsim.core.mobsim.dsim.NotifyAgentPartitionTransfer;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;

public class StrategicChargingReservationEngine
	implements DistributedActivityHandler, DistributedMobsimEngine, NotifyAgentPartitionTransfer {

	static public final String PERSON_ATTRIBUTE = "sevc:reservationSlack";

	/**
	 * Transfer message carrying a single advance reservation from one partition to another.
	 * Only primitives and IDs so it can be serialized by the framework.
	 */
	public record AdvanceReservation(
		double reservationTime,
		Id<Person> personId,
		Id<org.matsim.contrib.ev.infrastructure.Charger> chargerId,
		Id<Vehicle> vehicleId,
		double startTime,
		double endTime
	) implements Message {
	}

	private final DistributedChargerReservationManager manager;
	private final TimeInterpretation timeInterpretation;
	private final EventsManager eventsManager;
	private final String chargingMode;
	private final MobsimMessageCollector partitionTransfer;

	private InternalInterface internalInterface;

	/** Priority queue drained in {@link #doSimStep} — ordered by reservation time. */
	private final PriorityQueue<AdvanceReservation> queue = new PriorityQueue<>(
		Comparator.comparing(AdvanceReservation::reservationTime));

	/** Secondary index for O(1) per-agent lookup when an agent leaves the partition. */
	private final Map<Id<Person>, List<AdvanceReservation>> personReservations = new HashMap<>();

	@Inject
	public StrategicChargingReservationEngine(
		DistributedChargerReservationManager manager,
		TimeInterpretation timeInterpretation,
		WithinDayEvConfigGroup configGroup,
		EventsManager eventsManager,
		MobsimMessageCollector partitionTransfer) {
		this.manager = manager;
		this.timeInterpretation = timeInterpretation;
		this.chargingMode = configGroup.getCarMode();
		this.eventsManager = eventsManager;
		this.partitionTransfer = partitionTransfer;
	}

	// -------------------------------------------------------------------------
	// DistributedActivityHandler
	// -------------------------------------------------------------------------

	@Override
	public boolean handleActivity(MobsimAgent agent) {
		if (agent instanceof PlanAgent pa) {
			if (pa.getPreviousPlanElement() == null) {
				// First activity of the day on this partition — schedule advance reservations.
				// Agents may have started the day on another partition, so we cannot do this
				// in beforeMobsim().
				scheduleAdvanceReservations(pa);
			}
		}
		return false;
	}

	@Override
	public void rescheduleActivityEnd(MobsimAgent agent) {
		// no-op
	}

	@Override
	public double priority() {
		return 110.0;
	}

	// -------------------------------------------------------------------------
	// MobsimEngine (via DistributedMobsimEngine)
	// -------------------------------------------------------------------------

	@Override
	public void doSimStep(double time) {
		while (!queue.isEmpty() && queue.peek().reservationTime <= time) {
			AdvanceReservation advance = queue.poll();
			removeFromPersonIndex(advance);

			manager.addReservation(advance.chargerId(), advance.vehicleId(), advance.startTime(), advance.endTime(),
				reservation -> {
					var now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
					boolean successful = reservation.isPresent();
					eventsManager.processEvent(new AdvanceReservationEvent(now, advance.personId(),
						advance.vehicleId(), advance.chargerId(), advance.startTime(), advance.endTime(),
						successful));
				});
		}
	}

	@Override
	public void afterMobsim() {
		queue.clear();
		personReservations.clear();
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	// -------------------------------------------------------------------------
	// DistributedMobsimEngine — message handlers
	// -------------------------------------------------------------------------

	@Override
	public Map<Class<? extends Message>, org.matsim.core.mobsim.dsim.DSimComponentsMessageProcessor.MessageHandler> getMessageHandlers() {
		return Map.of(AdvanceReservation.class, this::handleAdvanceReservationMessages);
	}

	private void handleAdvanceReservationMessages(List<Message> messages, double now) {
		for (Message message : messages) {
			AdvanceReservation advance = (AdvanceReservation) message;
			enqueue(advance);
		}
	}

	// -------------------------------------------------------------------------
	// NotifyAgentPartitionTransfer
	// -------------------------------------------------------------------------

	@Override
	public void onAgentLeavesPartition(DistributedMobsimAgent agent, int toPartition) {
		List<AdvanceReservation> pending = personReservations.remove(agent.getId());
		if (pending == null) return;

		for (AdvanceReservation advance : pending) {
			queue.remove(advance);
			partitionTransfer.collect(advance, toPartition);
		}
	}

	@Override
	public void onAgentEntersPartition(DistributedMobsimAgent agent) {
		// Incoming AdvanceReservation messages are handled via handleAdvanceReservationMessages().
		// Nothing to do here — the messages arrive asynchronously and are enqueued there.
	}

	// -------------------------------------------------------------------------
	// Internal helpers
	// -------------------------------------------------------------------------

	private void scheduleAdvanceReservations(PlanAgent pa) {
		Person person = pa.getCurrentPlan().getPerson();

		if (!WithinDayEvEngine.isActive(person)) {
			return;
		}

		Double reservationSlack = getReservationSlack(person);
		if (reservationSlack == null) {
			return;
		}

		ChargingPlan plan = ChargingPlans.get(person.getSelectedPlan()).getSelectedPlan();
		if (plan == null) {
			return;
		}

		Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(person, chargingMode);

		List<Double> startTimes = new LinkedList<>();
		List<Double> endTimes = new LinkedList<>();

		TimeTracker timeTracker = new TimeTracker(timeInterpretation);

		for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
			double startTime = timeTracker.getTime().seconds();
			timeTracker.addElement(element);
			double endTime = timeTracker.getTime().orElse(Double.POSITIVE_INFINITY);

			if (element instanceof Activity activity) {
				if (TripStructureUtils.isStageActivityType(activity.getType())) {
					startTimes.add(startTime);
					endTimes.add(endTime);
				}
			}
		}

		for (ChargingPlanActivity activity : plan.getChargingActivities()) {
			if (!activity.isReserved()) {
				continue;
			}

			if (activity.isEnroute()) {
				double startTime = endTimes.get(activity.getFollowingActivityIndex() - 1);
				double endTime = startTime + activity.getDuration();
				double reservationTime = startTime - reservationSlack;

				enqueue(new AdvanceReservation(reservationTime, person.getId(),
					activity.getChargerId(), vehicleId, startTime, endTime));
			} else {
				double startTime = startTimes.get(activity.getStartActivityIndex());
				double endTime = endTimes.get(activity.getEndActivityIndex());
				double reservationTime = startTime - reservationSlack;

				enqueue(new AdvanceReservation(reservationTime, person.getId(),
					activity.getChargerId(), vehicleId, startTime, endTime));
			}
		}
	}

	private void enqueue(AdvanceReservation advance) {
		queue.add(advance);
		personReservations.computeIfAbsent(advance.personId(), _ -> new ArrayList<>()).add(advance);
	}

	private void removeFromPersonIndex(AdvanceReservation advance) {
		List<AdvanceReservation> list = personReservations.get(advance.personId());
		if (list != null) {
			list.remove(advance);
			if (list.isEmpty()) {
				personReservations.remove(advance.personId());
			}
		}
	}

	// -------------------------------------------------------------------------
	// Static helpers (mirrors StrategicChargingReservationEngine)
	// -------------------------------------------------------------------------

	static public void setReservationSlack(Person person, double slack) {
		person.getAttributes().putAttribute(PERSON_ATTRIBUTE, slack);
	}

	static public Double getReservationSlack(Person person) {
		return (Double) person.getAttributes().getAttribute(PERSON_ATTRIBUTE);
	}
}
