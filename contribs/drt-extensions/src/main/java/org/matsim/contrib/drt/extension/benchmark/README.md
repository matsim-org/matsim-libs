# DRT Scalability Benchmark

This module provides a comprehensive benchmark suite for testing DRT (Demand Responsive Transport) insertion search strategies with different request inserter types and partitioner strategies.

## Overview

The benchmark has **three orthogonal dimensions**:

1. **Request Inserter Type** – HOW requests are dispatched to vehicles:
   - `Default` – sequential processing via `DefaultUnplannedRequestInserter`
   - `Parallel` – partitioned parallel processing via `ParallelUnplannedRequestInserter`

2. **Insertion Search Strategy** – WHICH algorithm finds the best insertion:
   - `Selective` – fast heuristic (single best insertion per request)
   - `Extensive` – evaluates all feasible insertions
   - `RepeatedSelective` – retries selective search multiple times

3. **Detour Path Calculator Type** – WHICH routing algorithm computes detour paths:
   - `SpeedyALT` – default Dijkstra-based one-to-many router (good for small/medium networks)
   - `CH` – Contraction Hierarchies (dramatically faster on large networks, one-time build cost)

**Every combination is valid** (e.g., Default + Extensive + CH, Parallel + Selective + SpeedyALT, etc.). The benchmark runs the cross-product of all configured dimensions × agent counts, measuring execution time and capturing quality metrics.

## Quick Start

A sample configuration file is included in the resources: `src/main/resources/benchmark-config.xml`

```bash
# Copy the sample config to your working directory
cp src/main/resources/benchmark-config.xml my-benchmark-config.xml

# Run with default configuration
java -cp matsim.jar org.matsim.contrib.drt.extension.benchmark.RunScalabilityBenchmark \
    --config-path my-benchmark-config.xml

# Override parameters via command line
java -cp matsim.jar org.matsim.contrib.drt.extension.benchmark.RunScalabilityBenchmark \
    --config-path my-benchmark-config.xml \
    --config:drtBenchmark.requestInserterTypes Default,Parallel \
    --config:drtBenchmark.insertionSearchStrategies Selective,Extensive \
    --config:drtBenchmark.detourPathCalculatorTypes SpeedyALT,CH \
    --config:drtBenchmark.agentCounts 50000,100000,200000 \
    --config:drtBenchmark.maxPartitions 16
```

## Configuration

The benchmark uses the standard MATSim configuration system. Create a config XML file with a `drtBenchmark` module:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE config SYSTEM "http://www.matsim.org/files/dtd/config_v2.dtd">
<config>
    <module name="drtBenchmark">
        <!-- Agent & Vehicle Configuration -->
        <param name="agentCounts" value="50000,100000"/>
        <param name="vehiclesPerHundredAgents" value="1"/>

        <!-- Request Inserter & Insertion Search Configuration -->
        <param name="requestInserterTypes" value="Default,Parallel"/>
        <param name="insertionSearchStrategies" value="Selective,Extensive"/>
        <param name="detourPathCalculatorTypes" value="SpeedyALT,CH"/>

        <!-- Partitioner Configuration (only for Parallel inserter type) -->
        <param name="vehiclePartitioners" value="ShiftingRoundRobin"/>
        <param name="requestPartitioners" value="LoadAware"/>

        <!-- Parallel Inserter Settings (only for Parallel inserter type) -->
        <param name="collectionPeriods" value="90"/>
        <param name="maxPartitions" value="8"/>
        <param name="maxIterations" value="3"/>
        <param name="logPerformanceStats" value="true"/>

        <!-- Benchmark Settings -->
        <param name="warmupRuns" value="0"/>
        <param name="measuredRuns" value="1"/>

        <!-- Output Settings -->
        <param name="outputDirectory" value="output/benchmark"/>
        <param name="useSpatialFilter" value="true"/>
    </module>
