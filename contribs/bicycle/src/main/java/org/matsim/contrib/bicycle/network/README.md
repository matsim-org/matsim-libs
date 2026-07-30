# Bicycle network tools

Tools in `org.matsim.contrib.bicycle.network` for building MATSim bicycle networks from OSM data, with
cycling-infrastructure classification and elevation metrics.

This is a **new approach** to building bicycle networks. Rather than producing a plain network and inferring
cycling infrastructure downstream, it classifies each link's cycling-infrastructure category up front, *during*
the OSM read itself: the classifier is hooked into the reader via a per-link callback, so every link already
carries its `bicycle_infra` category by the time the network is written. DEM-based elevation metrics are attached
on top.

The classic approach remains available and unchanged: run
[`OsmBicycleReader`](/contribs/osm/src/main/java/org/matsim/contrib/osm/networkReader/OsmBicycleReader.java)
directly to get a bicycle network without the up-front infrastructure classification. The new pipeline builds on
that same reader — it just wires the classification (and elevation) into it — and is intended to eventually
replace the classic approach entirely.

## Entry point

`BicycleNetworkPipeline` — full pipeline: infra classification, OSM-attribute prefixing, bicycle-aware simplification,
service-link cleanup, and elevation metrics. Produces a MATSim network XML with Z coordinates on nodes and elevation
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

The DEM is optional — drop `--dem` / `--dem-crs` to build the network without elevation metrics.

| Option                  | Default | Meaning                                                                                          |
|-------------------------|---------|--------------------------------------------------------------------------------------------------|
| `--input` (required)    | —       | OSM input (`.osm.pbf`)                                                                           |
| `--dem`                 | —       | DEM GeoTIFF. Optional: omit it to build the network without elevation metrics. Needs `--dem-crs` when given. |
| `--dem-crs`             | —       | CRS of the DEM (e.g. `EPSG:32632` for Sonny Germany). Required only with `--dem`.                |
| `--output` (required)   | —       | Output network; compression is picked from the extension: `.xml.gz` (gzip), `.xml.zst` (Zstandard), `.xml` (none) |
| `--crs` (required)      | —       | Output network CRS (e.g. `EPSG:25832`)                                                           |
| `--mode`                | `bike`  | Network mode for cyclable links                                                                  |
| `--country`             | `de`    | Country profile for traffic-sign interpretation: `de`, `at`, or `generic` (see Country profiles) |
| `--free-speed-factor`   | `0.9`   | Free-speed factor for urban links; inherited from the OSM reader (see Free speed)                |
| `--ele-sample-step`     | `20.0`  | Distance between elevation samples along a link, in m                                            |
| `--ele-noise-tolerance` | `3.0`   | Douglas-Peucker vertical tolerance, in m                                                         |
| `--store-original-geometry` | `false` | Keep each link's true OSM shape in the `origgeom` attribute through simplification (use `--no-store-original-geometry` to disable) |

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
| `type`             | string | Raw OSM `highway=…` value (e.g. `service`) — not yet `osm:`-prefixed                   |
| `origid`           | string | Original OSM way ID(s); hyphen-separated when multiple links were merged               |

Gradients are signed in the direction of travel, so reverse links get the opposite sign. `gradient` alone reads 0 % on a
link with a hill between equal-height endpoints — `maxGradient`, `elevationGain` and `elevationLoss` fill that gap.

Not all of these are consumed by the simulation: `averageElevation`, `osm:bicycle` and `osm:cycleway` are written for
inspection only — handy for sanity-checking an extract, but not read by anything downstream.

The five elevation attributes (`averageElevation`, `gradient`, `maxGradient`, `elevationGain`, `elevationLoss`) are only
attached when a DEM is supplied via `--dem`; without one they are absent.

For ad-hoc debugging you can forward **arbitrary** OSM tags onto links: add their keys to `TAGS_TO_COPY` in
`BicycleNetworkPipeline` and `TagCopy` copies them on verbatim under the `osm:` prefix (empty by default, so a no-op
until you populate it).

## Files

