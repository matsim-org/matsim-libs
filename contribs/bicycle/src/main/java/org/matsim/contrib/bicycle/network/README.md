# Bicycle network tools

Tools in `org.matsim.contrib.bicycle.network` for building MATSim bicycle networks from OSM data, with
cycling-infrastructure classification and elevation KPIs.

## Entry point

`BicycleNetworkPipeline` — full pipeline: infra classification, bicycle-aware simplification, service-link cleanup,
and elevation KPIs. Produces a MATSim network XML with Z coordinates on nodes and elevation attributes on links.

## What gets attached to links

| Attribute          | Unit   | Meaning                                           |
|--------------------|--------|---------------------------------------------------|
| `bicycle_infra`    | string | Cycling infrastructure category (see below)       |
| `averageElevation` | m      | Mean elevation over the link                      |
| `gradient`         | ratio  | Signed end-to-end gradient (`+0.03` = 3 % uphill) |
| `maxGradient`      | ratio  | Steepest gradient on any sub-segment              |
| `elevationGain`    | m      | Cumulative meters climbed                         |
| `elevationLoss`    | m      | Cumulative meters descended                       |

Gradients are signed in the direction of travel, so reverse links get the opposite sign. `gradient` alone reads 0 % on a
link with a hill between equal-height endpoints — `maxGradient`, `elevationGain` and `elevationLoss` fill that gap.

## Files

- `ElevationDataParser` — reads a GeoTIFF DEM via GeoTools, handles CRS transformation, samples nearest-neighbor
- `LinkElevationProfile` — samples along a link, applies Douglas-Peucker smoothing, computes KPIs
- `BicycleInfraClassifier` — classifies OSM tags into a cycling infra category (GraphHopper-style precedence)
- `BicycleLinkPolicy` — per-link hook: infra classification + access rule enforcement (footway whitelist, `bicycle=no`,
  oneway handling)
- `TagCopy` — optional: copies selected raw OSM tags onto links with a prefix
- `BicycleNetworkPipeline` — full pipeline, entry point

## Pipeline

1. Read DEM (GeoTools, nearest-neighbor sampling).
2. Read OSM with `OsmBicycleReader`. During read, each link's endpoints get a Z stamped from the DEM, and
   `BicycleLinkPolicy` classifies the link and enforces access rules.
3. `NetworkUtils.cleanNetwork` drops isolated components.
4. Bicycle-aware simplification merges consecutive links only when their infra-relevant attributes agree
   (`bicycle_infra`, `type`, `surface`, `bicycle`, `smoothness`). The default simplifier would happily merge across
   infra
   changes and lose that information.
5. Service-link cleanup removes service dead-ends and hairline branches that don't connect anything useful.
6. Rename mode `bike` → `bicycle`.
7. For each surviving link, sample elevations every `SAMPLE_STEP_M` along the straight line between endpoints.
8. Apply Douglas-Peucker to the `(distance, elevation)` profile with tolerance `NOISE_TOLERANCE_M`.
9. Compute KPIs on the filtered profile, write MATSim XML.

Elevation KPIs are computed **after** the simplifier runs — on fewer, longer links — so we sample only what survives.

## Elevation parameters (top of `BicycleNetworkPipeline`)

**`SAMPLE_STEP_M`** — distance between samples along a link. Pick roughly the DEM resolution: `10` for Sonny 20 m DTM,
`50` for Sonny 50 m DTM. Finer than the DEM adds no information.

**`NOISE_TOLERANCE_M`** — Douglas-Peucker vertical tolerance. Intermediate samples whose elevation deviates less than
this from the straight line between their neighbors are dropped. Needed because DEM quantization, pixel-boundary jumps,
and terrain-vs-road mismatch (bridges, cuttings) produce spurious gradient spikes — seen up to 400 % on flat Berlin
streets without filtering.

| Value | Behaviour                    |
|-------|------------------------------|
| 0     | Disabled                     |
| 2 m   | Conservative                 |
| 3 m   | Default, balanced for Berlin |
| 5 m   | GraphHopper's default        |
| 10 m  | Only big hills survive       |

`gradient` is unaffected by DP (endpoints always kept); `maxGradient`, `elevationGain`, `elevationLoss` are what change.

## Infra classification (`BicycleInfraClassifier`)

Writes one of these values to the link attribute `bicycle_infra`. First match wins, so the order matters.

1. `CYCLEWAY_ON_HIGHWAY_PROTECTED` — protected bike lane (physical separation)
2. `CYCLEWAY_LINK` — cycleway link
3. `CROSSING` — crossings
4. `BICYCLE_ROAD` / `BICYCLE_ROAD_VEHICLE_DESTINATION` — Fahrradstraßen
5. `SHARED_BUS_LANE_*` — shared bus/bike lanes
6. `PEDESTRIAN_AREA_BICYCLE_YES` — pedestrian area with bicycle allowed
7. `SHARED_MOTOR_VEHICLE_LANE` — on-street cycling with motor traffic
8. `CYCLEWAY_ON_HIGHWAY_BETWEEN_LANES` — Angstweiche between motor lanes
9. `CYCLEWAY_ON_HIGHWAY_ADVISORY` / `_EXCLUSIVE` / `_ADVISORY_OR_EXCLUSIVE` — on-highway lanes
10. `CYCLEWAY_ADJOINING` / `_ISOLATED` / `_ADJOINING_OR_ISOLATED` — separated cycleways
11. `FOOT_AND_CYCLEWAY_SHARED` / `_SEGREGATED` (+ `_ADJOINING` / `_ISOLATED` / `_ADJOINING_OR_ISOLATED`) — combined
    foot+bike paths
12. `FOOTWAY_BICYCLE_YES*` — footway with bicycle allowed
13. `NEEDS_CLARIFICATION` — matched the precedence but OSM tags were ambiguous
14. `NONE` — no cycling infrastructure

`BicycleLinkPolicy` additionally kills links (empty modes, zero capacity) when they're footway/pedestrian without
explicit bike permission, or tagged `bicycle=no`, or the reverse direction of a bicycle-oneway.

## DEM

Sonny's DTMs (https://sonny.4lima.de/) are LiDAR-based, much better than SRTM. Germany is available as 20 m (~1.1 GB) or
50 m (~300 MB), both in `EPSG:32632`.

## Limitations

- Elevation sampling walks the **straight line** between link endpoints, not the OSM way's curve geometry.
- Nearest-neighbor DEM sampling (the DP filter compensates for most artifacts).
- Bridges and tunnels aren't flagged as such; DP hides most of the resulting spurious gradients but very long bridges
  can still look unrealistic.
- After simplification, some nodes may have been removed. Nodes that survive but were never touched by the reader's
  `setAfterLinkCreated` callback remain without a Z coordinate; the per-link KPIs are unaffected because they sample the
  DEM directly.
