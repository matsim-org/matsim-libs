package org.matsim.contrib.drt.extension.alonso_mora;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.matsim.contrib.drt.extension.alonso_mora.algorithm.assignment.BestResponseAssignmentSolver;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.assignment.CbcMpsAssignmentSolver;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.assignment.GlpkMpsAssignmentSolver;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.relocation.BestResponseRelocationSolver;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.relocation.CbcMpsRelocationSolver;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.relocation.GlpkMpsRelocationSolver;
import org.matsim.contrib.drt.extension.alonso_mora.travel_time.DrtDetourTravelTimeEstimator;
import org.matsim.contrib.drt.extension.alonso_mora.travel_time.EuclideanTravelTimeEstimator;
import org.matsim.contrib.drt.extension.alonso_mora.travel_time.HybridTravelTimeEstimator;
import org.matsim.contrib.drt.extension.alonso_mora.travel_time.MatrixTravelTimeEstimator;
import org.matsim.contrib.drt.extension.alonso_mora.travel_time.RoutingTravelTimeEstimator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import com.google.common.base.Verify;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Config group for the dispatching extension of DRT including the algorithm by
 * Alonso-Mora et al.
 */
public class AlonsoMoraConfigGroup extends ReflectiveConfigGroup {
	public final static String GROUP_NAME = "drtAlonsoMora";

	public AlonsoMoraConfigGroup() {
		super(GROUP_NAME);

		prepareAvailableComponents();
		prepareDefaultComponents();
	}

	/* Integration */
	private final static String MODE = "mode";
	private final static String MODE_COMMENT = "The DRT mode that will use the Alonso-Mora algorithm";

	@NotBlank
	private String mode = "drt";

	@StringGetter(MODE)
	public String getMode() {
		return mode;
	}

	@StringSetter(MODE)
	public void setMode(String value) {
		this.mode = value;
	}

	/* General */

	private final static String LOGGING_INTERVAL = "loggingInterval";
	private final static String LOGGING_INTERVAL_COMMENT = "Frequency of logging current status of the dispatcher in [s]";

	@PositiveOrZero
	private double loggingInterval = 600;

	@StringGetter(LOGGING_INTERVAL)
	public double getLoggingInterval() {
		return loggingInterval;
	}

	@StringSetter(LOGGING_INTERVAL)
	public void setLoggingInterval(double value) {
		this.loggingInterval = value;
	}

	private final static String MAXIMUM_QUEUE_TIME = "maximumQueueTime";
	private final static String MAXIMUM_QUEUE_TIME_COMMENT = "Maximum time the request stays in the dispatching queue after its earliest departure time (submission without prebooking) in [s]. Note that this is capped by the latest pickup time. A value of zero means that requests need to be matched in the dispatching step that comes right after submission / earliest departure time.";

	@PositiveOrZero
	private double maximumQueueTime = 0.0;

	@StringGetter(MAXIMUM_QUEUE_TIME)
	public double getMaximumQueueTime() {
		return maximumQueueTime;
	}

	@StringSetter(MAXIMUM_QUEUE_TIME)
	public void setMaximumQueueTime(double value) {
		this.maximumQueueTime = value;
	}

	private final static String MAXIMUM_GROUP_REQUEST_SIZE = "maximumGroupRequestSize";
	private final static String MAXIMUM_GROUP_REQUEST_SIZE_COMMENT = "For computational reasons, the implementation can group multiple individual requests with the same departure time and OD requirements into one aggregate request. This value defines the size limit for aggregation as large group requests may not fit in some vehicles (based on their capacity).";

	@Positive
	private int maximumGroupRequestSize = 6;

	@StringGetter(MAXIMUM_GROUP_REQUEST_SIZE)
	public int getMaximumGroupRequestSize() {
		return maximumGroupRequestSize;
	}

	@StringSetter(MAXIMUM_GROUP_REQUEST_SIZE)
	public void setMaximumGroupRequestSize(int value) {
		this.maximumGroupRequestSize = value;
	}

	private final static String USE_PLANNED_PICKUP_TIME = "usePlannedPickupTime";
	private final static String USE_PLANNED_PICKUP_TIME_COMMENT = "By default, the algorithm updates the latest pickup time for a request to the planned pickup time that has been calculated at the first assignment. Subsequent dispatching steps must adhere to that value. Using this flag, this functionality may be turned off.";

	private boolean usePlannedPickupTime = true;

	@StringGetter(USE_PLANNED_PICKUP_TIME)
	public boolean getUsePlannedPickupTime() {
		return usePlannedPickupTime;
	}