- `BicycleNetworkPipeline` — full pipeline, entry point
- `BicycleInfraClassifier` — classifies OSM tags into a `BicycleInfraCategory` ([radinfra.de](https://radinfra.de/)
  -style precedence)
- `BicycleInfraCategory` — enum of the 27 infrastructure categories the classifier can produce; `name()` is what gets
  written to the link attribute
- `BicycleLinkPolicy` — per-link hook: infra classification + access rule enforcement (footway whitelist, `bicycle=no`,
  `access=no/private/customer`, oneway handling)
- `BicycleOsmTags` — bicycle-specific OSM tag keys + frequently-used values, used as `import static`
- `BicycleCountryProfile` — interface for country-specific knobs (traffic-sign predicates, driving direction); see
  Country profiles below
- `BicycleCountryProfiles` — factory mapping a short code (`de` / `at` / `generic`) to a profile; used by the
  `--country` CLI flag
- `BicycleCountryProfileGermany`, `BicycleCountryProfileAustria`, `BicycleCountryProfileGeneric` — concrete profiles
- `ElevationDataParser` — reads a GeoTIFF DEM via GeoTools, handles CRS transformation, samples nearest-neighbor
- `LinkElevationProfile` — samples along a link, applies Douglas-Peucker smoothing, computes metrics
- `ServiceLinkCleaner` — removes service-link components that don't connect anything useful
- `TagCopy` — optional: copies selected raw OSM tags onto links with a prefix

Tests live in `contribs/bicycle/src/test/java/.../network`:

- `BicycleNetworkPipelineTest` — 23 cases, mostly for `process`, the pure transformation seam (no file I/O, synthetic
  `ElevationSource`): two end-to-end runs on a reader-like network (orchestration, gradient signs, `osm:` prefixing,
  `origid` normalization, mode rename), a no-DEM run that attaches no elevation metrics, plus tier-1 cases for the
  individual step methods — simplification merge guards, capacity de-boost, `origid` merging, and reversed-geometry
  repair. Two further cases parse a command line to pin `--free-speed-factor`, which the OSM reader consumes and
  `process` never sees
- `BicycleInfraClassifierTest` — 37 table-driven cases covering 22 of the 27 categories and the precedence ordering
- `BicycleLinkPolicyTest` — 13 cases for the footway/pedestrian whitelist, `bicycle=no`, `access=no/private/customer`
  (incl. the `bicycle=yes/designated` override), and bicycle-oneway handling
- `ServiceLinkCleanerTest` — 4 cases: removing a service dead-end, keeping a service link that connects two roads,
  a no-op when there are no service links, and trimming a hairline twig while keeping the connecting spine
- `LinkElevationProfileTest` — 9 cases using a synthetic `ElevationSource` (no DEM required, fast), incl. sampling
  along a stored `origgeom` course vs. the straight chord
- `ElevationDataParserTest` — 8 reference points in Berlin against Sonny's DTM 50 m. Uses a small cutout shipped in
  `contribs/bicycle/test/input/org/matsim/contrib/bicycle/network/` (see the README there for source and license); a
  different DTM can be passed via `-Ddem.path=…`. Skipped via `assumeTrue` when the DTM file is missing.
- `TagCopyTest` — 2 cases for the optional raw-OSM-tag copying (`TagCopy`)

## Pipeline

1. Read OSM with `OsmBicycleReader`. During read, each link's endpoints get a Z stamped from the DEM (when one is
   supplied via `--dem`), and `BicycleLinkPolicy` classifies the link's cycling infrastructure via
   `BicycleInfraClassifier` — written to the `bicycle_infra` attribute as a `BicycleInfraCategory` name — and enforces
   access rules.
2. Normalize `origid` to a `String` (the reader stores it as a `Long`), move OSM-derived attributes (`bicycle`,
   `surface`, `smoothness`, `cycleway`) under the `osm:` prefix, and — with `--store-original-geometry` — repair
   reversed geometry on the reader's synthetic `*_bike-reverse` links.
3. `NetworkUtils.cleanNetwork` drops isolated components.
4. Bicycle-aware simplification merges consecutive links only when their infra-relevant attributes agree
   (`bicycle_infra`, `type`, `osm:surface`, `osm:smoothness`, `allowed_speed`) and their link stats match (allowed
   modes, lanes, freespeed, base capacity). The default simplifier would happily merge across infra changes and lose
   that information.
