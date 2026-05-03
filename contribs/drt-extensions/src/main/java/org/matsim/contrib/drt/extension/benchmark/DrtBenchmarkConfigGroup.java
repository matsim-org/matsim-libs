/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** */

package org.matsim.contrib.drt.extension.benchmark;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.contrib.drt.optimizer.insertion.parallel.DrtParallelInserterParams.RequestsPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.DrtParallelInserterParams.VehiclesPartitioner;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.groups.ControllerConfigGroup;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MATSim configuration group for DRT Scalability Benchmark.
 * <p>
 * Can be loaded from a standard MATSim config XML file:
 * <pre>{@code
 * <module name="drtBenchmark">
 *     <param name="agentCounts" value="50000,100000"/>
 *     <param name="vehiclesPerHundredAgents" value="1"/>
 *     <param name="requestInserterTypes" value="Default,Parallel"/>
 *     <param name="insertionSearchStrategies" value="Selective,Extensive"/>
 *     <param name="vehiclePartitioners" value="Replicating,RoundRobin"/>
 *     <param name="requestPartitioners" value="RoundRobin,LoadAware"/>
 *     <param name="collectionPeriods" value="90"/>
 *     <param name="maxPartitions" value="8"/>
 *     <param name="maxIterations" value="3"/>
 *     <param name="warmupRuns" value="0"/>
 *     <param name="measuredRuns" value="1"/>
 *     <param name="outputDirectory" value="output/benchmark"/>
 *     <param name="useSpatialFilter" value="true"/>
 * </module>
 * }</pre>
 *
 * @author Steffen Axer
 */