	@StringSetter(USE_PLANNED_PICKUP_TIME)
	public void setUsePlannedPickupTime(boolean value) {
		this.usePlannedPickupTime = value;
	}

	private final static String PLANNED_PICKUP_TIME_SLACK = "plannedPickupTimeSlack";
	private final static String PLANNED_PICKUP_TIME_SLACK_COMMENT = "See usePlannedPickupTime. When updating the required pickup time, the operator may add a little slack to provide a more pessimistic estimate. The value specified here is added to the planned pickup time on first assignment if usePlannedPickupTime is enabled.";

	@PositiveOrZero
	private double plannedPickupTimeSlack = 0;

	@StringGetter(PLANNED_PICKUP_TIME_SLACK)
	public double getPlannedPickupTimeSlack() {
		return plannedPickupTimeSlack;
	}

	@StringSetter(PLANNED_PICKUP_TIME_SLACK)
	public void setPlannedPickupTimeSlack(double value) {
		this.plannedPickupTimeSlack = value;
	}

	private final static String CHECK_DETERMINISTIC_TRAVEL_TIMES = "checkDeterministicTravelTimes";
	private final static String CHECK_DETERMINISTIC_TRAVEL_TIMES_COMMENT = "Under ideal and correctly configured freeflow conditions, the algorithm will predict exactly what the vehicles will do in simulation. If this flag is enabled, the algorithm will perform self-checks to verify that this is the case. Use to verify your freeflow condiditons.";

	private boolean checkDeterminsticTravelTimes = false;

	@StringGetter(CHECK_DETERMINISTIC_TRAVEL_TIMES)
	public boolean getCheckDeterminsticTravelTimes() {
		return checkDeterminsticTravelTimes;
	}

	@StringSetter(CHECK_DETERMINISTIC_TRAVEL_TIMES)
	public void setCheckDeterminsticTravelTimes(boolean value) {
		this.checkDeterminsticTravelTimes = value;
	}

	/* Scheduling */

	private final static String REROUTE_DURING_SCHEDULING = "rerouteDuringScheduling";
	private final static String REROUTE_DURING_SCHEDULING_COMMENT = "During scheduling of the pickups and dropoffs we may find situations in which the vehicle is already on the way to its next destination on the current drive task, so not rerouting is necessary. However, it may be wanted if traffic conditions change frequently. This flag will enable rerouting for already pre-routed segments of the schedule.";

	private boolean rerouteDuringScheduling = false;

	@StringGetter(REROUTE_DURING_SCHEDULING)
	public boolean getRerouteDuringScheduling() {
		return rerouteDuringScheduling;
	}

	@StringSetter(REROUTE_DURING_SCHEDULING)
	public void setRerouteDuringScheduling(boolean value) {
		this.rerouteDuringScheduling = value;
	}

	/* Sequence generator */

	public enum SequenceGeneratorType {
		Extensive, Insertive, Combined, EuclideanBestResponse
	}

	private final static String SEQUENCE_GENERATOR_TYPE = "sequenceGeneratorType";
	private final static String SEQUENCE_GENERATOR_TYPE_COMMENT = "Defines which sequence generator to use: Extensive (trying to find all arrangements of pickups and dropoff for a route), Insertive (inserting new pickups and dropoffs in the existing order along a vehicle's route), Combined (Extensive below insertionStartOccupancy, Insertive after), EuclideanBestResponse (as a very fast test generator based on stepwise adding the closest pickup and dropoff by Euclidean distnace). ";

	private SequenceGeneratorType sequenceGeneratorType = SequenceGeneratorType.Combined;

	@StringGetter(SEQUENCE_GENERATOR_TYPE)
	public SequenceGeneratorType getSequenceGeneratorType() {
		return sequenceGeneratorType;
	}

	@StringSetter(SEQUENCE_GENERATOR_TYPE)
	public void setSequenceGeneratorType(SequenceGeneratorType value) {
		this.sequenceGeneratorType = value;
	}

	private final static String INSERTION_START_OCCUPANCY = "insertionStartOccupancy";
	private final static String INSERTION_START_OCCUPANCY_COMMENT = "Defines the occupany at which the Combined sequence generator will switch from Extensive to Insertive mode.";

	@PositiveOrZero
	private int insertionStartOccupancy = 5;

	@StringGetter(INSERTION_START_OCCUPANCY)
	public int getInsertionStartOccupancy() {
		return insertionStartOccupancy;
	}