5. Service-link cleanup removes service dead-ends and hairline branches that don't connect anything useful.
6. Second simplification pass; service cleanup may have created new merge candidates. With `--store-original-geometry`,
   a geometry-consistency check then warns if any stored polyline no longer matches its link length.
7. Optionally rename mode `bike` → whatever was passed via `--mode`. By default (`--mode bike`) this is a no-op.
8. For each surviving link, sample elevations every `--ele-sample-step` meters along its stored `origgeom` course (or
   the straight line between endpoints when none was stored), Douglas-Peucker-filter the profile with tolerance
   `--ele-noise-tolerance`, compute metrics. Skipped entirely when no DEM was supplied.
9. Write MATSim XML.

Elevation metrics are computed **after** the simplifier runs — on fewer, longer links — so we sample only what survives.

The pipeline logs a one-line summary after each step (`After OSM read: …`, `After cleanNetwork: …` etc.) so you can see
where the link count drops.

## Free speed

`--free-speed-factor` is a MATSim OSM-reader concept, not a bicycle one: it scales only links with an OSM `maxspeed`
tag below 51 km/h, everything else keeps the speed derived from its highway type. `allowed_speed` keeps the unscaled
value, so `freespeed < allowed_speed` marks the links it hit. `0.7` matches SUMO-converted scenarios such as Dresden
v1.0.

## Elevation parameters

**`--ele-sample-step`** — distance between samples along a link. Pick roughly the DEM resolution: `20` for the Sonny
20 m DTM (the default), `50` for the 50 m DTM. Finer than the DEM adds no information, and on a nearest-neighbor DEM it
introduces staircase artifacts that the DP filter then has to remove.

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
12. `FOOT_AND_CYCLEWAY_SEGREGATED_ADJOINING`, `_ISOLATED`, `_ADJOINING_OR_ISOLATED` — combined foot+bike paths (
    segregated)
13. `FOOTWAY_BICYCLE_YES_ADJOINING`, `_ISOLATED`, `_ADJOINING_OR_ISOLATED` — footway with bicycle allowed
14. `NEEDS_CLARIFICATION` — matched the precedence but OSM tags were ambiguous
15. `NONE` — no cycling infrastructure

The `_ADJOINING` / `_ISOLATED` / `_ADJOINING_OR_ISOLATED` suffixes correspond to the OSM `is_sidepath=yes` / `=no` /
unset distinction. See `BicycleInfraCategory` for the full list and `BicycleInfraClassifier` for the classification
rules.

`BicycleLinkPolicy` additionally drops links (empties their modes and zeroes capacity, so `cleanNetwork` prunes them)
when they're footway/pedestrian without explicit bike permission, a `service=parking_aisle`, have a restricted general
`access` (`no` / `private` / `customer`) without a `bicycle=yes/designated` override, or are the synthetic reverse of a
bicycle-oneway. A `bicycle=no` link instead only loses its bike mode, so a `highway=primary` etc. survives as a car-only
link.

## Country profiles

The classification rules that depend on the OSM `traffic_sign=*` tag are country-specific (DE:244 for German bicycle
roads, AT:53.26 for Austrian ones, etc.). These are pluggable via `--country`:

| Code      | Profile                        | Use for                                                                            |
|-----------|--------------------------------|------------------------------------------------------------------------------------|
| `de`      | `BicycleCountryProfileGermany` | Germany (default). Recognises DE:244, DE:237, DE:240, DE:241, etc.                 |
| `at`      | `BicycleCountryProfileAustria` | Austria. Recognises AT:53.26 (Fahrradstraße), AT:52.17, AT:52.17a-a/-b, AT:53.28b. |
| `generic` | `BicycleCountryProfileGeneric` | Everywhere else. Skips traffic-sign matching; relies on tag-only logic.            |

