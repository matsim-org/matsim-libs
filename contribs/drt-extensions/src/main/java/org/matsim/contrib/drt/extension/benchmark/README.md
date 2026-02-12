# DRT Scalability Benchmark

This module provides a comprehensive benchmark suite for testing DRT (Demand Responsive Transport) parallel insertion algorithms with different partitioner strategies.

## Overview

The benchmark allows systematic comparison of different vehicle and request partitioning strategies in the DRT parallel inserter. It generates synthetic scenarios with configurable agent counts, measures execution time, and captures quality metrics like rejection rate, wait times, and travel times.

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

        <!-- Partitioner Configuration -->
        <param name="vehiclePartitioners" value="Replicating,RoundRobin,ShiftingRoundRobin"/>
        <param name="requestPartitioners" value="RoundRobin,LoadAware"/>

        <!-- Parallel Inserter Settings -->
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
| `vehiclePartitioners` | `Replicating,RoundRobin,ShiftingRoundRobin` | Vehicle partitioners to test |
| `requestPartitioners` | `RoundRobin,LoadAware` | Request partitioners to test |
| `collectionPeriods` | `90` | Collection periods in seconds |
| `maxPartitions` | `8` | Number of parallel partitions (typically = CPU cores) |
| `maxIterations` | `3` | Max iterations for optimization |
| `warmupRuns` | `0` | Number of warmup runs (JIT warmup) |
| `measuredRuns` | `1` | Number of measured runs |
| `outputDirectory` | `output/benchmark` | Output directory |
| `useSpatialFilter` | `true` | Enable spatial request-fleet filter |
| `logPerformanceStats` | `true` | Log detailed performance statistics |

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

Results are written incrementally to a timestamped CSV file (`partitioner_comparison_YYYYMMDD_HHMMSS.csv`) with the following columns:

| Column | Description |
|--------|-------------|
| `name` | Scenario name (e.g., `Repl_RR_cp90_50k`) |
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

Scenario names follow the pattern: `{VehiclePartitioner}_{RequestPartitioner}_cp{CollectionPeriod}_{AgentCount}`

Examples:
- `Repl_RR_cp90_50k` - Replicating vehicles, RoundRobin requests, 90s collection, 50k agents
- `ShiftRR_LoadAware_cp90_100k` - ShiftingRoundRobin vehicles, LoadAware requests, 90s collection, 100k agents

## Quality Metrics

The benchmark captures DRT quality metrics from `drt_customer_stats_drt.csv`:

- **Rejection Rate**: Primary quality indicator. Lower is better.
- **Wait Time (avg/p95)**: How long passengers wait for pickup.
- **In-Vehicle Time**: Time spent in the vehicle.
- **Total Travel Time**: Complete journey time.

> **Note**: The rejection rate is a critical quality metric for comparing partitioner strategies. A faster partitioner with a significantly higher rejection rate may not be preferable.

## Example Results

```csv
name,iterations,min_ms,max_ms,avg_ms,stddev_ms,rides,rejections,rejection_rate,wait_avg_s,wait_p95_s,in_vehicle_time_s,total_travel_time_s
Repl_RR_cp90_50k,1,115485,115485,115485,0,41025,8975,0.1800,9712,19520,708,10420
Repl_LoadAware_cp90_50k,1,122911,122911,122911,0,40660,9340,0.1900,10288,20372,721,11009
RR_RR_cp90_50k,1,135094,135094,135094,0,36850,13150,0.2600,18451,34830,581,19032
RR_LoadAware_cp90_50k,1,142180,142180,142180,0,38043,11957,0.2400,16904,32187,708,17612
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
2. **Match Partitions to CPUs**: Set `maxPartitions` to your CPU core count for optimal performance.
3. **Consider Memory**: `Replicating` partitioner uses more memory but typically achieves the best quality.
4. **Monitor Rejection Rate**: The fastest configuration isn't always the best - check rejection rates.
5. **Multiple Runs**: For production benchmarks, use `measuredRuns > 1` and `warmupRuns >= 1` for more reliable results.

## Command Line Overrides

All parameters can be overridden via command line using the MATSim convention:

```bash
--config:drtBenchmark.agentCounts 50000,100000,200000
--config:drtBenchmark.maxPartitions 16
--config:drtBenchmark.vehiclePartitioners Replicating,RoundRobin
--config:drtBenchmark.outputDirectory output/my-benchmark
```

## Author

Steffen Axer

## License

GNU General Public License v2.0 (see LICENSE file in the repository root)



