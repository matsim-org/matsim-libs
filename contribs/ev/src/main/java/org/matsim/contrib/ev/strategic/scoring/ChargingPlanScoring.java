package org.matsim.contrib.ev.strategic.scoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.contrib.ev.charging.EnergyChargedEvent;
import org.matsim.contrib.ev.charging.EnergyChargedEventHandler;
import org.matsim.contrib.ev.charging.QueuedAtChargerEvent;
import org.matsim.contrib.ev.charging.QueuedAtChargerEventHandler;
import org.matsim.contrib.ev.charging.QuitQueueAtChargerEvent;
import org.matsim.contrib.ev.charging.QuitQueueAtChargerEventHandler;
import org.matsim.contrib.ev.discharging.DrivingEnergyConsumptionEvent;
import org.matsim.contrib.ev.discharging.DrivingEnergyConsumptionEventHandler;
import org.matsim.contrib.ev.discharging.IdlingEnergyConsumptionEvent;
import org.matsim.contrib.ev.discharging.IdlingEnergyConsumptionEventHandler;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.strategic.costs.ChargingCostCalculator;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;
import org.matsim.contrib.ev.withinday.WithinDayEvEngine;
import org.matsim.contrib.ev.withinday.events.AbortChargingAttemptEvent;
import org.matsim.contrib.ev.withinday.events.AbortChargingAttemptEventHandler;
import org.matsim.contrib.ev.withinday.events.AbortChargingProcessEvent;
import org.matsim.contrib.ev.withinday.events.AbortChargingProcessEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import com.google.common.util.concurrent.AtomicDouble;