	@StringSetter(INSERTION_START_OCCUPANCY)
	public void setInsertionStartOccupancy(int value) {
		this.insertionStartOccupancy = value;
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(MODE, MODE_COMMENT);
		comments.put(LOGGING_INTERVAL, LOGGING_INTERVAL_COMMENT);
		comments.put(MAXIMUM_QUEUE_TIME, MAXIMUM_QUEUE_TIME_COMMENT);
		comments.put(MAXIMUM_GROUP_REQUEST_SIZE, MAXIMUM_GROUP_REQUEST_SIZE_COMMENT);
		comments.put(USE_PLANNED_PICKUP_TIME, USE_PLANNED_PICKUP_TIME_COMMENT);
		comments.put(PLANNED_PICKUP_TIME_SLACK, PLANNED_PICKUP_TIME_SLACK_COMMENT);
		comments.put(CHECK_DETERMINISTIC_TRAVEL_TIMES, CHECK_DETERMINISTIC_TRAVEL_TIMES_COMMENT);
		comments.put(REROUTE_DURING_SCHEDULING, REROUTE_DURING_SCHEDULING_COMMENT);
		comments.put(SEQUENCE_GENERATOR_TYPE, SEQUENCE_GENERATOR_TYPE_COMMENT);
		comments.put(INSERTION_START_OCCUPANCY, INSERTION_START_OCCUPANCY_COMMENT);
		comments.put(RELOCATION_INTERVAL, RELOCATION_INTERVAL_COMMENT);
		comments.put(USE_BINDING_RELOCATIONS, USE_BINDING_RELOCATIONS_COMMENT);
		comments.put(ASSIGNMENT_INTERVAL, ASSIGNMENT_INTERVAL_COMMENT);
		comments.put(REJECTION_PENALTY, REJECTION_PENALTY_COMMENT);
		comments.put(UNASSIGNMENT_PENALTY, UNASSIGNMENT_PENALTY_COMMENT);
		comments.put(VIOLATION_FACTOR, VIOLATION_FACTOR_COMMENT);
		comments.put(VIOLATION_OFFSET, VIOLATION_OFFSET_COMMENT);
		comments.put(PREFER_NON_VIOLATION, PREFER_NON_VIOLATION_COMMENT);
		return comments;
	}

	/* Congestion mitigation */

	static public class CongestionMitigationParameters extends ReflectiveConfigGroup {
		static public final String SET_NAME = "congestionMitigation";

		public CongestionMitigationParameters() {
			super(SET_NAME);
		}

		private final static String ALLOW_BARE_REASSIGNMENT = "allowBareReassignment";
		private final static String ALLOW_BARE_REASSIGNMENT_COMMENT = "In some dispatching steps no new request have arrived, so no reassignment is necessary. However, if congestion is involved one might want to perform a reassignment to react to changed traffic conditions.";

		private boolean allowBareReassignment = false;

		@StringGetter(ALLOW_BARE_REASSIGNMENT)
		public boolean getAllowBareReassignment() {
			return allowBareReassignment;
		}

		@StringSetter(ALLOW_BARE_REASSIGNMENT)
		public void setAllowBareReassignment(boolean value) {
			this.allowBareReassignment = value;
		}

		private final static String PRESERVE_VEHICLE_ASSIGNMENTS = "preserveVehicleAssignments";
		private final static String PRESERVE_VEHICLE_ASSIGNMENTS_COMMENT = "Keep current assignments of a vehicle in the shareability graph also they might otherwise be filtered out due to changed traffic conditions.";

		private boolean preserveVehicleAssignments = true;

		@StringGetter(PRESERVE_VEHICLE_ASSIGNMENTS)
		public boolean getPreserveVehicleAssignments() {
			return preserveVehicleAssignments;
		}

		@StringSetter(PRESERVE_VEHICLE_ASSIGNMENTS)
		public void setPreserveVehicleAssignments(boolean value) {
			this.preserveVehicleAssignments = value;
		}

		private final static String ALLOW_PICKUP_VIOLATIONS = "allowPickupViolations";
		private final static String ALLOW_PICKUP_VIOLATIONS_COMMENT = "Allows that a request that is already assigned to the current vehicle can violate the pickup constraint if it is caused by traffic.";

		private boolean allowPickupViolations = true;

		@StringGetter(ALLOW_PICKUP_VIOLATIONS)
		public boolean getAllowPickupViolations() {
			return allowPickupViolations;
		}

		@StringSetter(ALLOW_PICKUP_VIOLATIONS)
		public void setAllowPickupViolations(boolean value) {
			this.allowPickupViolations = value;
		}

		private final static String ALLOW_PICKUPS_WITH_DROPOFF_VIOLATIONS = "allowPickupsWithDropoffViolations";
		private final static String ALLOW_PICKUPS_WITH_DROPOFF_VIOLATIONS_COMMENT = "Allows that new pickups can be integrated into a vehicle although some requests already have dropoff violations due to congestion.";

