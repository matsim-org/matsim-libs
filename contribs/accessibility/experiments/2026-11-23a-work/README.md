# Berlin Autofrei work accessibility

The Java runner reads completed MATSim runs and writes one set of accessibility CSVs per scenario, mode, and opportunity. SLURM schedules the jobs. R reads the CSVs afterward.

## 1. Run a small local check

In IntelliJ, run `org.matsim.contrib.accessibility.run.RunPersonBasedAccBerlin` with JDK 25 and the `accessibility` module classpath. Example program arguments:

```text
--opportunity=facilities --activity-type=work --scenario=base --mode=car --measure-type=rawSum --scenario-dir=/path/to/base-run --output-dir=/tmp/berlin-work-check --sample=0.001
```

Pass the simulation output folder as `--scenario-dir`. The runner detects the filename prefix and supports plain XML, gzip, and zstd.

## 2. Build and submit on Lise

From the `matsim-libs` root:

```bash
mvn -Pcluster-jar -pl contribs/accessibility -am -DskipTests package
```

Upload the launcher and the single shaded JAR from `contribs/accessibility/target/RunPersonBasedAccBerlin-cluster.jar` into the cluster experiment directory. Preserve the simulation folder names as shown below. For each run, transfer only its final `output_network`, `output_plans`, `output_transitSchedule`, `output_events`, and `output_facilities` XML files, keeping their filenames and compression.

- `2026-11-23a-work/10pct-v6.4/berlin-autofrei-v6.4-baseCaseCtdExtended-10pct/`
- `2026-11-23a-work/10pct-v6.4/berlin-autofrei-v6.4-policy-10pct-deparking-p20_d0_i0_max1000/`

From the cluster experiment directory:

```bash
./run-person-based-acc-berlin.sh
```

The launcher creates `logs/` and submits six independent jobs named `acc-<scenario>-<poi>-<mode>` for `work`, covering base and policy across car, PT, and walk. Add entries such as `facilities:shop_daily` to the launcher’s `opportunities` list to submit six more tasks per entry. The pilot uses `SAMPLE=0.1` of each input population. Set `SAMPLE=1` for the full input populations.

## 3. Bring results back for R

Copy `results/<run-id>/base/` and `results/<run-id>/policy/` back with the opportunity subfolders intact. Keep the `pbAcc_*.csv`, `persons_*.csv`, and `locations.csv` files.

In [analysis.R](../../src/main/R/THESIS/analysis.R), point `run_dirs` at the returned folders. Before analysing work, add its `poi_meta` entry and set `opportunities <- "work"`. The main multimodal calculation currently reuses base PT and walk. Update it to read the policy PT and walk outputs when comparing all six runs.

For reviews or automated edits, match records by person ID and check sample overlap across scenarios. Each scenario reads its own facilities file. Compare the work destination sets before interpreting the accessibility difference as a transport-only effect. Keep the mode outputs in `rawSum` units so R can sum them before taking the log.
