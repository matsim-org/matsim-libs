package org.matsim.contrib.drt.extension.insertion;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.insertion.ConfigurableCostCalculatorStrategy.SoftInsertionParams;
import org.matsim.contrib.drt.extension.insertion.constraints.ExclusivityConstraint;
import org.matsim.contrib.drt.extension.insertion.constraints.ExclusivityConstraint.ExclusivityVoter;
import org.matsim.contrib.drt.extension.insertion.constraints.SingleRequestConstraint;
import org.matsim.contrib.drt.extension.insertion.constraints.SkillsConstraint;
import org.matsim.contrib.drt.extension.insertion.constraints.SkillsConstraint.RequestRequirementsSupplier;
import org.matsim.contrib.drt.extension.insertion.constraints.SkillsConstraint.VehicleSkillsSupplier;
import org.matsim.contrib.drt.extension.insertion.constraints.VehicleRangeConstraint;
import org.matsim.contrib.drt.extension.insertion.constraints.VehicleRangeConstraint.VehicleRangeSupplier;
import org.matsim.contrib.drt.extension.insertion.distances.DistanceApproximator;
import org.matsim.contrib.drt.extension.insertion.distances.DistanceCalculator;
import org.matsim.contrib.drt.extension.insertion.distances.EuclideanDistanceApproximator;
import org.matsim.contrib.drt.extension.insertion.distances.RoutingDistanceCalculator;
import org.matsim.contrib.drt.extension.insertion.objectives.PassengerDelayObjective;
import org.matsim.contrib.drt.extension.insertion.objectives.VehicleActiveTimeObjective;
import org.matsim.contrib.drt.extension.insertion.objectives.VehicleDistanceObjective;
import org.matsim.contrib.drt.extension.insertion.objectives.VehicleDistanceObjective.VehcileDistanceWeights;
import org.matsim.contrib.drt.optimizer.insertion.CostCalculationStrategy;
import org.matsim.contrib.drt.passenger.DrtOfferAcceptor;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.common.base.Preconditions;
import com.google.inject.Key;
import com.google.inject.Singleton;

public class DrtInsertionModule extends AbstractDvrpModeQSimModule {
	public DrtInsertionModule(DrtConfigGroup drtConfig) {
		super(drtConfig.getMode());
	}

	// EXCLUSIVITY CONSTRAINT

	private boolean useExclusivityContraint = false;
	private Class<? extends ExclusivityVoter> exclusivityVoterClass;
	private ExclusivityVoter exclusivityVoter;

	public DrtInsertionModule withExclusivity(Class<? extends ExclusivityVoter> exclusivityVoterClass) {
		Preconditions.checkState(!this.useExclusivityContraint);
		this.useExclusivityContraint = true;
		this.exclusivityVoterClass = exclusivityVoterClass;
		return this;
	}

	public DrtInsertionModule withExclusivity(ExclusivityVoter exclusivityVoter) {
		Preconditions.checkState(!this.useExclusivityContraint);
		this.useExclusivityContraint = true;
		this.exclusivityVoter = exclusivityVoter;
		return this;
	}

	private void configureExclusivityConstraint(List<Key<? extends DrtInsertionConstraint>> constraintBindings) {
		if (useExclusivityContraint) {
			if (exclusivityVoter != null) {
				bindModal(ExclusivityVoter.class).toInstance(exclusivityVoter);
			}

			if (exclusivityVoterClass != null) {
				bindModal(ExclusivityVoter.class).to(modalKey(exclusivityVoterClass));
			}

			bindModal(ExclusivityConstraint.class).toProvider(modalProvider(getter -> {
				ExclusivityVoter exclusivityVoter = getter.getModal(ExclusivityVoter.class);
				return new ExclusivityConstraint(exclusivityVoter);
			})).in(Singleton.class);

			constraintBindings.add(modalKey(ExclusivityConstraint.class));
		}
	}

	// SINGLE REQUEST CONSTRAINT

	private boolean useSingleRequestContraint = false;

	public DrtInsertionModule withSingleRequestPerPerson() {
		Preconditions.checkState(!this.useSingleRequestContraint);
		this.useSingleRequestContraint = true;
		return this;
	}