		private boolean allowPickupsWithDropoffViolations = true;

		@StringGetter(ALLOW_PICKUPS_WITH_DROPOFF_VIOLATIONS)
		public boolean getAllowPickupsWithDropoffViolations() {
			return allowPickupsWithDropoffViolations;
		}

		@StringSetter(ALLOW_PICKUPS_WITH_DROPOFF_VIOLATIONS)
		public void setAllowPickupsWithDropoffViolations(boolean value) {
			this.allowPickupsWithDropoffViolations = value;
		}

		@Override
		public Map<String, String> getComments() {
			Map<String, String> comments = super.getComments();
			comments.put(ALLOW_BARE_REASSIGNMENT, ALLOW_BARE_REASSIGNMENT_COMMENT);
			comments.put(PRESERVE_VEHICLE_ASSIGNMENTS, PRESERVE_VEHICLE_ASSIGNMENTS_COMMENT);
			comments.put(ALLOW_PICKUP_VIOLATIONS, ALLOW_PICKUP_VIOLATIONS_COMMENT);
			comments.put(ALLOW_PICKUPS_WITH_DROPOFF_VIOLATIONS, ALLOW_PICKUPS_WITH_DROPOFF_VIOLATIONS_COMMENT);
			return comments;
		}
	}

	/* Violations */

	private final static String VIOLATION_FACTOR = "violationFactor";
	private final static String VIOLATION_FACTOR_COMMENT = "Violations (for pickup and dropoff) are initially expressed in seconds. This factor is added to the violations to arrive at the final value.";

	private double violationFactor = 60.0;

	@StringGetter(VIOLATION_FACTOR)
	public double getViolationFactor() {
		return violationFactor;
	}

	@StringSetter(VIOLATION_FACTOR)
	public void setViolationFactor(double value) {
		this.violationFactor = value;
	}

	private final static String VIOLATION_OFFSET = "violationOffset";
	private final static String VIOLATION_OFFSET_COMMENT = "Constant value that is added to each solution that has any violations.";

	private double violationOffset = 10000.0;

	@StringGetter(VIOLATION_OFFSET)
	public double getViolationOffset() {
		return violationOffset;
	}

	@StringSetter(VIOLATION_OFFSET)
	public void setViolationOffset(double value) {
		this.violationOffset = value;
	}

	private final static String PREFER_NON_VIOLATION = "preferNonViolation";
	private final static String PREFER_NON_VIOLATION_COMMENT = "Always prefer solutions without violations, even if a solution with violations and lower objective has been found.";

	private boolean preferNonViolation = false;

	@StringGetter(PREFER_NON_VIOLATION)
	public boolean getPreferNonViolation() {
		return preferNonViolation;
	}

	@StringSetter(PREFER_NON_VIOLATION)
	public void setPreferNonViolation(boolean value) {
		this.preferNonViolation = value;
	}

	/* Assignment */

	static private final String ASSIGNMENT_INTERVAL = "assignmentInterval";
	static private final String ASSIGNMENT_INTERVAL_COMMENT = "The frequency with which assignment is performed";

	@PositiveOrZero
	private double assignmentInterval = 30;

	@StringGetter(ASSIGNMENT_INTERVAL)
	public double getAssignmentInterval() {
		return assignmentInterval;
	}

	@StringSetter(ASSIGNMENT_INTERVAL)
	public void setAssignmentInterval(double value) {
		this.assignmentInterval = value;
	}

	static private final String REJECTION_PENALTY = "rejectionPenalty";
	static private final String REJECTION_PENALTY_COMMENT = "Penalty in the ILP problem that is added for rejecting requests (before they have been assigned)";

	@PositiveOrZero
	private double rejectionPenalty = 24.0 * 3600.0;

	@StringGetter(REJECTION_PENALTY)
	public double getRejectionPenalty() {
		return rejectionPenalty;
	}

	@StringSetter(REJECTION_PENALTY)
	public void setRejectionPenalty(double value) {
		this.rejectionPenalty = value;
	}

	static private final String UNASSIGNMENT_PENALTY = "unassignmentPenalty";
	static private final String UNASSIGNMENT_PENALTY_COMMENT = "Penalty in the ILP problem that is added when not assigning and already assigned requests";

	@PositiveOrZero
	private double unassignmentPenalty = 24.0 * 3600.0 * 1000;

	@StringGetter(UNASSIGNMENT_PENALTY)
	public double getUnassignmentPenalty() {
		return unassignmentPenalty;
	}

	@StringSetter(UNASSIGNMENT_PENALTY)
	public void setUnassignmentPenalty(double value) {
		this.unassignmentPenalty = value;
	}

