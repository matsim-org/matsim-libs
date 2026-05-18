# Bicycle network tools

Tools in `org.matsim.contrib.bicycle.network` for building MATSim bicycle networks from OSM data, with
cycling-infrastructure classification and elevation KPIs.

## Entry point

`BicycleNetworkPipeline` — full pipeline: infra classification, OSM-attribute prefixing, bicycle-aware simplification,
service-link cleanup, and elevation KPIs. Produces a MATSim network XML with Z coordinates on nodes and elevation
attributes on links.

CLI usage:

```bash
mvn -pl contribs/bicycle exec:java \
    -Dexec.mainClass=org.matsim.contrib.bicycle.network.BicycleNetworkPipeline \
    -Dexec.args="--input berlin.osm.pbf \
                 --dem sonny-germany-50m.tif --dem-crs EPSG:32632 \
                 --crs EPSG:25832 \
                 --output berlin-bicycle-network.xml.gz"
```

| Option                  | Default | Meaning                                                                                          |
|-------------------------|---------|--------------------------------------------------------------------------------------------------|
| `--input` (required)    | —       | OSM input (`.osm.pbf`)                                                                           |
| `--dem` (required)      | —       | DEM GeoTIFF                                                                                      |
| `--dem-crs` (required)  | —       | CRS of the DEM (e.g. `EPSG:32632` for Sonny Germany)                                             |
| `--output` (required)   | —       | Output network (`.xml.gz`)                                                                       |
| `--crs` (required)      | —       | Output network CRS (e.g. `EPSG:25832`)                                                           |
| `--mode`                | `bike`  | Network mode for cyclable links                                                                  |
| `--country`             | `de`    | Country profile for traffic-sign interpretation: `de`, `at`, or `generic` (see Country profiles) |
| `--ele-sample-step`     | `10.0`  | Distance between elevation samples along a link, in m                                            |
| `--ele-noise-tolerance` | `3.0`   | Douglas-Peucker vertical tolerance, in m                                                         |

## What gets attached to links

| Attribute          | Unit   | Meaning                                                                                |
|--------------------|--------|----------------------------------------------------------------------------------------|
| `bicycle_infra`    | string | Cycling infrastructure category (one of `BicycleInfraCategory.name()`, see below)      |
| `averageElevation` | m      | Mean elevation over the link                                                           |
| `gradient`         | ratio  | Signed end-to-end gradient (`+0.03` = 3 % uphill)                                      |
| `maxGradient`      | ratio  | Steepest gradient on any sub-segment                                                   |
| `elevationGain`    | m      | Cumulative meters climbed                                                              |
| `elevationLoss`    | m      | Cumulative meters descended                                                            |
| `osm:bicycle`      | string | Raw OSM `bicycle=…` value, if present                                                  |
| `osm:surface`      | string | Raw OSM `surface=…` value, if present                                                  |
| `osm:smoothness`   | string | Raw OSM `smoothness=…` value, if present                                               |
| `osm:cycleway`     | string | Raw OSM `cycleway=…` value, if present                                                 |
| `type`             | string | `OsmBicycleReader` highway category (e.g. `highway.service`) — not yet `osm:`-prefixed |
| `origid`           | string | Original OSM way ID(s); hyphen-separated when multiple links were merged               |

Gradients are signed in the direction of travel, so reverse links get the opposite sign. `gradient` alone reads 0 % on a
link with a hill between equal-height endpoints — `maxGradient`, `elevationGain` and `elevationLoss` fill that gap.

## Files

- `BicycleNetworkPipeline` — full pipeline, entry point
- `BicycleInfraClassifier` — classifies OSM tags into a `BicycleInfraCategory` (GraphHopper-style precedence)
- `BicycleInfraCategory` — enum of the 27 infrastructure categories the classifier can produce; `name()` is what gets
  written to the link attribute
- `BicycleLinkPolicy` — per-link hook: infra classification + access rule enforcement (footway whitelist, `bicycle=no`,
  oneway handling)
- `BicycleOsmTags` — bicycle-specific OSM tag keys + frequently-used values, used as `import static`
- `BicycleCountryProfile` — interface for country-specific knobs (traffic-sign predicates, driving direction); see
  Country profiles below
- `BicycleCountryProfiles` — factory mapping a short code (`de` / `at` / `generic`) to a profile; used by the
  `--country` CLI flag