	private void configureSingleRequestConstraint(List<Key<? extends DrtInsertionConstraint>> constraintBindings) {
		if (useSingleRequestContraint) {
			bindModal(SingleRequestConstraint.class).toProvider(modalProvider(getter -> {
				return new SingleRequestConstraint();
			})).in(Singleton.class);

			addMobsimScopeEventHandlerBinding().to(SingleRequestConstraint.class);
			constraintBindings.add(modalKey(SingleRequestConstraint.class));
		}
	}

	// SKILLS CONSTRAINT

	private boolean useSkillsConstraint = false;

	private Class<? extends VehicleSkillsSupplier> vehicleSkillsSupplierClass;
	private VehicleSkillsSupplier vehicleSkillsSupplier;

	private Class<? extends RequestRequirementsSupplier> requestRequirementsSupplierClass;
	private RequestRequirementsSupplier requestRequirementsSupplier;

	public DrtInsertionModule withSkills(VehicleSkillsSupplier vehicleSkillsSupplier,
			RequestRequirementsSupplier requestRequirementsSupplier) {
		Preconditions.checkState(!this.useSkillsConstraint);
		this.vehicleSkillsSupplier = vehicleSkillsSupplier;
		this.requestRequirementsSupplier = requestRequirementsSupplier;
		this.useSkillsConstraint = true;
		return this;
	}

	public DrtInsertionModule withSkills(Class<? extends VehicleSkillsSupplier> vehicleSkillsSupplierClass,
			Class<? extends RequestRequirementsSupplier> requestRequirementsSupplierClass) {
		Preconditions.checkState(!this.useSkillsConstraint);
		this.vehicleSkillsSupplierClass = vehicleSkillsSupplierClass;
		this.requestRequirementsSupplierClass = requestRequirementsSupplierClass;
		this.useSkillsConstraint = true;
		return this;
	}

	private void configureSkillsContraint(List<Key<? extends DrtInsertionConstraint>> constraintBindings) {
		if (useSkillsConstraint) {
			if (vehicleSkillsSupplier != null) {
				bindModal(VehicleSkillsSupplier.class).toInstance(vehicleSkillsSupplier);
			}

			if (vehicleSkillsSupplierClass != null) {
				bindModal(VehicleSkillsSupplier.class).to(modalKey(vehicleSkillsSupplierClass));
			}

			if (requestRequirementsSupplier != null) {
				bindModal(RequestRequirementsSupplier.class).toInstance(requestRequirementsSupplier);
			}

			if (requestRequirementsSupplierClass != null) {
				bindModal(RequestRequirementsSupplier.class).to(modalKey(requestRequirementsSupplierClass));
			}

			bindModal(SkillsConstraint.class).toProvider(modalProvider(getter -> {
				VehicleSkillsSupplier vehicleSkillsSupplier = getter.getModal(VehicleSkillsSupplier.class);
				RequestRequirementsSupplier requestRequirementsSupplier = getter
						.getModal(RequestRequirementsSupplier.class);
				return new SkillsConstraint(vehicleSkillsSupplier, requestRequirementsSupplier);
			})).in(Singleton.class);

			constraintBindings.add(modalKey(SkillsConstraint.class));
		}
	}

	// CUSTOM CONSTRAINTS

	private final List<Class<? extends DrtInsertionConstraint>> customConstraintClasses = new LinkedList<>();
	private final List<DrtInsertionConstraint> customConstraints = new LinkedList<>();

	public DrtInsertionModule withConstraint(Class<? extends DrtInsertionConstraint> constraintClass) {
		customConstraintClasses.add(constraintClass);
		return this;
	}

	public DrtInsertionModule withConstraint(DrtInsertionConstraint constraint) {
		customConstraints.add(constraint);
		return this;
	}

	// DISTANCES

	private DistanceCalculator distanceCalculator;
	private DistanceApproximator distanceApproximator;

	private Class<? extends DistanceCalculator> distanceCalculatorClass;
	private Class<? extends DistanceApproximator> distanceApproximatorClass;

	public DrtInsertionModule withDistanceCalculator(DistanceCalculator distanceCalculator) {
		Preconditions.checkState(this.distanceCalculator == null && this.distanceCalculatorClass == null);
		this.distanceCalculator = distanceCalculator;
		return this;
	}

