# Bicycle network with elevation

Enriches an OSM-derived MATSim bicycle network with elevation data from a GeoTIFF DEM.

## Output

Each node gets a Z coordinate. Each link gets five attributes:

| Attribute          | Unit  | Meaning                                           |
|--------------------|-------|---------------------------------------------------|
| `averageElevation` | m     | Mean elevation over the link                      |
| `gradient`         | ratio | Signed end-to-end gradient (`+0.03` = 3 % uphill) |
| `maxGradient`      | ratio | Steepest gradient on any sub-segment              |
| `elevationGain`    | m     | Cumulative meters climbed                         |
| `elevationLoss`    | m     | Cumulative meters descended                       |

Gradients are signed in the direction of travel, so reverse links get the opposite sign. `gradient` alone would read 0 %
on a link with a hill between two equal-height endpoints — `maxGradient`, `elevationGain` and `elevationLoss` fill that
gap.

## Files

- `ElevationDataParser` — reads the GeoTIFF, handles CRS transformation
- `LinkElevationProfile` — samples along a link, smooths, computes KPIs
- `CreateBicycleNetworkWithElevation` — end-to-end pipeline, entry point

## Pipeline

1. Read DEM (GeoTools, nearest-neighbor sampling)
2. Build OSM network; stamp elevations onto nodes
3. `cleanNetwork` drops isolated components
4. For each surviving link, sample elevations every `SAMPLE_STEP_M` along the straight line between endpoints
5. Apply Douglas-Peucker to the `(distance, elevation)` profile with tolerance `NOISE_TOLERANCE_M`
6. Compute KPIs on the filtered profile, write MATSim XML

## Parameters (top of `CreateBicycleNetworkWithElevation`)

**`SAMPLE_STEP_M`** — distance between samples. Pick roughly the DEM resolution: `10` for Sonny 20 m DTM, `50` for Sonny
50 m DTM. Finer than the DEM adds no information.

**`NOISE_TOLERANCE_M`** — Douglas-Peucker vertical tolerance. Intermediate samples whose elevation deviates less than
this from the straight line between neighbors are dropped. Necessary because DEM quantization, pixel-boundary jumps, and
terrain-vs-road mismatch (bridges, cuttings) produce spurious gradient spikes up to 400 % on flat streets.

| Value | Behaviour                    |
|-------|------------------------------|
| 0     | Disabled                     |
| 2 m   | Conservative                 |
| 3 m   | Default, balanced for Berlin |
| 5 m   | GraphHopper's default        |
| 10 m  | Only big hills survive       |

`gradient` is unaffected by DP (endpoints always kept); `maxGradient` / gain / loss are what change.

## DEM

Sonny's DTMs (https://sonny.4lima.de/) are LiDAR-based, much better than SRTM. Germany comes as 20 m (~1.1 GB) or 50
m (~300 MB), both in `EPSG:32632`.

## Limitations

- Samples the straight line between endpoints, not the OSM way's curve
- Nearest-neighbor DEM sampling (DP filter compensates)
- Bridges and tunnels aren't flagged; DP hides most artifacts