	/* Relocation */

	static private final String RELOCATION_INTERVAL = "relocationInterval";
	static private final String RELOCATION_INTERVAL_COMMENT = "The frequency with which relocation is performed";

	@PositiveOrZero
	private double relocationInterval = 30;

	@StringGetter(RELOCATION_INTERVAL)
	public double getRelocationInterval() {
		return relocationInterval;
	}

	@StringSetter(RELOCATION_INTERVAL)
	public void setRelocationInterval(double value) {
		this.relocationInterval = value;
	}

	static private final String USE_BINDING_RELOCATIONS = "useBindingRelocations";
	static private final String USE_BINDING_RELOCATIONS_COMMENT = "Defines whether vehicles that are already relocating can be used for relocation in the next relocation step";

	private boolean useBindingRelocations = false;

	@StringGetter(USE_BINDING_RELOCATIONS)
	public boolean getUseBindingRelocations() {
		return useBindingRelocations;
	}

	@StringSetter(USE_BINDING_RELOCATIONS)
	public void setUseBindingRelocations(boolean value) {
		this.useBindingRelocations = value;
	}

	/* Block handling */

	public static class AssignmentSolverParameters extends ReflectiveConfigGroup {
		static public final String SET_PREFIX = "assignmentSolver:";

		private final String solverType;

		public AssignmentSolverParameters(String solverType) {
			super(SET_PREFIX + solverType);
			this.solverType = solverType;
		}

		public String getSolverType() {
			return solverType;
		}

		static private final String RUNTIME_THRESHOLD = "runtimeThreshold_ms";
		static private final String RUNTIME_THRESHOLD_COMMENT = "Defines the runtime threshold of the assignment algorithm [ms]";

		private int runtimeThreshold = 3600 * 1000;

		@StringGetter(RUNTIME_THRESHOLD)
		public int getRuntimeThreshold() {
			return runtimeThreshold;
		}

		@StringSetter(RUNTIME_THRESHOLD)
		public void setRuntimeThreshold(int value) {
			this.runtimeThreshold = value;
		}

		@Override
		public Map<String, String> getComments() {
			Map<String, String> comments = super.getComments();
			comments.put(RUNTIME_THRESHOLD, RUNTIME_THRESHOLD_COMMENT);
			return comments;
		}
	}

	public static class RelocationSolverParameters extends ReflectiveConfigGroup {
		static public final String SET_PREFIX = "relocationSolver:";

		private final String solverType;

		public RelocationSolverParameters(String solverType) {
			super(SET_PREFIX + solverType);
			this.solverType = solverType;
		}

		public String getSolverType() {
			return solverType;
		}

		static private final String RUNTIME_THRESHOLD = "runtimeThreshold_ms";
		static private final String RUNTIME_THRESHOLD_COMMENT = "Defines the runtime threshold of the assignment algorithm [ms]";

		private int runtimeThreshold = 3600 * 1000;

		@StringGetter(RUNTIME_THRESHOLD)
		public int getRuntimeThreshold() {
			return runtimeThreshold;
		}

		@StringSetter(RUNTIME_THRESHOLD)
		public void setRuntimeThreshold(int value) {
			this.runtimeThreshold = value;
		}

		@Override
		public Map<String, String> getComments() {
			Map<String, String> comments = super.getComments();
			comments.put(RUNTIME_THRESHOLD, RUNTIME_THRESHOLD_COMMENT);
			return comments;
		}
	}

	public static class GlpkMpsRelocationParameters extends RelocationSolverParameters {
		public GlpkMpsRelocationParameters() {
			super(GlpkMpsRelocationSolver.TYPE);
		}
	}

	public static class CbcMpsRelocationParameters extends RelocationSolverParameters {
		public CbcMpsRelocationParameters() {
			super(CbcMpsRelocationSolver.TYPE);
		}
	}

	public static class TravelTimeEstimatorParameters extends ReflectiveConfigGroup {
		static public final String SET_PREFIX = "travelTimeEstimator:";

		private final String estimatorType;

		public TravelTimeEstimatorParameters(String estimatorType) {
			super(SET_PREFIX + estimatorType);
			this.estimatorType = estimatorType;
		}

		public String getEstimatorType() {
			return estimatorType;
		}
	}

	private final Map<String, Supplier<AssignmentSolverParameters>> availableAssignmentSolvers = new HashMap<>();
	private final Map<String, Supplier<RelocationSolverParameters>> availableRelocationSolvers = new HashMap<>();
	private final Map<String, Supplier<TravelTimeEstimatorParameters>> availableTravelTimeEstimators = new HashMap<>();