The bulk of the classification is country-independent and works from generic OSM tags (`highway=*`, `cycleway=*`,
`bicycle=*`, `foot=*`, `segregated=*`, `bicycle_road=*`, `is_sidepath`, `separation:*`, `cycleway:right/left`, sidewalk
subtags). The
country profile only kicks in for the handful of rules that consult `traffic_sign=*`. So `--country generic` is a
reasonable default for any country without a dedicated profile — it doesn't break anything, it just doesn't pick up the
extra signal from country-specific traffic signs.

Adding a new country: implement `BicycleCountryProfile`, register it in `BicycleCountryProfiles.forCode`, and look at
`BicycleCountryProfileGermany` / `BicycleCountryProfileAustria` as templates. The right-hand-traffic assumption is
currently still
hard-coded in `BicycleInfraClassifier` regardless of the profile; left-hand-traffic countries (UK, IE, …) need a
broader refactor.

## DEM

Sonny's DTMs (https://sonny.4lima.de/) are LiDAR-based, much better than SRTM. Germany is available as 20 m (~1.4 GB) or
50 m (~300 MB), both in `EPSG:32632`. License: CC BY 4.0.

## Limitations

### Elevation

- **Sampling follows the true OSM course when it was stored.** With `--store-original-geometry`, elevations are sampled
  along the link's real shape (the `origgeom` polyline), which is more accurate on curved and merged links — the
  straight chord would cut corners and shorten the horizontal run the gradient is measured over. Without the flag no
  geometry is stored, so sampling falls back to the straight line between the link's end nodes.
- **The Douglas-Peucker smoothing targets DEM vertical noise, not geometry — so it stays.** `--ele-noise-tolerance`
  removes spurious gradient spikes from DEM quantization, pixel-boundary jumps, and terrain-vs-road mismatch (bridges,
  cuttings) — up to 400 % on flat Berlin streets without it. That noise is vertical and independent of the horizontal
  path, so sampling more accurate geometry would *not* remove the need for smoothing.
- **Nearest-neighbor DEM sampling.** `ElevationDataParser` reads the nearest DEM pixel; the DP filter compensates for
  most of the resulting artifacts.
- **Bridges and tunnels aren't flagged as such.** DP hides most of the resulting spurious gradients, but very long
  bridges can still look unrealistic.
- **Node Z is transitional and on its way out.** Today the simulation derives each link's gradient from the node Z
  coordinates (in both the speed model and scoring/routing). The intended direction is to stop relying on Z and instead
  consume the pre-computed `gradient` attribute directly, plus the richer `maxGradient` / `elevationGain` /
  `elevationLoss` metrics. How exactly those feed into speed, scoring and routing is still an open design question. Until
  that's settled, both the Z coordinates and the gradient attributes are written.
- **Some surviving nodes may lack a Z coordinate.** A node that survives simplification but was never touched by the
  reader's `setAfterLinkCreated` callback keeps no Z. The per-link metrics are unaffected (they sample the DEM directly).
  This gap disappears once node Z is retired.

### Geometry

- **Reverse-direction links need a geometry repair.** With `--store-original-geometry`, the reader copies geometry onto
  its synthetic `*_bike-reverse` links in the wrong order; a heuristic pass (`repairReversedGeometry`) mirrors it back
  before simplification. The proper fix belongs upstream in `OsmBicycleReader`.

### Attributes & scoring

- **Scoring reads unprefixed attribute keys.** `BicycleUtils.getSurface()` and `getCyclewaytype()` read `surface` /
  `cycleway`, but this pipeline writes them as `osm:surface` / `osm:cycleway`. A network built here yields `null` there,
  and the default scoring silently falls back to a comfort factor of 1.0 — no error, no warning. Until resolved, either
  skip the `osm:` prefixing for those keys or give `BicycleUtils` a fallback to the prefixed keys. The same trap applies
  to a future `type` → `osm:highway` rename, since `BicycleUtils.WAY_TYPE = "type"`.
- **`type` and `origid` are not yet `osm:`-prefixed** because both carry semantics other code depends on
  (`type=service` for `ServiceLinkCleaner`; `origid` for `NetworkSimplifier` merge tracking).
- **Some attributes are inspection-only** and aren't consumed by the simulation: `averageElevation`, `osm:bicycle`, and
  `osm:cycleway`.