</config>
```

### Configuration Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `agentCounts` | `50000,100000` | Comma-separated list of agent counts to test |
| `vehiclesPerHundredAgents` | `1` | Number of vehicles per 100 agents |
| `requestInserterTypes` | `Default` | Request inserter types to benchmark (see below) |
| `insertionSearchStrategies` | `Selective` | Insertion search strategies to benchmark (see below) |
| `detourPathCalculatorTypes` | `SpeedyALT` | Detour path calculator types to benchmark (see below) |
| `vehiclePartitioners` | `Replicating,RoundRobin,ShiftingRoundRobin` | Vehicle partitioners (**Parallel only**) |
| `requestPartitioners` | `RoundRobin,LoadAware` | Request partitioners (**Parallel only**) |
| `collectionPeriods` | `90` | Collection periods in seconds (**Parallel only**) |
| `maxPartitions` | `8` | Number of parallel partitions (**Parallel only**, typically = CPU cores) |
| `maxIterations` | `3` | Max iterations for optimization (**Parallel only**) |
| `warmupRuns` | `0` | Number of warmup runs (JIT warmup) |
| `measuredRuns` | `1` | Number of measured runs |
| `outputDirectory` | `output/benchmark` | Output directory |
| `useSpatialFilter` | `true` | Enable spatial request-fleet filter |
| `logPerformanceStats` | `true` | Log detailed performance statistics |

## Request Inserter Types

| Type | Description |
|------|-------------|
| `Default` | Sequential request processing. Uses the standard `DefaultUnplannedRequestInserter`. Each request is inserted one at a time. |
| `Parallel` | Partitioned parallel processing via `ParallelUnplannedRequestInserter`. Requests and vehicles are split across partitions. Requires partitioner and collection period configuration. |

## Insertion Search Strategies

| Strategy | Description |
|----------|-------------|
| `Selective` | Fast heuristic – finds a single best insertion per request using beeline-distance pre-filtering. Good baseline. |
| `Extensive` | Evaluates all feasible insertions for each request. Higher quality but slower. |
| `RepeatedSelective` | Retries the selective search multiple times. Balances speed and quality. |

> **Key insight**: The search strategy and the inserter type are orthogonal. The Parallel inserter internally uses the same search strategy – it just distributes the work across partitions. This means you can compare e.g. `Default + Selective` vs. `Parallel + Selective` to measure the parallelization speedup, or `Default + Selective` vs. `Default + Extensive` to compare search algorithm quality.

## Partitioner Strategies

### Vehicle Partitioners

| Name | Short | Description |
|------|-------|-------------|
| `ReplicatingVehicleEntryPartitioner` | `Repl` | Each partition receives all vehicles. Best quality, highest memory usage. |
| `RoundRobinVehicleEntryPartitioner` | `RR` | Vehicles distributed round-robin across partitions. |
| `ShiftingRoundRobinVehicleEntryPartitioner` | `ShiftRR` | Like RoundRobin but shifts assignment each iteration for better coverage. |

### Request Partitioners

| Name | Short | Description |
|------|-------|-------------|
| `RoundRobinRequestsPartitioner` | `RR` | Requests distributed round-robin. |
| `LoadAwareRoundRobinRequestsPartitioner` | `LoadAware` | Distributes based on partition load. Slightly slower but often better quality. |

## Output

### CSV Report

Results are written incrementally to a timestamped CSV file (`benchmark_YYYYMMDD_HHMMSS.csv`) with the following columns:

| Column | Description |
|--------|-------------|
| `name` | Scenario name (e.g., `Default_Selective_50k` or `Parallel_Selective_ShiftRR_LoadAware_cp90_50k`) |
| `iterations` | Number of measured iterations |
| `min_ms` | Minimum execution time (ms) |
| `max_ms` | Maximum execution time (ms) |
| `avg_ms` | Average execution time (ms) |
| `stddev_ms` | Standard deviation (ms) |
| `rides` | Number of served rides |
| `rejections` | Number of rejected requests |
| `rejection_rate` | Rejection rate (0.0 - 1.0) |
| `wait_avg_s` | Average wait time (seconds) |
| `wait_p95_s` | 95th percentile wait time (seconds) |
| `in_vehicle_time_s` | Mean in-vehicle travel time (seconds) |
| `total_travel_time_s` | Mean total travel time (seconds) |

### Scenario Naming Convention

Scenario names depend on the inserter type:

- **Default inserter**: `Default_{SearchStrategy}_{AgentCount}`
  - `Default_Selective_50k` – Default inserter, Selective search, 50k agents
  - `Default_Extensive_100k` – Default inserter, Extensive search, 100k agents
- **Parallel inserter**: `Parallel_{SearchStrategy}_{VehiclePartitioner}_{RequestPartitioner}_cp{CollectionPeriod}_{AgentCount}`
  - `Parallel_Selective_ShiftRR_LoadAware_cp90_50k` – Parallel inserter, Selective search, ShiftRR vehicles, LoadAware requests, 90s collection, 50k agents

## Quality Metrics

The benchmark captures DRT quality metrics from `drt_customer_stats_drt.csv`:

- **Rejection Rate**: Primary quality indicator. Lower is better.
- **Wait Time (avg/p95)**: How long passengers wait for pickup.
- **In-Vehicle Time**: Time spent in the vehicle.
- **Total Travel Time**: Complete journey time.

> **Note**: The rejection rate is a critical quality metric for comparing strategies. A faster configuration with a significantly higher rejection rate may not be preferable.

## Example Results

```csv
name,iterations,min_ms,max_ms,avg_ms,stddev_ms,rides,rejections,rejection_rate,wait_avg_s,wait_p95_s,in_vehicle_time_s,total_travel_time_s
Default_Selective_50k,1,95000,95000,95000,0,42100,7900,0.1580,8500,17200,690,9190
Default_Extensive_50k,1,180000,180000,180000,0,43500,6500,0.1300,7800,15900,685,8485
Parallel_Selective_ShiftRR_LoadAware_cp90_50k,1,45000,45000,45000,0,41025,8975,0.1800,9712,19520,708,10420
Parallel_Extensive_ShiftRR_LoadAware_cp90_50k,1,72000,72000,72000,0,42800,7200,0.1440,8900,17800,695,9595
```

## Module Structure

```
drt-extensions/
├── src/main/resources/
│   └── benchmark-config.xml         # Sample configuration (copy & customize)
└── src/main/java/.../benchmark/
    ├── README.md                    # This file
    ├── RunScalabilityBenchmark.java # Main entry point
    ├── DrtBenchmarkConfigGroup.java # MATSim config group
    ├── InsertionStrategy.java       # Enums: RequestInserterType, InsertionSearchStrategy
    ├── DrtBenchmarkRunner.java      # Benchmark execution engine
    ├── BenchmarkResult.java         # Result record with quality stats
    └── scenario/
        ├── SyntheticBenchmarkScenario.java  # Scenario builder
        ├── GridNetworkGenerator.java        # Grid network generation
        ├── PopulationGenerator.java         # Population generation
        └── FleetGenerator.java              # Fleet generation