- `BicycleCountryProfileGermany`, `BicycleCountryProfileAustria`, `BicycleCountryProfileGeneric` — concrete profiles
- `ElevationDataParser` — reads a GeoTIFF DEM via GeoTools, handles CRS transformation, samples nearest-neighbor
- `LinkElevationProfile` — samples along a link, applies Douglas-Peucker smoothing, computes KPIs
- `ServiceLinkCleaner` — removes service-link components that don't connect anything useful
- `TagCopy` — optional: copies selected raw OSM tags onto links with a prefix

Tests live in `contribs/bicycle/src/test/java/.../network`:

- `BicycleInfraClassifierTest` — 37 table-driven cases covering every category and the precedence ordering
- `LinkElevationProfileTest` — 7 cases using a synthetic `ElevationSource` (no DEM required, fast)
- `ElevationDataParserTest` — 8 reference points in Berlin against Sonny's DTM 50 m. Uses a small cutout shipped in
  `contribs/bicycle/test/input/...` (see the README there); skipped via `assumeTrue` when the DTM file is missing.

## Pipeline

1. Read OSM with `OsmBicycleReader`. During read, each link's endpoints get a Z stamped from the DEM, and
   `BicycleLinkPolicy` classifies the link and enforces access rules.
2. Move OSM-derived attributes (`bicycle`, `surface`, `smoothness`, `cycleway`) under the `osm:` prefix to separate
   them from pipeline-internal attributes.
3. `NetworkUtils.cleanNetwork` drops isolated components.
4. Bicycle-aware simplification merges consecutive links only when their infra-relevant attributes agree
   (`bicycle_infra`, `type`, `osm:surface`, `osm:bicycle`, `osm:smoothness`). The default simplifier would happily merge
   across infra changes and lose that information.
5. Service-link cleanup removes service dead-ends and hairline branches that don't connect anything useful.
6. Second simplification pass; service cleanup may have created new merge candidates.
7. Optionally rename mode `bike` → whatever was passed via `--mode`. By default (`--mode bike`) this is a no-op.
8. For each surviving link, sample elevations every `--ele-sample-step` meters along the straight line between
   endpoints, Douglas-Peucker-filter the profile with tolerance `--ele-noise-tolerance`, compute KPIs.
9. Write MATSim XML.

Elevation KPIs are computed **after** the simplifier runs — on fewer, longer links — so we sample only what survives.

The pipeline logs a one-line summary after each step (`After OSM read: …`, `After cleanNetwork: …` etc.) so you can see
where the link count drops.

## Elevation parameters

**`--ele-sample-step`** — distance between samples along a link. Pick roughly the DEM resolution: `10` for Sonny 20 m
DTM (the default), `50` for Sonny 50 m DTM. Finer than the DEM adds no information.

**`--ele-noise-tolerance`** — Douglas-Peucker vertical tolerance. Intermediate samples whose elevation deviates less
than this from the straight line between their neighbors are dropped. Needed because DEM quantization, pixel-boundary
jumps, and terrain-vs-road mismatch (bridges, cuttings) produce spurious gradient spikes — seen up to 400 % on flat
Berlin streets without filtering.

| Value | Behaviour                    |
|-------|------------------------------|
| 0     | Disabled                     |
| 2 m   | Conservative                 |
| 3 m   | Default, balanced for Berlin |
| 5 m   | GraphHopper's default        |
| 10 m  | Only big hills survive       |

`gradient` is unaffected by DP (endpoints always kept); `maxGradient`, `elevationGain`, `elevationLoss` are what change.

## Infra classification

`BicycleInfraClassifier` writes one of 27 `BicycleInfraCategory` values to the link attribute `bicycle_infra` (as
`enum.name()`). First match wins, so the order matters. Categories, grouped by precedence rule:

1. `CYCLEWAY_ON_HIGHWAY_PROTECTED` — protected bike lane (physical separation)
2. `CYCLEWAY_LINK` — cycleway link
3. `CROSSING` — crossings
4. `BICYCLE_ROAD`, `BICYCLE_ROAD_VEHICLE_DESTINATION` — Fahrradstraßen
5. `SHARED_BUS_LANE_BUS_WITH_BIKE`, `SHARED_BUS_LANE_BIKE_WITH_BUS` — shared bus/bike lanes
6. `PEDESTRIAN_AREA_BICYCLE_YES` — pedestrian area with bicycle allowed
7. `SHARED_MOTOR_VEHICLE_LANE` — on-street cycling with motor traffic
8. `CYCLEWAY_ON_HIGHWAY_BETWEEN_LANES` — Angstweiche between motor lanes
9. `CYCLEWAY_ON_HIGHWAY_ADVISORY`, `_EXCLUSIVE`, `_ADVISORY_OR_EXCLUSIVE` — on-highway lanes
10. `CYCLEWAY_ADJOINING`, `_ISOLATED`, `_ADJOINING_OR_ISOLATED` — separated cycleways
11. `FOOT_AND_CYCLEWAY_SHARED_ADJOINING`, `_ISOLATED`, `_ADJOINING_OR_ISOLATED` — combined foot+bike paths (shared)
12. `FOOT_AND_CYCLEWAY_SEGREGATED_ADJOINING`, `_ISOLATED`, `_ADJOINING_OR_ISOLATED` — combined foot+bike paths (segregated)
13. `FOOTWAY_BICYCLE_YES_ADJOINING`, `_ISOLATED`, `_ADJOINING_OR_ISOLATED` — footway with bicycle allowed
14. `NEEDS_CLARIFICATION` — matched the precedence but OSM tags were ambiguous
15. `NONE` — no cycling infrastructure

The `_ADJOINING` / `_ISOLATED` / `_ADJOINING_OR_ISOLATED` suffixes correspond to the OSM `is_sidepath=yes` / `=no` /
unset distinction. See `BicycleInfraCategory` for the full list and `BicycleInfraClassifier` for the classification
rules.

`BicycleLinkPolicy` additionally kills links (empty modes, zero capacity) when they're footway/pedestrian without
explicit bike permission, or tagged `bicycle=no`, or the reverse direction of a bicycle-oneway.

## Country profiles

The classification rules that depend on the OSM `traffic_sign=*` tag are country-specific (DE:244 for German bicycle
roads, AT:53.27 for Austrian ones, etc.). These are pluggable via `--country`:

| Code      | Profile                          | Use for                                                                                |
|-----------|----------------------------------|----------------------------------------------------------------------------------------|
| `de`      | `BicycleCountryProfileGermany`   | Germany (default). Recognises DE:244, DE:237, DE:240, DE:241, etc.                     |
| `at`      | `BicycleCountryProfileAustria`   | Austria. Recognises AT:53.26 (Fahrradstraße), AT:52.17, AT:52.17a-a/-b, AT:53.28b.     |
| `generic` | `BicycleCountryProfileGeneric`   | Everywhere else. Skips traffic-sign matching; relies on tag-only logic.                |

The bulk of the classification is country-independent and works from generic OSM tags (`highway=*`, `cycleway=*`,
`bicycle=*`, `foot=*`, `segregated=*`, `is_sidepath`, `separation:*`, `cycleway:right/left`, sidewalk subtags). The
country profile only kicks in for the handful of rules that consult `traffic_sign=*`. So `--country generic` is a
reasonable default for any country without a dedicated profile — it doesn't break anything, it just doesn't pick up the
extra signal from country-specific traffic signs.

Adding a new country: implement `BicycleCountryProfile`, register it in `BicycleCountryProfiles.forCode`, and look at
`BicycleCountryProfileGermany` / `BicycleCountryProfileAustria` as templates. The right-hand-traffic assumption is currently still
hard-coded in `BicycleInfraClassifier` regardless of the profile; left-hand-traffic countries (UK, IE, …) need a
broader refactor.

## DEM

Sonny's DTMs (https://sonny.4lima.de/) are LiDAR-based, much better than SRTM. Germany is available as 20 m (~1.1 GB) or
50 m (~300 MB), both in `EPSG:32632`. License: CC BY 4.0.

## Limitations

- Elevation sampling walks the **straight line** between link endpoints, not the OSM way's curve geometry.
- Nearest-neighbor DEM sampling (the DP filter compensates for most artifacts).
- Bridges and tunnels aren't flagged as such; DP hides most of the resulting spurious gradients but very long bridges
  can still look unrealistic.
- After simplification, some nodes may have been removed. Nodes that survive but were never touched by the reader's
  `setAfterLinkCreated` callback remain without a Z coordinate; the per-link KPIs are unaffected because they sample the
  DEM directly.
- `type` and `origid` are not yet under the `osm:` prefix (see TODO P3.1) because both carry semantics that other code
  depends on (`type=highway.service` for `ServiceLinkCleaner`; `origid` for `NetworkSimplifier` merge tracking).
