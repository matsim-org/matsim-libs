# PSim performance benchmark

The explicit PSim benchmark provides comparable evidence for optimization work without adding latency to the normal
unit-test or quality-harness runs. It measures the event-generation core separately from end-to-end PSim execution so
that coordinator latency does not hide allocation and lookup improvements.

Run the default workload from the repository root:

```bash
contribs/pseudosimulation/scripts/performance.sh
```

Record a Java Flight Recorder profile with:

```bash
contribs/pseudosimulation/scripts/performance.sh --jfr
```

The recording is written to `contribs/pseudosimulation/target/psim-performance.jfr`. Inspect it with JDK Mission
Control or the JDK `jfr` command. For example:

```bash
jfr summary contribs/pseudosimulation/target/psim-performance.jfr
jfr view hot-methods contribs/pseudosimulation/target/psim-performance.jfr
```

## Workloads and correctness guard

The benchmark creates fixed teleport, car, transit, and evenly mixed populations. Each plan contains one leg. Car
plans traverse a two-link network, and transit plans use a deterministic emulator. Every measured invocation verifies
the exact number of emitted events; an optimization that changes observable output fails the benchmark.

The `serialized-link-lookup` workload performs 100 lookups per configured plan against two links and validates the
accumulated result. Its throughput column represents lookups rather than events.

Results contain the median and minimum elapsed time, events emitted per invocation, and throughput calculated from the
median. Warm-up rounds are excluded. End-to-end results include worker startup and the coordinator's completion wait.

## Reproducible comparisons

Use the same commit base, machine, Java version, power settings, and otherwise idle environment. Run the baseline and
candidate at least three times, compare medians, and retain JFR recordings for material changes. Treat differences
within the observed run-to-run variance as inconclusive.

The defaults are 20,000 plans, three warm-up rounds, and seven measured rounds. They can be overridden without editing
versioned files:

```bash
PSIM_BENCHMARK_PLANS=50000 \
PSIM_BENCHMARK_WARMUPS=5 \
PSIM_BENCHMARK_ROUNDS=10 \
contribs/pseudosimulation/scripts/performance.sh
```

Additional forked-JVM options can be supplied with `PSIM_BENCHMARK_JVM_ARGS`. Do not compare runs with different heap,
garbage collector, or processor settings.

## Initial baseline

The first baseline was captured on 2026-08-12 with OpenJDK 25.0.3+9-LTS, 32 available processors, 20,000 plans,
three warm-up rounds, and seven measured rounds. The environment was the command sandbox used for the refactoring work;
results should be compared only with subsequent runs in an equivalent environment.

| Workload | Median | Minimum | Events/run | Median events/s |
| --- | ---: | ---: | ---: | ---: |
| teleport | 3.813 ms | 2.769 ms | 100,000 | 26,226,516 |
| car | 7.973 ms | 5.444 ms | 200,000 | 25,084,623 |
| transit | 5.227 ms | 4.984 ms | 120,000 | 22,958,049 |
| mixed core | 3.788 ms | 3.752 ms | 140,001 | 36,962,692 |
| mixed end-to-end | 100.696 ms | 100.575 ms | 140,001 | 1,390,332 |

The JFR allocation profile attributes 37.07% of sampled allocation pressure to `LinkedList.linkLast`, supporting the
event-buffer optimization tracked separately. Event construction and the executor account for most remaining sampled
allocation. The short recording contained too few execution samples for reliable CPU ranking; future optimization runs
should use larger plan/round settings when CPU attribution, rather than allocation pressure and elapsed time, is the
primary question.

The approximately 97 ms gap between mixed-core and end-to-end medians also confirms that the coordinator's 100 ms
polling interval dominates small and medium PSim iterations in this workload.