	private void prepareAvailableComponents() {
		availableAssignmentSolvers.put(BestResponseAssignmentSolver.TYPE, () -> new BestResponseAssignmentParameters());
		availableAssignmentSolvers.put(CbcMpsAssignmentSolver.TYPE, () -> new CbcMpsAssignmentParameters());
		availableAssignmentSolvers.put(GlpkMpsAssignmentSolver.TYPE, () -> new GlpkMpsAssignmentParameters());

		availableRelocationSolvers.put(BestResponseRelocationSolver.TYPE, () -> new BestResponseRelocationParameters());
		availableRelocationSolvers.put(CbcMpsRelocationSolver.TYPE, () -> new CbcMpsRelocationParameters());
		availableRelocationSolvers.put(GlpkMpsRelocationSolver.TYPE, () -> new GlpkMpsRelocationParameters());

		availableTravelTimeEstimators.put(DrtDetourTravelTimeEstimator.TYPE, () -> new DrtDetourEstimatorParameters());
		availableTravelTimeEstimators.put(EuclideanTravelTimeEstimator.TYPE, () -> new EuclideanEstimatorParameters());
		availableTravelTimeEstimators.put(RoutingTravelTimeEstimator.TYPE, () -> new RoutingEstimatorParameters());
		availableTravelTimeEstimators.put(HybridTravelTimeEstimator.TYPE, () -> new HybridEstimatorParameters());
		availableTravelTimeEstimators.put(MatrixTravelTimeEstimator.TYPE, () -> new MatrixEstimatorParameters());
	}

	private void prepareDefaultComponents() {
		super.addParameterSet(new BestResponseAssignmentParameters());
		super.addParameterSet(new BestResponseRelocationParameters());
		super.addParameterSet(new EuclideanEstimatorParameters());
		super.addParameterSet(new CongestionMitigationParameters());
	}

	public void addAssignmentSolverDefinition(String solverType, Supplier<AssignmentSolverParameters> creator) {
		availableAssignmentSolvers.put(solverType, creator);
	}

	public void addRelocationSolverDefinition(String solverType, Supplier<RelocationSolverParameters> creator) {
		availableRelocationSolvers.put(solverType, creator);
	}

	public void addTravelTimeEstimatorDefinition(String estimatorType,
			Supplier<TravelTimeEstimatorParameters> creator) {
		availableTravelTimeEstimators.put(estimatorType, creator);
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		if (type.startsWith(AssignmentSolverParameters.SET_PREFIX)) {
			String solverType = type.replaceFirst(AssignmentSolverParameters.SET_PREFIX, "");

			Verify.verify(availableAssignmentSolvers.containsKey(solverType),
					"Assignment solver of type " + solverType + " is not registered.");
			return availableAssignmentSolvers.get(solverType).get();
		}

		if (type.startsWith(RelocationSolverParameters.SET_PREFIX)) {
			String solverType = type.replaceFirst(RelocationSolverParameters.SET_PREFIX, "");

			Verify.verify(availableRelocationSolvers.containsKey(solverType),
					"Relocation solver of type " + solverType + " is not registered.");
			return availableRelocationSolvers.get(solverType).get();
		}

		if (type.startsWith(TravelTimeEstimatorParameters.SET_PREFIX)) {
			String estimatorType = type.replaceFirst(TravelTimeEstimatorParameters.SET_PREFIX, "");

			Verify.verify(availableTravelTimeEstimators.containsKey(estimatorType),
					"Travel time estimator of type " + estimatorType + " is not registered.");
			return availableTravelTimeEstimators.get(estimatorType).get();
		}

		if (type.equals(CongestionMitigationParameters.SET_NAME)) {
			return new CongestionMitigationParameters();
		}

		throw new IllegalStateException("Invalid parameter set for the Alonso-Mora config group: " + type);
	}

	@Override
	public void addParameterSet(ConfigGroup set) {
		if (set instanceof CongestionMitigationParameters) {
			removeParameterSet(getCongestionMitigationParameters());
		}

		if (set instanceof AssignmentSolverParameters) {
			removeParameterSet(getAssignmentSolverParameters());
		}

		if (set instanceof RelocationSolverParameters) {
			removeParameterSet(getRelocationSolverParameters());
		}

		if (set instanceof TravelTimeEstimatorParameters) {
			removeParameterSet(getTravelTimeEstimatorParameters());
		}

		super.addParameterSet(set);
	}

	/* Assignment parameters */

	public static class BestResponseAssignmentParameters extends AssignmentSolverParameters {
		public BestResponseAssignmentParameters() {
			super(BestResponseAssignmentSolver.TYPE);
		}
	}

