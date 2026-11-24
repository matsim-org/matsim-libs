# Berlin Autofrei MATSim facilities

Use the same JAR build and input layout as the [work experiment](../2026-11-23a-work/README.md). Run `./run-person-based-acc-berlin.sh` from this experiment directory. The default sample is 10% of each input population. Set `SAMPLE=1` to use the full populations.

The launcher submits 48 independent SLURM jobs. Each of the 12 facilities types runs base car, PT, and walk plus policy car. The jobs appear in the queue as `acc-<scenario>-<poi>-<mode>`. Copy the printed `results/<run-id>/` folder back for R.

The comparison assumes base PT and walk represent policy PT and walk. The two simulation runs have matching facility IDs, coordinates, and activity types and identical transit schedules. Their networks differ. If changes to the network affect PT routing or access and egress, run policy PT explicitly before estimating those effects.

In `analysis.R`, set `run_dirs` and register the selected activity types in `poi_meta` and `opportunities`. The script already reuses base PT and walk for the policy comparison. Keep the activity subfolders intact and check person-ID overlap.