	public DrtInsertionModule withDistanceCalculator(Class<? extends DistanceCalculator> distanceCalculatorClass) {
		Preconditions.checkState(this.distanceCalculator == null && this.distanceCalculatorClass == null);
		this.distanceCalculatorClass = distanceCalculatorClass;
		return this;
	}

	public DrtInsertionModule withDistanceApproximator(DistanceApproximator distanceApproximator) {
		Preconditions.checkState(this.distanceApproximator == null && this.distanceApproximatorClass == null);
		this.distanceApproximator = distanceApproximator;
		return this;
	}

	public DrtInsertionModule withDistanceApproximator(
			Class<? extends DistanceApproximator> distanceApproximatorClass) {
		Preconditions.checkState(this.distanceApproximator == null && this.distanceApproximatorClass == null);
		this.distanceApproximatorClass = distanceApproximatorClass;
		return this;
	}

	public DrtInsertionModule withEuclideanDistanceApproximator(double euclideanDistanceFactor) {
		Preconditions.checkState(this.distanceApproximator == null && this.distanceApproximatorClass == null);
		this.distanceApproximator = new EuclideanDistanceApproximator(euclideanDistanceFactor);
		return this;
	}

	public DrtInsertionModule withEuclideanDistanceCalculator(double euclideanDistanceFactor) {
		Preconditions.checkState(this.distanceCalculator == null && this.distanceCalculatorClass == null);
		this.distanceCalculator = new EuclideanDistanceApproximator(euclideanDistanceFactor);
		return this;
	}

	private void configureDistances() {
		bindModal(RoutingDistanceCalculator.class).toProvider(modalProvider(getter -> {
			LeastCostPathCalculatorFactory factory = new SpeedyALTFactory();

			TravelTime travelTime = getter.getModal(TravelTime.class);
			TravelDisutility travelDisutility = new OnlyTimeDependentTravelDisutility(travelTime);
			Network network = getter.getModal(Network.class);

			printDistanceWarning();

			return new RoutingDistanceCalculator(factory.createPathCalculator(network, travelDisutility, travelTime),
					travelTime);
		}));

		if (distanceCalculator == null && distanceCalculatorClass == null) {
			distanceCalculatorClass = RoutingDistanceCalculator.class;
		}

		if (distanceApproximator == null && distanceApproximatorClass == null) {
			distanceApproximator = DistanceApproximator.NULL;
		}

		if (distanceCalculator != null) {
			bindModal(DistanceCalculator.class).toInstance(distanceCalculator);
		}

		if (distanceCalculatorClass != null) {
			bindModal(DistanceCalculator.class).to(modalKey(distanceCalculatorClass));
		}

		if (distanceApproximator != null) {
			bindModal(DistanceApproximator.class).toInstance(distanceApproximator);
		}

		if (distanceApproximatorClass != null) {
			bindModal(DistanceApproximator.class).to(modalKey(distanceApproximatorClass));
		}
	}

	// RANGE CONSTRAINT

	private boolean useRangeConstraint = false;

	private VehicleRangeSupplier vehicleRangeSupplier;
	private Class<? extends VehicleRangeSupplier> vehicleRangeSupplierClass;

	public DrtInsertionModule withVehicleRange(VehicleRangeSupplier rangeSupplier) {
		Preconditions.checkState(!useRangeConstraint);
		useRangeConstraint = true;
		this.vehicleRangeSupplier = rangeSupplier;
		return this;
	}

	public DrtInsertionModule withVehicleRange(double vehicleRange) {
		withVehicleRange(v -> vehicleRange);
		return this;
	}

	public DrtInsertionModule withVehicleRange(Class<? extends VehicleRangeSupplier> rangeSupplierClass) {
		Preconditions.checkState(!useRangeConstraint);
		useRangeConstraint = true;
		this.vehicleRangeSupplierClass = rangeSupplierClass;
		return this;
	}