/**
 * This class manages the scoring of charging plans. See the documentation of
 * the package or the respective ChargingPlanScoringParameters for more
 * information on the individaul scoring dimensions.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ChargingPlanScoring implements IterationStartsListener, ScoringListener, AbortChargingProcessEventHandler,
		AbortChargingAttemptEventHandler, DrivingEnergyConsumptionEventHandler, IdlingEnergyConsumptionEventHandler,
		ChargingEndEventHandler, QueuedAtChargerEventHandler, QuitQueueAtChargerEventHandler, ChargingStartEventHandler,
		LinkEnterEventHandler, LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler, EnergyChargedEventHandler,
		MobsimEngine {
	static public final String MINIMUM_SOC_PERSON_ATTRIBUTE = "sevc:minimumSoc";
	static public final String MINIMUM_END_SOC_PERSON_ATTRIBUTE = "sevc:minimumEndSoc";

	static public final String MONEY_EVENT_PURPOSE = "strategic charging";

	private final EventsManager eventsManager;

	private final Population population;
	private final Network network;
	private final ElectricFleetSpecification fleet;

	private final ChargingPlanScoringParameters parameters;
	private final ChargingCostCalculator costCalculator;

	private final String chargingMode;

	private final ScoringTracker tracker;

	public ChargingPlanScoring(EventsManager eventsManager, Population population, Network network,
			ElectricFleetSpecification fleet, ChargingCostCalculator costCalculator,
			ChargingPlanScoringParameters parameters, String chargingMode, ScoringTracker tracker) {
		this.eventsManager = eventsManager;
		this.population = population;
		this.network = network;
		this.fleet = fleet;
		this.costCalculator = costCalculator;
		this.parameters = parameters;
		this.chargingMode = chargingMode;
		this.tracker = tracker;

		initializePersons();
	}

	// PERSONS for vehicles: vehicle-related scoring is added to the score of the
	// owning person

	private final List<Person> activePersons = new ArrayList<>();
	private final IdMap<Vehicle, Id<Person>> vehiclePersons = new IdMap<>(Vehicle.class);

	private void initializePersons() {
		for (Person person : population.getPersons().values()) {
			if (WithinDayEvEngine.isActive(person) && VehicleUtils.hasVehicleId(person, chargingMode)) {
				Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(person, chargingMode);
				Id<Person> previousId = vehiclePersons.put(vehicleId, person.getId());
				activePersons.add(person);

				if (previousId != null) {
					throw new IllegalStateException(
							"Vehicle " + vehicleId + " is attached to multiple persons (at least " + person.getId()
									+ " and " + previousId + ")");
				}
			}
		}
	}

	private Id<Person> getPerson(Id<Vehicle> vehicleId) {
		return vehiclePersons.get(vehicleId);
	}

	// SCORING handling

	private final IdMap<Person, AtomicDouble> scores = new IdMap<>(Person.class);
	private boolean finalized = false;

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		tracker.start(event.getIteration(), event.isLastIteration());

		// scoring per day starts here

		initializeEnergy();
		initializeCosts();
		initializeWaiting();
		initializeDetours();

		finalized = false;
		scores.clear();

		for (Person person : activePersons) {
			scores.put(person.getId(), new AtomicDouble(0.0));
		}

		eventsManager.addHandler(this);
	}

	private void addScoreForPerson(Id<Person> personId, double score) {
		if (score != 0.0) {
			AtomicDouble item = scores.get(personId);

			if (item != null) {
				item.addAndGet(score);
			}
		}
	}

	private void trackScoreForPerson(double time, Id<Person> personId, String dimension, double score, Double value) {
		if (score != 0.0) {
			tracker.trackScore(time, personId, dimension, score, value);
		}
	}

	private void addScoreForVehicle(Id<Vehicle> vehicleId, double score) {
		Id<Person> personId = getPerson(vehicleId);

		if (personId != null) {
			addScoreForPerson(personId, score);
		}
	}

	private void trackScoreForVehicle(double time, Id<Vehicle> vehicleId, String dimension, double score,
			Double value) {
		Id<Person> personId = getPerson(vehicleId);

		if (personId != null) {
			trackScoreForPerson(time, personId, dimension, score, value);
		}
	}

	void finalizeScoring() {
		// this may be called by standard scoring or directly by notifyScoring,
		// depending
		// on whether charging scoring is fed back to standard scoring

		if (finalized) {
			return;
		}

		finalized = true;

		// scoring per day ends here

		finalizeSoc(simStepTime);
		finalizeDetours(simStepTime);

		eventsManager.removeHandler(this);
		tracker.finish();

		for (Person person : activePersons) {
			AtomicDouble score = scores.get(person.getId());
			ChargingPlans chargingPlans = ChargingPlans.get(person.getSelectedPlan());

			if (chargingPlans.getChargingPlans().size() > 0) {
				chargingPlans.getSelectedPlan().setScore(score.get());
			}
		}
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		finalizeScoring();
	}

	double getScore(Id<Person> personId) {
		return scores.getOrDefault(personId, new AtomicDouble(0.0)).get();
	}

	// SOC TRACKING

	private class EnergyEntry {
		double total;
		double current;
	}

	private final IdMap<Vehicle, EnergyEntry> energy = new IdMap<>(Vehicle.class);
	private final IdMap<Person, Double> minimumSoc = new IdMap<>(Person.class);

	private void initializeEnergy() {
		energy.clear();
		minimumSoc.clear();

		for (ElectricVehicleSpecification vehicle : fleet.getVehicleSpecifications().values()) {
			EnergyEntry entry = new EnergyEntry();
			entry.total = vehicle.getBatteryCapacity();
			entry.current = vehicle.getInitialCharge();
			energy.put(vehicle.getId(), entry);
		}

		for (Person person : activePersons) {
			Double minimumSoc = getMinimumSoc(person);

			if (minimumSoc != null) {
				this.minimumSoc.put(person.getId(), minimumSoc);
			}
		}
	}

	private void handleEnergy(double now, Id<Vehicle> vehicleId, double endCharge) {
		EnergyEntry entry = this.energy.get(vehicleId);
		double initialSoc = entry.current / entry.total;

		if (entry != null) {
			entry.current = endCharge;
		}

		double finalSoc = entry.current / entry.total;
		handleChangeSoc(now, vehicleId, initialSoc, finalSoc);
	}

	@Override
	public void handleEvent(IdlingEnergyConsumptionEvent event) {
		handleEnergy(event.getTime(), event.getVehicleId(), event.getEndCharge());
	}

	@Override
	public void handleEvent(DrivingEnergyConsumptionEvent event) {
		handleEnergy(event.getTime(), event.getVehicleId(), event.getEndCharge());
	}

	@Override
	public void handleEvent(EnergyChargedEvent event) {
		handleEnergy(event.getTime(), event.getVehicleId(), event.getEndCharge());
	}

	@Override
	public void handleEvent(ChargingEndEvent event) {
		handleEnergy(event.getTime(), event.getVehicleId(), event.getCharge());
		handleFinishCharging(event);
	}

	// SOC: handle soc-related scoring

	private void handleChangeSoc(double now, Id<Vehicle> vehicleId, double initialSoc, double finalSoc) {
		if (parameters.zeroSoc != 0.0) {
			if (initialSoc > 0.0 && finalSoc <= 0.0) {
				addScoreForVehicle(vehicleId, parameters.zeroSoc);
				trackScoreForVehicle(now, vehicleId, "zero_soc", parameters.zeroSoc, null);
			}
		}

		if (parameters.belowMinimumSoc != 0.0) {
			Id<Person> personId = getPerson(vehicleId);
			if (personId != null && minimumSoc.containsKey(personId)) {
				double personMinimumSoc = minimumSoc.get(personId);

				if (initialSoc >= personMinimumSoc && finalSoc < personMinimumSoc) {
					addScoreForPerson(personId, parameters.belowMinimumSoc);
					trackScoreForPerson(now, personId, "minimum_soc", parameters.belowMinimumSoc, finalSoc);
				}
			}
		}
	}

	private void finalizeSoc(double now) {
		if (parameters.belowMinimumEndSoc != 0.0) {
			for (Person person : activePersons) {
				Double minimumEndOfDaySoc = getMinimumEndSoc(person);

				if (minimumEndOfDaySoc != null) {
					Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(person, chargingMode);

					if (energy.containsKey(vehicleId)) {
						EnergyEntry entry = energy.get(vehicleId);

						if (entry.current / entry.total < minimumEndOfDaySoc) {
							addScoreForPerson(person.getId(), parameters.belowMinimumEndSoc);
							trackScoreForPerson(now, person.getId(), "minimum_end_soc", parameters.belowMinimumEndSoc,
									null);
						}
					}
				}
			}
		}
	}

	// CHARGING PROCESS: handle failed attempts

	@Override
	public void handleEvent(AbortChargingAttemptEvent event) {
		// handles an unsuccessful charging attempt, but the agent tries another one
		addScoreForPerson(event.getPersonId(), parameters.failedChargingAttempt);
		trackScoreForPerson(event.getTime(), event.getPersonId(), "failed_attempt", parameters.failedChargingAttempt,
				null);
	}

	@Override
	public void handleEvent(AbortChargingProcessEvent event) {
		// handles an unsuccessful charging process after trying several chargers
		addScoreForPerson(event.getPersonId(), parameters.failedChargingProcess);
		trackScoreForPerson(event.getTime(), event.getPersonId(), "failed_process", parameters.failedChargingProcess,
				null);
	}

	// WAITING scoring

	private final IdMap<Vehicle, Double> enterQueueTimes = new IdMap<>(Vehicle.class);

	private void initializeWaiting() {
		enterQueueTimes.clear();
	}

	@Override
	public void handleEvent(QueuedAtChargerEvent event) {
		enterQueueTimes.put(event.getVehicleId(), event.getTime());
	}

	private void handleQuitQueue(Id<Vehicle> vehicleId, double quitTime) {
		Double enterTime = enterQueueTimes.remove(vehicleId);

		if (enterTime != null && parameters.waitTime_min != 0.0) {
			double waitTime_min = (quitTime - enterTime) / 60.0;
			addScoreForVehicle(vehicleId, parameters.waitTime_min * waitTime_min);
			trackScoreForVehicle(quitTime, vehicleId, "wait_time_min", parameters.waitTime_min * waitTime_min,
					waitTime_min);
		}
	}

	@Override
	public void handleEvent(QuitQueueAtChargerEvent event) {
		handleQuitQueue(event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(ChargingStartEvent event) {
		handleQuitQueue(event.getVehicleId(), event.getTime());
		handleStartCharging(event);
	}

	// COST scoring

	private record ChargingStartState(double time, double charge) {
	}

	private final IdMap<Vehicle, ChargingStartState> chargingStartStates = new IdMap<>(Vehicle.class);

	private record MoneyRecord(Id<Person> personId, Id<Charger> chargerId, double amount) {
	}

	private final List<MoneyRecord> moneyEvents = Collections.synchronizedList(new LinkedList<>());

	private void initializeCosts() {
		chargingStartStates.clear();
		moneyEvents.clear();
	}

	private void handleStartCharging(ChargingStartEvent event) {
		chargingStartStates.put(event.getVehicleId(), new ChargingStartState(event.getTime(), event.getCharge()));
	}

	private void handleFinishCharging(ChargingEndEvent event) {
		ChargingStartState start = chargingStartStates.remove(event.getVehicleId());

		double duration = event.getTime() - start.time;
		double energy = event.getCharge() - start.charge;

		Id<Person> personId = getPerson(event.getVehicleId());

		if (personId != null) {
			double cost = costCalculator.calculateChargingCost(personId, event.getChargerId(), duration, energy);
			addScoreForVehicle(event.getVehicleId(), cost * parameters.cost);
			trackScoreForVehicle(event.getTime(), event.getVehicleId(), "cost", cost * parameters.cost, cost);

			if (cost != 0.0) {
				moneyEvents.add(new MoneyRecord(personId, event.getChargerId(), cost));
			}
		}
	}

	// DETOUR scoring : works by calculating the travel time according to schedule
	// and comparing the recorded travel time

	private record DetourPair(AtomicDouble travelTime, AtomicDouble travelDistance) {
	}

	private final IdMap<Vehicle, DetourPair> detours = new IdMap<>(Vehicle.class);
	private final IdMap<Vehicle, Double> linkEnterTimes = new IdMap<>(Vehicle.class);

	private void initializeDetours() {
		linkEnterTimes.clear();
		detours.clear();

		for (Person person : activePersons) {
			Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(person, chargingMode);

			AtomicDouble travelTime = new AtomicDouble(0.0);
			AtomicDouble travelDistance = new AtomicDouble(0.0);

			for (Leg leg : TripStructureUtils.getLegs(person.getSelectedPlan())) {
				travelTime.addAndGet(-leg.getTravelTime().seconds());
				travelDistance.addAndGet(-leg.getRoute().getDistance());
			}

			detours.put(vehicleId, new DetourPair(travelTime, travelDistance));
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (detours.containsKey(event.getVehicleId())) {
			linkEnterTimes.put(event.getVehicleId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Double enterTime = linkEnterTimes.remove(event.getVehicleId());

		if (enterTime != null) {
			DetourPair pair = detours.get(event.getVehicleId());
			pair.travelTime.addAndGet(event.getTime() - enterTime);
			pair.travelDistance.addAndGet(network.getLinks().get(event.getLinkId()).getLength());
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		linkEnterTimes.remove(event.getVehicleId());
	}

	private void finalizeDetours(double now) {
		for (var entry : detours.entrySet()) {
			double detourTravelTime_min = Math.max(0.0, entry.getValue().travelTime().get()) / 60.0;
			double detourTravelDistance_km = Math.max(0.0, entry.getValue().travelTime().get()) * 1e-3;

			if (detourTravelTime_min * parameters.detourTime_min != 0.0) {
				addScoreForVehicle(entry.getKey(), detourTravelTime_min * parameters.detourTime_min);
				trackScoreForVehicle(now, entry.getKey(), "detour_time_min",
						detourTravelTime_min * parameters.detourTime_min,
						detourTravelTime_min);
			}

			if (detourTravelDistance_km * parameters.detourDistance_km != 0.0) {
				addScoreForVehicle(entry.getKey(), detourTravelDistance_km * parameters.detourDistance_km);
				trackScoreForVehicle(now, entry.getKey(), "detour_distance_km",
						detourTravelDistance_km * parameters.detourDistance_km, detourTravelDistance_km);
			}
		}
	}

	private double simStepTime = 0.0;

	@Override
	public void doSimStep(double time) {
		simStepTime = time;

		for (MoneyRecord moneyRecord : moneyEvents) {
			eventsManager.processEvent(
					new PersonMoneyEvent(time, moneyRecord.personId, moneyRecord.amount, MONEY_EVENT_PURPOSE,
							moneyRecord.chargerId.toString(),
							null));
		}

		moneyEvents.clear();
	}

	@Override
	public void onPrepareSim() {
	}

	@Override
	public void afterSim() {
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
	}

	/**
	 * Sets the minimum SoC under which a person doesn't want to fall.
	 */
	static public void setMinimumSoc(Person person, double minimumSoc) {
		person.getAttributes().putAttribute(MINIMUM_SOC_PERSON_ATTRIBUTE, minimumSoc);
	}

	/**
	 * Returns the minimum SoC under which a person doesn't want to fall.
	 */
	static public Double getMinimumSoc(Person person) {
		return (Double) person.getAttributes().getAttribute(MINIMUM_SOC_PERSON_ATTRIBUTE);
	}

	/**
	 * Sets the minimum SoC under which a person doesn't want to be at the end of
	 * the day.
	 */
	static public void setMinimumEndSoc(Person person, double minimumEndSoc) {
		person.getAttributes().putAttribute(MINIMUM_END_SOC_PERSON_ATTRIBUTE, minimumEndSoc);
	}

	/**
	 * Returns the minimum SoC under which a person doesn't want to be at the end of
	 * the day.
	 */
	static public Double getMinimumEndSoc(Person person) {
		return (Double) person.getAttributes().getAttribute(MINIMUM_END_SOC_PERSON_ATTRIBUTE);
	}
}