	public static class GlpkMpsAssignmentParameters extends AssignmentSolverParameters {
		public GlpkMpsAssignmentParameters() {
			super(GlpkMpsAssignmentSolver.TYPE);
		}
	}

	public static class CbcMpsAssignmentParameters extends AssignmentSolverParameters {
		public CbcMpsAssignmentParameters() {
			super(CbcMpsAssignmentSolver.TYPE);
		}
	}

	/* Relocation parameters */

	public static class BestResponseRelocationParameters extends RelocationSolverParameters {
		public BestResponseRelocationParameters() {
			super(BestResponseRelocationSolver.TYPE);
		}
	}

	/* Travel time estimator parameters */

	public static class EuclideanEstimatorParameters extends TravelTimeEstimatorParameters {
		public EuclideanEstimatorParameters() {
			super(EuclideanTravelTimeEstimator.TYPE);
		}

		protected EuclideanEstimatorParameters(String estimatorType) {
			super(estimatorType);
		}

		private final static String EUCLIDEAN_DISTANCE_FACTOR = "euclideanDistanceFactor";
		private final static String EUCLIDEAN_DISTANCE_FACTOR_COMMENT = "Factor added to the Euclidean distance to estimate the travel time.";

		private double euclideanDistanceFactor = 1.3;

		@StringGetter(EUCLIDEAN_DISTANCE_FACTOR)
		public double getEuclideanDistanceFactor() {
			return euclideanDistanceFactor;
		}

		@StringSetter(EUCLIDEAN_DISTANCE_FACTOR)
		public void setEuclideanDistanceFactor(double value) {
			this.euclideanDistanceFactor = value;
		}

		private final static String EUCLIDEAN_SPEED = "euclideanSpeed";
		private final static String EUCLIDEAN_SPEED_COMMENT = "Speed along the scaled crofly distance in [km/h]";

		private double euclideanSpeed = 40.0;

		@StringGetter(EUCLIDEAN_SPEED)
		public double getEuclideanSpeed() {
			return euclideanSpeed;
		}

		@StringSetter(EUCLIDEAN_SPEED)
		public void setEuclideanSpeed(double value) {
			this.euclideanSpeed = value;
		}

		@Override
		public Map<String, String> getComments() {
			Map<String, String> comments = super.getComments();
			comments.put(EUCLIDEAN_DISTANCE_FACTOR, EUCLIDEAN_DISTANCE_FACTOR_COMMENT);
			comments.put(EUCLIDEAN_SPEED, EUCLIDEAN_SPEED_COMMENT);
			return comments;
		}
	}

	public static class RoutingEstimatorParameters extends TravelTimeEstimatorParameters {
		public RoutingEstimatorParameters() {
			super(RoutingTravelTimeEstimator.TYPE);
		}

		private final static String CACHE_LIFETIME = "cacheLifetime";
		private final static String CACHE_LIFETIME_COMMENT = "Delay until which a specific OD pair needs to be rerouted again";

		private double cacheLifetime = 1200.0;

		@StringGetter(CACHE_LIFETIME)
		public double getCacheLifetime() {
			return cacheLifetime;
		}

		@StringSetter(CACHE_LIFETIME)
		public void setCacheLifetime(double value) {
			this.cacheLifetime = value;
		}

		@Override
		public Map<String, String> getComments() {
			Map<String, String> comments = super.getComments();
			comments.put(CACHE_LIFETIME, CACHE_LIFETIME_COMMENT);
			return comments;
		}
	}

	public static class HybridEstimatorParameters extends EuclideanEstimatorParameters {
		public HybridEstimatorParameters() {
			super(HybridTravelTimeEstimator.TYPE);
		}

		private final static String CACHE_LIFETIME = "cacheLifetime";
		private final static String CACHE_LIFETIME_COMMENT = "Delay until which a specific OD pair needs to be rerouted again";

		private double cacheLifetime = 1200.0;

		@StringGetter(CACHE_LIFETIME)
		public double getCacheLifetime() {
			return cacheLifetime;
		}

		@StringSetter(CACHE_LIFETIME)
		public void setCacheLifetime(double value) {
			this.cacheLifetime = value;
		}

		@Override
		public Map<String, String> getComments() {
			Map<String, String> comments = super.getComments();
			comments.put(CACHE_LIFETIME, CACHE_LIFETIME_COMMENT);
			return comments;
		}
	}

	public static class DrtDetourEstimatorParameters extends TravelTimeEstimatorParameters {
		public DrtDetourEstimatorParameters() {
			super(DrtDetourTravelTimeEstimator.TYPE);
		}
	}