	private void configureRangeContraint(List<Key<? extends DrtInsertionConstraint>> constraintBindings) {
		if (useRangeConstraint) {
			if (vehicleRangeSupplier != null) {
				bindModal(VehicleRangeSupplier.class).toInstance(vehicleRangeSupplier);
			}

			if (vehicleRangeSupplierClass != null) {
				bindModal(VehicleRangeSupplier.class).to(modalKey(vehicleRangeSupplierClass));
			}

			bindModal(VehicleRangeConstraint.class).toProvider(modalProvider(getter -> {
				DistanceCalculator distanceCalculator = getter.getModal(DistanceCalculator.class);
				DistanceApproximator distanceApproximator = getter.getModal(DistanceApproximator.class);

				if (distanceApproximator == DistanceApproximator.NULL) {
					distanceApproximator = null;
				}

				return new VehicleRangeConstraint(vehicleRangeSupplier, distanceCalculator, distanceApproximator);
			}));

			constraintBindings.add(modalKey(VehicleRangeConstraint.class));
		}
	}

	// SOFT INSERTION

	private boolean useSoftInsertionPenalties = false;
	private ConfigurableCostCalculatorStrategy.SoftInsertionParams softInsertionParams;

	public DrtInsertionModule withSoftInsertionPenalty(double pickupWeight, double dropoffWeight) {
		Preconditions.checkState(!this.useSoftInsertionPenalties);
		this.useSoftInsertionPenalties = true;
		this.softInsertionParams = new SoftInsertionParams(pickupWeight, dropoffWeight);
		return this;
	}

	// PICKUP AND DROPOFF TIME WINDOW

	private double promisedPickupTimeWindow = Double.POSITIVE_INFINITY;
	private double promisedDropoffTimeWindow = Double.POSITIVE_INFINITY;

	public DrtInsertionModule withPromisedPickupTimeWindow(double promisedPickupTimeWindow) {
		this.promisedPickupTimeWindow = promisedPickupTimeWindow;
		return this;
	}

	public DrtInsertionModule withPromisedDropoffTimeWindow(double promisedDropoffTimeWindow) {
		this.promisedDropoffTimeWindow = promisedDropoffTimeWindow;
		return this;
	}

	// OBJECTIVES

	private enum Objective {
		Custom, VehicleActiveTime, PassengerDelay, VehicleDistance
	}

	private Objective selectedObjective = null;

	// Vehicle active time

	public DrtInsertionModule minimizeVehicleActiveTime() {
		Preconditions.checkState(selectedObjective == null);
		selectedObjective = Objective.VehicleActiveTime;
		return this;
	}

	private void configureVehicleActiveTimeObjective() {
		bindModal(DrtInsertionObjective.class).toInstance(new VehicleActiveTimeObjective());
	}

	// Passenger delay

	private double passengerDelayPickupWeight = 1.0;
	private double passengerDelayDropoffWeight = 1.0;

	public DrtInsertionModule minimizePassengerDelay() {
		return minimizePassengerDelay(1.0, 1.0);
	}

	public DrtInsertionModule minimizePassengerDelay(double pickupWeight, double dropoffWeight) {
		Preconditions.checkState(selectedObjective == null);
		selectedObjective = Objective.PassengerDelay;
		passengerDelayPickupWeight = pickupWeight;
		passengerDelayDropoffWeight = dropoffWeight;
		return this;
	}

	private void configurePassengerDelayObjective() {
		bindModal(DrtInsertionObjective.class)
				.toInstance(new PassengerDelayObjective(passengerDelayPickupWeight, passengerDelayDropoffWeight));
	}

	// Custom objective

	private DrtInsertionObjective configuredObjective;
	private Class<? extends DrtInsertionObjective> configuredObjectiveClass;

	public DrtInsertionModule minimize(DrtInsertionObjective objective) {
		Preconditions.checkState(selectedObjective == null);
		selectedObjective = Objective.Custom;
		configuredObjective = objective;
		return this;
	}

	public DrtInsertionModule minimize(Class<? extends DrtInsertionObjective> objective) {
		Preconditions.checkState(selectedObjective == null);
		selectedObjective = Objective.Custom;
		configuredObjectiveClass = objective;
		return this;
	}

	private void configureCustomObjective() {
		if (configuredObjective != null) {
			bindModal(DrtInsertionObjective.class).toInstance(configuredObjective);
		}

		if (configuredObjectiveClass != null) {
			bindModal(DrtInsertionObjective.class).to(modalKey(configuredObjectiveClass));
		}
	}

	// Vehicle distance