```

## Tips

1. **Start Small**: Begin with fewer agents (e.g., 10000) to verify the setup works.
2. **Compare Inserter Types**: Use `requestInserterTypes=Default,Parallel` with the same search strategy to measure parallelization speedup.
3. **Compare Search Strategies**: Use `insertionSearchStrategies=Selective,Extensive` with the same inserter to compare algorithm quality.
4. **Full Cross-Product**: Combine both dimensions to get a complete performance × quality picture.
5. **Match Partitions to CPUs**: Set `maxPartitions` to your CPU core count for optimal performance.
6. **Consider Memory**: `Replicating` partitioner uses more memory but typically achieves the best quality.
7. **Monitor Rejection Rate**: The fastest configuration isn't always the best - check rejection rates.
8. **Multiple Runs**: For production benchmarks, use `measuredRuns > 1` and `warmupRuns >= 1` for more reliable results.

## Command Line Overrides

All parameters can be overridden via command line using the MATSim convention:

```bash
--config:drtBenchmark.requestInserterTypes Default,Parallel
--config:drtBenchmark.insertionSearchStrategies Selective,Extensive,RepeatedSelective
--config:drtBenchmark.agentCounts 50000,100000,200000
--config:drtBenchmark.maxPartitions 16
--config:drtBenchmark.vehiclePartitioners Replicating,RoundRobin
--config:drtBenchmark.outputDirectory output/my-benchmark
```

## Author

Steffen Axer

## License

GNU General Public License v2.0 (see LICENSE file in the repository root)