	public static class MatrixEstimatorParameters extends TravelTimeEstimatorParameters {
		public MatrixEstimatorParameters() {
			super(MatrixTravelTimeEstimator.TYPE);
		}

		private final static String LAZY = "lazy";
		private final static String LAZY_COMMENT = "Defines whether the travel time matrix is constructed step by step when routes get requested or all at once in the beginning";

		private boolean lazy = false;

		@StringGetter(LAZY)
		public boolean isLazy() {
			return lazy;
		}

		@StringSetter(LAZY)
		public void setLazy(boolean value) {
			this.lazy = value;
		}

		@Override
		public Map<String, String> getComments() {
			Map<String, String> comments = super.getComments();
			comments.put(LAZY, LAZY_COMMENT);
			return comments;
		}
	}

	/* Some convenience getters */

	public AssignmentSolverParameters getAssignmentSolverParameters() {
		List<? extends AssignmentSolverParameters> assignmentSolvers = getParameterSets() //
				.values().stream() //
				.flatMap(collection -> collection.stream()) //
				.filter(s -> s instanceof AssignmentSolverParameters) //
				.map(s -> (AssignmentSolverParameters) s) //
				.collect(Collectors.toList());

		Verify.verify(assignmentSolvers.size() > 0, "Exactly one assignment solver must be defined (currently none)");
		Verify.verify(assignmentSolvers.size() < 2,
				"Exactly one assignment solver must be defined (currently multiple)");

		return assignmentSolvers.get(0);
	}

	public RelocationSolverParameters getRelocationSolverParameters() {
		List<? extends RelocationSolverParameters> relocationSolvers = getParameterSets() //
				.values().stream() //
				.flatMap(collection -> collection.stream()) //
				.filter(s -> s instanceof RelocationSolverParameters) //
				.map(s -> (RelocationSolverParameters) s) //
				.collect(Collectors.toList());

		Verify.verify(relocationSolvers.size() > 0, "Exactly one relocation solver must be defined (currently none)");
		Verify.verify(relocationSolvers.size() < 2,
				"Exactly one relocation solver must be defined (currently multiple)");

		return relocationSolvers.get(0);
	}

	public TravelTimeEstimatorParameters getTravelTimeEstimatorParameters() {
		List<? extends TravelTimeEstimatorParameters> estimators = getParameterSets() //
				.values().stream() //
				.flatMap(collection -> collection.stream()) //
				.filter(s -> s instanceof TravelTimeEstimatorParameters) //
				.map(s -> (TravelTimeEstimatorParameters) s) //
				.collect(Collectors.toList());

		Verify.verify(estimators.size() > 0, "Exactly one travel time estimator must be defined (currently none)");
		Verify.verify(estimators.size() < 2, "Exactly one travel time estimator must be defined (currently multiple)");

		return estimators.get(0);
	}

	public CongestionMitigationParameters getCongestionMitigationParameters() {
		List<? extends CongestionMitigationParameters> items = getParameterSets() //
				.values().stream() //
				.flatMap(collection -> collection.stream()) //
				.filter(s -> s instanceof CongestionMitigationParameters) //
				.map(s -> (CongestionMitigationParameters) s) //
				.collect(Collectors.toList());

		Verify.verify(items.size() > 0, "Exactly one congestion parameter set must be defined (currently none)");
		Verify.verify(items.size() < 2, "Exactly one congestion parameter set must be defined (currently multiple)");

		return items.get(0);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		Verify.verify(relocationInterval % assignmentInterval == 0,
				"Relocation interval must be multiple of the assignment interval");
		Verify.verify(loggingInterval % assignmentInterval == 0,
				"Logging interval must be multiple of the assignment interval");

		if (plannedPickupTimeSlack > 0.0) {
			Verify.verify(!usePlannedPickupTime,
					"Non-zero value for plannedPickupTimeSlack has no effect if usePlannedPickupTime is false");
		}

		getAssignmentSolverParameters();
		getRelocationSolverParameters();
		getTravelTimeEstimatorParameters();
		getCongestionMitigationParameters();

		boolean foundDrt = false;

		for (DrtConfigGroup drtModeConfig : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			if (drtModeConfig.getMode().equals(getMode())) {
				foundDrt = true;

				if (drtModeConfig.getRebalancingParams().isPresent()) {
					Verify.verify(relocationInterval == 0,
							"If DRT rebalancing is enabled, relocationInterval should be zero (disabling Alonso-Mora relocation)");
				}
			}
		}

		Verify.verify(foundDrt, "Mode {} was defined for Alonso-Mora, but does not exist in DRT", getMode());
	}
}