	private VehcileDistanceWeights vehicleDistanceWeights = null;
	private double distanceObjectiveEstimationFactor = Double.NaN;

	public DrtInsertionModule minimizeDistances(double occupiedWeight, double emptyWeight, double passengerWeight) {
		Preconditions.checkState(selectedObjective == null);
		selectedObjective = Objective.VehicleDistance;
		vehicleDistanceWeights = new VehcileDistanceWeights(occupiedWeight, emptyWeight, passengerWeight);
		return this;
	}

	public DrtInsertionModule minimizeVehicleDistance() {
		Preconditions.checkState(selectedObjective == null);
		selectedObjective = Objective.VehicleDistance;
		vehicleDistanceWeights = new VehcileDistanceWeights(1.0, 1.0, 0.0);
		return this;
	}

	public DrtInsertionModule minimizeEmptyDistance() {
		Preconditions.checkState(selectedObjective == null);
		selectedObjective = Objective.VehicleDistance;
		vehicleDistanceWeights = new VehcileDistanceWeights(0.0, 1.0, 0.0);
		return this;
	}

	public DrtInsertionModule withDistanceObjectiveFactor(double estimationFactor) {
		this.distanceObjectiveEstimationFactor = estimationFactor;
		return this;
	}

	private void configureVehicleDistanceObjective() {
		bindModal(DrtInsertionObjective.class).toProvider(modalProvider(getter -> {
			final DistanceCalculator distanceCalculator;

			if (Double.isFinite(distanceObjectiveEstimationFactor)) {
				distanceCalculator = new EuclideanDistanceApproximator(distanceObjectiveEstimationFactor);
			} else {
				distanceCalculator = getter.getModal(RoutingDistanceCalculator.class);
			}

			return new VehicleDistanceObjective(vehicleDistanceWeights, distanceCalculator);
		}));
	}

	@Override
	protected void configureQSim() {
		configureDistances();

		List<Key<? extends DrtInsertionConstraint>> constraintBindings = new LinkedList<>();
		configureExclusivityConstraint(constraintBindings);
		configureSingleRequestConstraint(constraintBindings);
		configureSkillsContraint(constraintBindings);
		configureRangeContraint(constraintBindings);

		if (selectedObjective == null) {
			selectedObjective = Objective.VehicleActiveTime;
		}

		switch (selectedObjective) {
		case Custom:
			configureCustomObjective();
			break;
		case PassengerDelay:
			configurePassengerDelayObjective();
			break;
		case VehicleDistance:
			configureVehicleDistanceObjective();
			break;
		case VehicleActiveTime:
		default:
			configureVehicleActiveTimeObjective();
			break;
		}

		bindModal(ConfigurableCostCalculatorStrategy.class).toProvider(modalProvider(getter -> {
			List<DrtInsertionConstraint> constraints = new LinkedList<>();

			for (Key<? extends DrtInsertionConstraint> constraintBinding : constraintBindings) {
				constraints.add(getter.get(constraintBinding));
			}

			for (Class<? extends DrtInsertionConstraint> constraintClass : customConstraintClasses) {
				constraints.add(getter.getModal(constraintClass));
			}

			for (DrtInsertionConstraint constraint : customConstraints) {
				constraints.add(constraint);
			}

			return new ConfigurableCostCalculatorStrategy(getter.getModal(DrtInsertionObjective.class), constraints,
					softInsertionParams);
		})).in(Singleton.class);

		bindModal(PromisedTimeWindowOfferAcceptor.class).toProvider(modalProvider(getter -> {
			return new PromisedTimeWindowOfferAcceptor(promisedPickupTimeWindow, promisedDropoffTimeWindow);
		})).in(Singleton.class);

		bindModal(CostCalculationStrategy.class).to(modalKey(ConfigurableCostCalculatorStrategy.class));
		bindModal(DrtOfferAcceptor.class).to(modalKey(PromisedTimeWindowOfferAcceptor.class));
	}

	private final static Logger logger = LogManager.getLogger(DrtInsertionModule.class);

	private static void printDistanceWarning() {
		logger.warn(
				"Depending on your configruation, distance-based objectives and constraints may be very impacting on performance. The features should be considered experimental. See discussion of #2947 on Github.");
	}
}