public class DrtBenchmarkConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "drtBenchmark";

	public DrtBenchmarkConfigGroup() {
		super(GROUP_NAME);
	}

	/**
	 * Gets the DrtBenchmarkConfigGroup from a Config, or creates a new one if not present.
	 */
	public static DrtBenchmarkConfigGroup get(Config config) {
		ConfigGroup group = config.getModule(GROUP_NAME);
		if (group instanceof DrtBenchmarkConfigGroup) {
			return (DrtBenchmarkConfigGroup) group;
		}
		// Create and add if not exists
		DrtBenchmarkConfigGroup benchmarkConfig = new DrtBenchmarkConfigGroup();
		config.addModule(benchmarkConfig);
		return benchmarkConfig;
	}

	// =========================================================================
	// Agent & Vehicle Configuration
	// =========================================================================

	private static final String AGENT_COUNTS = "agentCounts";
	private static final String VEHICLES_PER_HUNDRED_AGENTS = "vehiclesPerHundredAgents";

	@Comment("Comma-separated list of agent counts to test (e.g., '50000,100000,200000')")
	private String agentCounts = "50000,100000";

	@Comment("Number of vehicles per 100 agents. With 50000 agents and value 1, you get 500 vehicles.")
	@Positive
	private int vehiclesPerHundredAgents = 1;

	@StringGetter(AGENT_COUNTS)
	public String getAgentCountsString() {
		return agentCounts;
	}

	@StringSetter(AGENT_COUNTS)
	public void setAgentCountsString(String agentCounts) {
		this.agentCounts = agentCounts;
	}

	public List<Integer> getAgentCounts() {
		return parseIntList(agentCounts);
	}

	public void setAgentCounts(List<Integer> counts) {
		this.agentCounts = counts.stream().map(String::valueOf).collect(Collectors.joining(","));
	}

	@StringGetter(VEHICLES_PER_HUNDRED_AGENTS)
	public int getVehiclesPerHundredAgents() {
		return vehiclesPerHundredAgents;
	}

	@StringSetter(VEHICLES_PER_HUNDRED_AGENTS)
	public void setVehiclesPerHundredAgents(int vehiclesPerHundredAgents) {
		this.vehiclesPerHundredAgents = vehiclesPerHundredAgents;
	}

	// =========================================================================
	// Request Inserter & Insertion Search Configuration
	// =========================================================================

	private static final String REQUEST_INSERTER_TYPES = "requestInserterTypes";
	private static final String INSERTION_SEARCH_STRATEGIES = "insertionSearchStrategies";

	@Comment("Comma-separated list of request inserter types to benchmark. " +
		"Options: Default, Parallel. " +
		"'Default' uses the standard sequential DefaultUnplannedRequestInserter. " +
		"'Parallel' uses the partitioned ParallelUnplannedRequestInserter (requires partitioner settings).")
	@NotBlank
	private String requestInserterTypes = "Default";

	@Comment("Comma-separated list of insertion search strategies to benchmark. " +
		"Options: Selective, Extensive, RepeatedSelective. " +
		"Each search strategy is combined with each inserter type (cross-product).")
	@NotBlank
	private String insertionSearchStrategies = "Selective";

	@StringGetter(REQUEST_INSERTER_TYPES)
	public String getRequestInserterTypesString() {
		return requestInserterTypes;
	}

	@StringSetter(REQUEST_INSERTER_TYPES)
	public void setRequestInserterTypesString(String requestInserterTypes) {
		this.requestInserterTypes = requestInserterTypes;
	}

	public List<InsertionStrategy.RequestInserterType> getRequestInserterTypes() {
		return Arrays.stream(requestInserterTypes.split(","))
			.map(String::trim)
			.map(this::parseRequestInserterType)
			.collect(Collectors.toList());
	}

	public void setRequestInserterTypes(List<InsertionStrategy.RequestInserterType> types) {
		this.requestInserterTypes = types.stream()
			.map(Enum::name)
			.collect(Collectors.joining(","));
	}

	@StringGetter(INSERTION_SEARCH_STRATEGIES)
	public String getInsertionSearchStrategiesString() {
		return insertionSearchStrategies;
	}

	@StringSetter(INSERTION_SEARCH_STRATEGIES)
	public void setInsertionSearchStrategiesString(String insertionSearchStrategies) {
		this.insertionSearchStrategies = insertionSearchStrategies;
	}

	public List<InsertionStrategy.InsertionSearchStrategy> getInsertionSearchStrategies() {
		return Arrays.stream(insertionSearchStrategies.split(","))
			.map(String::trim)
			.map(this::parseInsertionSearchStrategy)
			.collect(Collectors.toList());
	}

	public void setInsertionSearchStrategies(List<InsertionStrategy.InsertionSearchStrategy> strategies) {
		this.insertionSearchStrategies = strategies.stream()
			.map(Enum::name)
			.collect(Collectors.joining(","));
	}

	// =========================================================================
	// Partitioner Configuration (only applies to Parallel strategy)
	// =========================================================================

	private static final String VEHICLE_PARTITIONERS = "vehiclePartitioners";
	private static final String REQUEST_PARTITIONERS = "requestPartitioners";

	@Comment("Comma-separated list of vehicle partitioners to test. " +
		"Options: Replicating, RoundRobin, ShiftingRoundRobin (or short: Repl, RR, ShiftRR)")
	@NotBlank
	private String vehiclePartitioners = "Replicating,RoundRobin,ShiftingRoundRobin";

	@Comment("Comma-separated list of request partitioners to test. " +
		"Options: RoundRobin, LoadAware (or short: RR)")
	@NotBlank
	private String requestPartitioners = "RoundRobin,LoadAware";

	@StringGetter(VEHICLE_PARTITIONERS)
	public String getVehiclePartitionersString() {
		return vehiclePartitioners;
	}

	@StringSetter(VEHICLE_PARTITIONERS)
	public void setVehiclePartitionersString(String vehiclePartitioners) {
		this.vehiclePartitioners = vehiclePartitioners;
	}

	public List<VehiclesPartitioner> getVehiclePartitioners() {
		return Arrays.stream(vehiclePartitioners.split(","))
			.map(String::trim)
			.map(this::parseVehiclePartitioner)
			.collect(Collectors.toList());
	}

	public void setVehiclePartitioners(List<VehiclesPartitioner> partitioners) {
		this.vehiclePartitioners = partitioners.stream()
			.map(this::vehiclePartitionerToString)
			.collect(Collectors.joining(","));
	}

	@StringGetter(REQUEST_PARTITIONERS)
	public String getRequestPartitionersString() {
		return requestPartitioners;
	}

	@StringSetter(REQUEST_PARTITIONERS)
	public void setRequestPartitionersString(String requestPartitioners) {
		this.requestPartitioners = requestPartitioners;
	}

	public List<RequestsPartitioner> getRequestPartitioners() {
		return Arrays.stream(requestPartitioners.split(","))
			.map(String::trim)
			.map(this::parseRequestPartitioner)
			.collect(Collectors.toList());
	}

	public void setRequestPartitioners(List<RequestsPartitioner> partitioners) {
		this.requestPartitioners = partitioners.stream()
			.map(this::requestPartitionerToString)
			.collect(Collectors.joining(","));
	}

	// =========================================================================
	// Parallel Inserter Settings
	// =========================================================================

	private static final String COLLECTION_PERIODS = "collectionPeriods";
	private static final String MAX_PARTITIONS = "maxPartitions";
	private static final String MAX_ITERATIONS = "maxIterations";
	private static final String LOG_PERFORMANCE_STATS = "logPerformanceStats";
	private static final String MATRIX_CELL_SIZE = "matrixCellSize";

	@Comment("Comma-separated list of collection periods in seconds to test (e.g., '60,90,120')")
	private String collectionPeriods = "90";

	@Comment("Number of parallel partitions (typically = number of CPU cores)")
	@Positive
	private int maxPartitions = 8;

	@Comment("Maximum iterations for the parallel inserter optimization")
	@Positive
	private int maxIterations = 3;

	@Comment("Log detailed performance statistics for each partition")
	private boolean logPerformanceStats = true;

	@Comment("Cell size [m] for the DVRP travel-time matrix square-grid zone system. " +
		"Larger values reduce matrix zones/memory/time; smaller values increase precision.")
	@Positive
	private double matrixCellSize = 200.0;

	@StringGetter(COLLECTION_PERIODS)
	public String getCollectionPeriodsString() {
		return collectionPeriods;
	}

	@StringSetter(COLLECTION_PERIODS)
	public void setCollectionPeriodsString(String collectionPeriods) {
		this.collectionPeriods = collectionPeriods;
	}

	public List<Integer> getCollectionPeriods() {
		return parseIntList(collectionPeriods);
	}

	public void setCollectionPeriods(List<Integer> periods) {
		this.collectionPeriods = periods.stream().map(String::valueOf).collect(Collectors.joining(","));
	}

	@StringGetter(MAX_PARTITIONS)
	public int getMaxPartitions() {
		return maxPartitions;
	}

	@StringSetter(MAX_PARTITIONS)
	public void setMaxPartitions(int maxPartitions) {
		this.maxPartitions = maxPartitions;
	}

	@StringGetter(MAX_ITERATIONS)
	public int getMaxIterations() {
		return maxIterations;
	}

	@StringSetter(MAX_ITERATIONS)
	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	@StringGetter(LOG_PERFORMANCE_STATS)
	public boolean isLogPerformanceStats() {
		return logPerformanceStats;
	}

	@StringSetter(LOG_PERFORMANCE_STATS)
	public void setLogPerformanceStats(boolean logPerformanceStats) {
		this.logPerformanceStats = logPerformanceStats;
	}

	@StringGetter(MATRIX_CELL_SIZE)
	public double getMatrixCellSize() {
		return matrixCellSize;
	}

	@StringSetter(MATRIX_CELL_SIZE)
	public void setMatrixCellSize(double matrixCellSize) {
		this.matrixCellSize = matrixCellSize;
	}

	// =========================================================================
	// Benchmark Settings
	// =========================================================================

	private static final String WARMUP_RUNS = "warmupRuns";
	private static final String MEASURED_RUNS = "measuredRuns";

	@Comment("Number of warmup runs (results discarded, JIT warmup)")
	@PositiveOrZero
	private int warmupRuns = 0;

	@Comment("Number of measured runs (results averaged)")
	@Positive
	private int measuredRuns = 1;

	@StringGetter(WARMUP_RUNS)
	public int getWarmupRuns() {
		return warmupRuns;
	}

	@StringSetter(WARMUP_RUNS)
	public void setWarmupRuns(int warmupRuns) {
		this.warmupRuns = warmupRuns;
	}

	@StringGetter(MEASURED_RUNS)
	public int getMeasuredRuns() {
		return measuredRuns;
	}

	@StringSetter(MEASURED_RUNS)
	public void setMeasuredRuns(int measuredRuns) {
		this.measuredRuns = measuredRuns;
	}

	// =========================================================================
	// Output Settings
	// =========================================================================

	private static final String OUTPUT_DIRECTORY = "outputDirectory";
	private static final String USE_SPATIAL_FILTER = "useSpatialFilter";
	private static final String DETOUR_PATH_CALCULATOR_TYPES = "detourPathCalculatorTypes";
	private static final String NETWORK_URL = "networkUrl";

	@Comment("Output directory for benchmark results (timestamp subfolder will be created)")
	@NotBlank
	private String outputDirectory = "output/benchmark";

	@Comment("Enable spatial request-fleet filter (reduces search space)")
	private boolean useSpatialFilter = true;

	@Comment("Comma-separated list of routing algorithm types for detour path computation. " +
		"Options: SpeedyALT, CHRouter. " +
		"Maps directly to config.controller.routingAlgorithmType.")
	@NotBlank
	private String detourPathCalculatorTypes = "SpeedyALT";

	@Comment("URL or file path to a MATSim network file (xml or xml.gz). " +
		"If empty, a synthetic grid network is generated. " +
		"Example: https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v7.0/input/berlin-v7.0-network.xml.gz")
	private String networkUrl = "";

	@StringGetter(OUTPUT_DIRECTORY)
	public String getOutputDirectory() {
		return outputDirectory;
	}

	@StringSetter(OUTPUT_DIRECTORY)
	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	@StringGetter(USE_SPATIAL_FILTER)
	public boolean isUseSpatialFilter() {
		return useSpatialFilter;
	}

	@StringSetter(USE_SPATIAL_FILTER)
	public void setUseSpatialFilter(boolean useSpatialFilter) {
		this.useSpatialFilter = useSpatialFilter;
	}

	@StringGetter(DETOUR_PATH_CALCULATOR_TYPES)
	public String getDetourPathCalculatorTypesString() {
		return detourPathCalculatorTypes;
	}

	@StringSetter(DETOUR_PATH_CALCULATOR_TYPES)
	public void setDetourPathCalculatorTypesString(String detourPathCalculatorTypes) {
		this.detourPathCalculatorTypes = detourPathCalculatorTypes;
	}

	public List<ControllerConfigGroup.RoutingAlgorithmType> getRoutingAlgorithmTypes() {
		return Arrays.stream(detourPathCalculatorTypes.split(","))
			.map(String::trim)
			.map(this::parseRoutingAlgorithmType)
			.collect(Collectors.toList());
	}

	public void setRoutingAlgorithmTypes(List<ControllerConfigGroup.RoutingAlgorithmType> types) {
		this.detourPathCalculatorTypes = types.stream()
			.map(Enum::name)
			.collect(Collectors.joining(","));
	}

	@StringGetter(NETWORK_URL)
	public String getNetworkUrl() {
		return networkUrl;
	}

	@StringSetter(NETWORK_URL)
	public void setNetworkUrl(String networkUrl) {
		this.networkUrl = networkUrl;
	}

	public boolean hasExternalNetwork() {
		return networkUrl != null && !networkUrl.isBlank();
	}

	// =========================================================================
	// Helper Methods
	// =========================================================================

	private List<Integer> parseIntList(String value) {
		return Arrays.stream(value.split(","))
			.map(String::trim)
			.map(s -> s.replace("_", ""))
			.map(Integer::parseInt)
			.collect(Collectors.toList());
	}

	private VehiclesPartitioner parseVehiclePartitioner(String name) {
		return switch (name.toLowerCase()) {
			case "replicating", "repl" -> VehiclesPartitioner.ReplicatingVehicleEntryPartitioner;
			case "roundrobin", "rr" -> VehiclesPartitioner.RoundRobinVehicleEntryPartitioner;
			case "shiftingroundrobin", "shiftrr" -> VehiclesPartitioner.ShiftingRoundRobinVehicleEntryPartitioner;
			default -> throw new IllegalArgumentException("Unknown vehicle partitioner: " + name +
				". Valid options: Replicating, RoundRobin, ShiftingRoundRobin");
		};
	}

	private String vehiclePartitionerToString(VehiclesPartitioner vp) {
		return switch (vp) {
			case ReplicatingVehicleEntryPartitioner -> "Replicating";
			case RoundRobinVehicleEntryPartitioner -> "RoundRobin";
			case ShiftingRoundRobinVehicleEntryPartitioner -> "ShiftingRoundRobin";
		};
	}

	private RequestsPartitioner parseRequestPartitioner(String name) {
		return switch (name.toLowerCase()) {
			case "roundrobin", "rr" -> RequestsPartitioner.RoundRobinRequestsPartitioner;
			case "loadaware", "loadawareroundrobin" -> RequestsPartitioner.LoadAwareRoundRobinRequestsPartitioner;
			default -> throw new IllegalArgumentException("Unknown request partitioner: " + name +
				". Valid options: RoundRobin, LoadAware");
		};
	}

	private String requestPartitionerToString(RequestsPartitioner rp) {
		return switch (rp) {
			case RoundRobinRequestsPartitioner -> "RoundRobin";
			case LoadAwareRoundRobinRequestsPartitioner -> "LoadAware";
		};
	}

	private InsertionStrategy.RequestInserterType parseRequestInserterType(String name) {
		return switch (name.toLowerCase()) {
			case "default" -> InsertionStrategy.RequestInserterType.Default;
			case "parallel" -> InsertionStrategy.RequestInserterType.Parallel;
			default -> throw new IllegalArgumentException("Unknown request inserter type: " + name +
				". Valid options: Default, Parallel");
		};
	}

	private InsertionStrategy.InsertionSearchStrategy parseInsertionSearchStrategy(String name) {
		return switch (name.toLowerCase()) {
			case "selective" -> InsertionStrategy.InsertionSearchStrategy.Selective;
			case "extensive" -> InsertionStrategy.InsertionSearchStrategy.Extensive;
			case "repeatedselective" -> InsertionStrategy.InsertionSearchStrategy.RepeatedSelective;
			default -> throw new IllegalArgumentException("Unknown insertion search strategy: " + name +
				". Valid options: Selective, Extensive, RepeatedSelective");
		};
	}

	private ControllerConfigGroup.RoutingAlgorithmType parseRoutingAlgorithmType(String name) {
		return switch (name.toLowerCase()) {
			case "speedyalt", "speedy" -> ControllerConfigGroup.RoutingAlgorithmType.SpeedyALT;
			case "ch", "chrouter" -> ControllerConfigGroup.RoutingAlgorithmType.CHRouter;
			default -> throw new IllegalArgumentException("Unknown detour path calculator type: " + name +
				". Valid options: SpeedyALT, CHRouter");
		};
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(AGENT_COUNTS, "Comma-separated list of agent counts to test (e.g., '50000,100000,200000')");
		comments.put(VEHICLES_PER_HUNDRED_AGENTS, "Number of vehicles per 100 agents");
		comments.put(REQUEST_INSERTER_TYPES, "Request inserter types: Default, Parallel");
		comments.put(INSERTION_SEARCH_STRATEGIES, "Insertion search strategies: Selective, Extensive, RepeatedSelective");
		comments.put(VEHICLE_PARTITIONERS, "Vehicle partitioners (Parallel inserter only): Replicating, RoundRobin, ShiftingRoundRobin");
		comments.put(REQUEST_PARTITIONERS, "Request partitioners (Parallel inserter only): RoundRobin, LoadAware");
		comments.put(COLLECTION_PERIODS, "Collection periods in seconds (Parallel inserter only, comma-separated)");
		comments.put(MAX_PARTITIONS, "Number of parallel partitions");
		comments.put(MAX_ITERATIONS, "Max iterations for parallel inserter");
		comments.put(MATRIX_CELL_SIZE, "Cell size [m] for DVRP TT matrix zones (larger = fewer zones)");
		comments.put(WARMUP_RUNS, "Number of warmup runs");
		comments.put(MEASURED_RUNS, "Number of measured runs");
		comments.put(OUTPUT_DIRECTORY, "Output directory for results");
		comments.put(USE_SPATIAL_FILTER, "Enable spatial filter");
		comments.put(DETOUR_PATH_CALCULATOR_TYPES, "Detour path calculator types: SpeedyALT, CHRouter");
		comments.put(NETWORK_URL, "URL or file path to a MATSim network (empty = synthetic grid)");
		return comments;
	}
}
