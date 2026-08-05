# Bicycle network tools

Tools in `org.matsim.contrib.bicycle.network` for attaching cycling-infrastructure categories and elevation
metrics to a MATSim network. The classification itself — `BicycleInfraClassifier`, the category enum and the
country profiles — is shared; what differs is where the network comes from.

## Two paths

| | **SUMO** (`bicycle-attributes`) | **Supersonic** (`bicycle-network`) |
|---|---|---|
| Network built by | osmosis → netconvert → `network-from-sumo` | `OsmBicycleReader`, in this process |
| Classification runs | afterwards, on the finished network | during the OSM read, via a per-link callback |
| Use it when | the scenario already builds its network with SUMO | you want one command from `.osm.pbf` to network |

**Prefer the SUMO path for anything that has to end up in a VSP scenario**, because that is how those scenarios
build their networks — the classification then lands in the scenario network itself instead of beside it. It
also inherits what netconvert brings along: turn restrictions, traffic-light detection, junction joining, and
integer lane counts per direction.

The Supersonic path stays as the reference implementation and is the shorter route for a standalone bicycle
network. Both write the same attributes, so downstream code does not care which produced the network.

---

# Path 1 — SUMO networks

Two commands, both `MATSimAppCommand`s, so a scenario reaches them as `$(sc) prepare <name>`.

**`network-from-sumo` must be run with `--keep-cyclable-minor-ways`.** Footway, pedestrian and
track edges have no link properties of their own, and without the flag the converter skips them
as unknown types.

## `bicycle-attributes`

Runs last, after `network-from-sumo` and `clean-network`, and attaches the categories, the OSM tags and the
elevation metrics.

It needs three views of the same data:

| Input | Why |
|---|---|
| `--network` | the MATSim network to annotate |
| `--sumo-network` | the `sumo.net.xml` it came from: supplies each link's OSM way ids and the edge polyline |
| `--osm` | the `network.osm` netconvert consumed: supplies the way tags |

**Why the tags come from the OSM file and not from the network.** netconvert can pass OSM tags through as edge
params, but it ignores them when deciding whether to merge two edges. On a merged edge the values of the
constituent ways are concatenated — and a tag present on only one of them wins silently. A way tagged
`cycleway=lane` merged with an untagged one yields a single edge tagged `cycleway=lane` over twice the length,
with nothing in the network hinting that half of it is a plain road. Going back to the ways via `origId` is the
only way to see that, and it is why merged links whose ways disagree end up as `NEEDS_CLARIFICATION` rather than
inheriting whichever way happened to win.

```bash
$(sc) prepare bicycle-attributes \
    --network network.xml.gz --sumo-network sumo.net.xml --osm network.osm \
    --dem "DTM Germany 50m v3b by Sonny.tif" --dem-crs EPSG:32632 \
    --output network-bike.xml.gz
```

| Option | Default | Meaning |
|---|---|---|
| `--network` (required) | — | MATSim network from `network-from-sumo` |
| `--sumo-network` (required) | — | the `sumo.net.xml` it was converted from |
| `--osm` (required) | — | the `.osm` file netconvert consumed |
| `--output` (required) | — | output network |
| `--dem` | — | DEM GeoTIFF; without it no elevation metrics. Needs `--dem-crs`. |
| `--dem-crs` | — | CRS of the DEM. Required only with `--dem`. |
| `--country` | `de` | country profile for traffic signs: `de`, `at`, `generic` |
| `--mode` | `bike` | network mode for cyclable links |
| `--bike-area-marker` | — | OSM tag (`key` or `key=value`) restricting the full treatment to marked ways |
| `--ele-sample-step` | `20.0` | distance between elevation samples along a link, in m |
| `--ele-noise-tolerance` | `3.0` | Douglas-Peucker vertical tolerance, in m |
| `--simplify` | off | merge consecutive links that agree on the bicycle attributes (and on modes, lanes, freespeed and capacity), with the Supersonic pipeline's rules — see below |

It writes the two companion files alongside the network, under the matching name and in the same
format `network-from-sumo` uses — including its compression rule: the companions inherit the
network's own extension, so a `.xml.gz` network gets `.csv.gz` companions and a plain `.xml`
network plain CSVs.

| File | |
|---|---|
| `<output>-linkGeometries.csv[.gz]` | generated from the SUMO edges, one polyline per surviving link — the same course the elevation sampling used, so the two cannot disagree |
| `<output>-ft.csv[.gz]` | the link features from next to `--network`, minus the rows of links that were dropped. Skipped when there are none; `apply-network-params` is what reads them. |

Without these the annotated network would have no companion files of its own: the ones
`network-from-sumo` produced are named after the network *before* annotation and still list
everything since dropped. They stay valid by link id — this command only ever removes links,
never renames or adds them — but nothing that looks for `<network>-linkGeometries.csv` finds
them.

The network CRS is read from the network's own `coordinateReferenceSystem` attribute, so there is no `--crs`.
Elevation is sampled along the **SUMO edge polyline**, which holds the geometry nodes `--geometry.remove` folded
in — so gradients follow the real curve without any stored geometry.

**`--simplify`** re-merges what netconvert's attribute-strict `geometry.remove` and the cleanups leave
fragmented: consecutive links merge only when they agree on `bicycle_infra`, `bicycle_area`, `type`,
`osm:surface`, `osm:smoothness` and `allowed_speed` AND on modes, lane count, freespeed and base capacity — the same
rules (and code) the Supersonic pipeline uses, so no classification boundary and no physically different
link is ever merged over. Kilometres are preserved exactly; capacities come out exact because both
converters share `LinkProperties.getLaneCapacity` and its <50 m crossing boost. The merge runs after the
cleanups and **before** the elevation metrics, so gradients are computed over the merged polylines, the
geometry companion carries the concatenated SUMO shapes under the merged ids, and the feature companion
gets a synthesized row per merged link (from the downstream constituent, whose to-junction the merged
link now ends at; the length column is rewritten). What merged links do lose: `osm:` raw tags beyond
surface/smoothness, and `name`/`restricted_lanes` when the constituents disagree on them.

It fails fast rather than producing a plausible-looking wrong network: no link with mode `bike` (usually a
`clean-network` run whose `--modes` forgot `bike`), or a DEM that does not cover the network (usually a wrong
`--dem-crs`). Every step logs counters — merged links whose ways disagreed, tag values dropped as ambiguous,
links dropped per rule, links the DEM had no data for.

### What netconvert does and does not handle

The netconvert column describes version 1.27.1. `--osm.bike-access` is **not optional**: it creates the
contraflow edges for `oneway:bicycle=no` and is what keeps `footway` + `bicycle=yes` alive through
`--keep-edges.by-vclass` — without it that category disappears from the network entirely.

| Rule | netconvert | this command |
|---|---|---|
| contraflow for `oneway:bicycle=no`, `cycleway=opposite_lane` | yes, as `-<wayid>` edges allowing only `bicycle` | nothing to do |
| `service=parking_aisle` | keeps it bike-accessible | drops it |
| `access=no` / `private` / `customer(s)` / `emergency` / `permissive` / `permit` | inconsistent — most of these ignored outright | drops it, unless `bicycle=yes`/`designated` overrides |
| footway/pedestrian without bike permission | mostly removes them, not always | drops the rest |
| `bicycle=no` | mostly disallows bicycle, not always | removes the bike mode, link survives for other traffic |

The rules are applied unconditionally: idempotent where netconvert already did the work, and closing the gap
where it did not.

## `bicycle-keep-edges`

netconvert's `--geometry.remove` joins consecutive edges whose SUMO attributes match, knowing nothing about
cycling infrastructure — so it merges a stretch with a bike lane into one without.

This command produces the keep-edges list for a **second netconvert pass**:

```
netconvert WITHOUT --geometry.remove   →  network-from-sumo  →  bicycle-keep-edges  →  keep.txt
netconvert WITH    --geometry.remove --geometry.remove.keep-edges.input-file keep.txt
```

Everything still merges except across category boundaries, so way-pure classification costs a small
fraction of the additional links that skipping `--geometry.remove` entirely would.

| Option | Default | Meaning |
|---|---|---|
| `--network` (required) | — | MATSim network from the pass **without** `geometry.remove` |
| `--sumo-network` (required) | — | the matching `sumo.net.xml`, also from that pass |
| `--osm` (required) | — | the `.osm` file netconvert consumed |
| `--output` (required) | — | the keep-edges list, one edge id per line |
| `--country` | `de` | country profile for traffic signs |

The comparison is **strictly directed**. Cycling infrastructure is tagged per side of the road, so a way tagged
`cycleway:right=lane` followed by an untagged one is a boundary along the way but not against it, where both
sides are unset. Comparing whole junctions would invent boundaries that are not there.

Skipping the two-pass entirely is a legitimate choice: the network is smaller, more links come out as
`NEEDS_CLARIFICATION`, and `bicycle-attributes` counts exactly how many. What is not legitimate is not knowing.

---

# Path 2 — Supersonic reader (reference)

Rather than producing a plain network and inferring cycling infrastructure downstream, this classifies each
link's category up front, *during* the OSM read itself: the classifier is hooked into
[`OsmBicycleReader`](/contribs/osm/src/main/java/org/matsim/contrib/osm/networkReader/OsmBicycleReader.java)
via a per-link callback, so every link already carries its `bicycle_infra` category by the time the network is
written. DEM-based elevation metrics are attached on top.

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

---

# Shared reference

Everything below applies to both paths, except where a heading says otherwise.

## What gets attached to links

Both paths write these. `bicycle_infra_mixed` and `origid` are the only path-specific ones. The keys are
defined in `BicycleUtils` (with typed getters) and are snake_case throughout — the convention
`network-from-sumo` already uses on the same networks (`allowed_speed`, `restricted_lanes`).

| Attribute          | Unit   | Meaning                                                                                |
|--------------------|--------|----------------------------------------------------------------------------------------|
| `bicycle_infra`    | string | Cycling infrastructure category (one of `BicycleInfraCategory.name()`, see below)      |
| `bicycle_infra_mixed` | bool | *(SUMO path)* set when the category is `NEEDS_CLARIFICATION` because netconvert merged ways that classify differently — as opposed to the classifier finding the tags ambiguous. Only the former is fixable, via `bicycle-keep-edges`. |
| `bicycle_area`     | bool   | Whether the link lies inside the `--bike-area-marker` area: `true` inside, `false` outside, **absent** when no marker was given and the whole network got the full treatment. Filter on this rather than on "has a category" — a link can also be uncategorised for unrelated reasons. |
| `average_elevation` | m     | Mean elevation over the link                                                           |
| `gradient`         | ratio  | Signed end-to-end gradient (`+0.03` = 3 % uphill)                                      |
| `max_gradient`     | ratio  | Steepest gradient on any sub-segment                                                   |
| `elevation_gain`   | m      | Cumulative meters climbed                                                              |
| `elevation_loss`   | m      | Cumulative meters descended                                                            |
| `osm:bicycle`      | string | Raw OSM `bicycle=…` value, if present                                                  |
| `osm:surface`      | string | Raw OSM `surface=…` value, if present                                                  |
| `osm:smoothness`   | string | Raw OSM `smoothness=…` value, if present                                               |
| `osm:cycleway`     | string | Raw OSM `cycleway=…` value, if present                                                 |
| `type`             | string | Raw OSM `highway=…` value (e.g. `service`) — not yet `osm:`-prefixed                   |
| `origid`           | string | *(Supersonic path)* Original OSM way ID(s); hyphen-separated when multiple links were merged. On the SUMO path the way ids live in the `sumo.net.xml` instead. |

The SUMO path writes every tag of `BicycleOsmTags.classificationKeys()` it finds under the `osm:` prefix, not
just the four listed above — and on a merged link only values all constituent ways agree on.

Scoring reads these through `BicycleUtils`: `getSurface()` and `getCyclewaytype()` check the plain key
(`surface`, as `OsmBicycleReader` writes it) first and fall back to the `osm:`-prefixed one, so a network
from either path scores the same.

Gradients are signed in the direction of travel, so reverse links get the opposite sign. `gradient` alone reads 0 % on a
link with a hill between equal-height endpoints — `max_gradient`, `elevation_gain` and `elevation_loss` fill that gap.

Not all of these are consumed by the simulation: `average_elevation` and `osm:bicycle` are written for
inspection only — handy for sanity-checking an extract, but not read by anything downstream.

The five elevation attributes (`average_elevation`, `gradient`, `max_gradient`, `elevation_gain`, `elevation_loss`) are only
attached when a DEM is supplied via `--dem`; without one they are absent.

For ad-hoc debugging you can forward **arbitrary** OSM tags onto links: add their keys to `TAGS_TO_COPY` in
`BicycleNetworkPipeline` and `TagCopier` copies them on verbatim under the `osm:` prefix (empty by default, so a no-op
until you populate it).

## Files

**Classification — shared, and independent of where the network came from:**

- `BicycleInfraClassifier` — classifies OSM tags into a `BicycleInfraCategory` ([radinfra.de](https://radinfra.de/)
  -style precedence)
- `BicycleInfraCategory` — enum of the 27 infrastructure categories the classifier can produce; `name()` is what gets
  written to the link attribute
- `BicycleOsmTags` — bicycle-specific OSM tag keys + frequently-used values, used as `import static`;
  `classificationKeys()` is the single list of tag keys the classifier consults
- `OsmWayDirection` — `FORWARD` / `REVERSE`, reader-neutral on purpose: the Supersonic path maps the reader's own enum
  onto it, the SUMO path derives it from the sign of the link id
- `BicycleCountryProfile` — interface for country-specific knobs (traffic-sign predicates, driving direction); see
  Country profiles below
- `BicycleCountryProfiles` — factory mapping a short code (`de` / `at` / `generic`) to a profile; used by the
  `--country` CLI flag
- `BicycleCountryProfileGermany`, `BicycleCountryProfileAustria`, `BicycleCountryProfileGeneric` — concrete profiles

**Elevation, cleanup and reporting — shared:**

- `ElevationDataParser` — reads a GeoTIFF DEM via GeoTools, handles CRS transformation, samples nearest-neighbor;
  returns `NaN` where the DEM has no data
- `LinkElevationProfile` — samples along a link, applies Douglas-Peucker smoothing, computes metrics
- `ServiceLinkCleaner` — removes service-link components that don't connect anything useful
- `BicycleNetworkOps` — internal: elevation stamping and the infra distribution table; the attribute keys
  live in `BicycleUtils`, next to the getters scoring reads them with

**SUMO path:**

- `SumoBicycleAttributes` — the `bicycle-attributes` command
- `SumoBicycleKeepEdges` — the `bicycle-keep-edges` command
- `OsmWayTags` — reads way tags from an `.osm` file, keyed by way id

**Supersonic path:**

- `BicycleNetworkPipeline` — full pipeline, entry point
- `BicycleLinkPolicy` — per-link hook: infra classification + access rule enforcement (footway whitelist, `bicycle=no`,
  `access` restrictions, oneway handling)
- `TagCopier` — optional: copies selected raw OSM tags onto links with a prefix

**Deprecated, kept for now:** `BicycleOsmNetworkReaderV2`, `CreateBicycleNetworkWithElevation` — both build on the
old `OsmNetworkReader` and are superseded by `BicycleNetworkPipeline`. Nothing references them.

Tests live in `contribs/bicycle/src/test/java/.../network`. The SUMO-path tests run against netconvert-generated
fixtures checked in under `contribs/bicycle/test/input/.../<TestName>/` — real network files, not hand-written
ones, each with the `.osm` it came from so the setup can be reproduced:

- `SumoBicycleAttributesTest` — 10 cases against a ring of nine OSM ways (a ring, because two of them get dropped
  and a chain would fall apart). Covers classification from way tags, the contraflow link, the asphalt fallback,
  the drop rules, elevation along the SUMO polyline, a run without a DEM, and idempotence. One case pins the
  central point: netconvert wrote `cycleway=lane` onto a merged edge although only one of its two ways carried
  it, and the command must not adopt it
- `SumoBicycleKeepEdgesTest` — 5 cases against a chain of five identically-attributed ways. Pins that the
  comparison is directed: a `cycleway:right=lane` boundary appears along the way but not against it
- `OsmWayTagsTest` — 5 cases: whitelist filtering, ways with no relevant tag, and tags on nodes and relations
  that must not leak onto ways
- `BicycleNetworkPipelineTest` — 23 cases, mostly for `process`, the pure transformation seam (no file I/O, synthetic
  `ElevationSource`): two end-to-end runs on a reader-like network (orchestration, gradient signs, `osm:` prefixing,
  `origid` normalization, mode rename), a no-DEM run that attaches no elevation metrics, plus tier-1 cases for the
  individual step methods — simplification merge guards, capacity de-boost, `origid` merging, and reversed-geometry
  repair. Two further cases parse a command line to pin `--free-speed-factor`, which the OSM reader consumes and
  `process` never sees
- `BicycleInfraClassifierTest` — 37 table-driven cases covering 22 of the 27 categories and the precedence ordering
- `BicycleLinkPolicyTest` — 13 cases for the footway/pedestrian whitelist, `bicycle=no`, `access` restrictions
  (incl. the `bicycle=yes/designated` override), and bicycle-oneway handling
- `ServiceLinkCleanerTest` — 4 cases: removing a service dead-end, keeping a service link that connects two roads,
  a no-op when there are no service links, and trimming a hairline twig while keeping the connecting spine
- `LinkElevationProfileTest` — 9 cases using a synthetic `ElevationSource` (no DEM required, fast), incl. sampling
  along a stored `origgeom` course vs. the straight chord
- `ElevationDataParserTest` — 8 reference points in Berlin against Sonny's DTM 50 m, plus 5 cases for missing
  data: `NaN` outside the raster instead of an exception, and the coverage check that turns a wrong `--dem-crs`
  into a message naming both CRS. Uses a small cutout shipped in
  `contribs/bicycle/test/input/org/matsim/contrib/bicycle/network/` (see the README there for source and license); a
  different DTM can be passed via `-Ddem.path=…`. Skipped via `assumeTrue` when the DTM file is missing.
- `TagCopierTest` — 2 cases for the optional raw-OSM-tag copying (`TagCopier`)

## Pipeline (Supersonic path)

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
9. Write MATSim XML, with `--crs` recorded on the network as the `coordinateReferenceSystem` attribute.

Elevation metrics are computed **after** the simplifier runs — on fewer, longer links — so we sample only what survives.

The pipeline logs a one-line summary after each step (`After OSM read: …`, `After cleanNetwork: …` etc.) so you can see
where the link count drops.

## Free speed (Supersonic path)

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
jumps, and terrain-vs-road mismatch (bridges, cuttings) produce spurious gradient spikes, even on flat streets.

| Value | Behaviour                    |
|-------|------------------------------|
| 0     | Disabled                     |
| 2 m   | Conservative                 |
| 3 m   | Default, balanced for Berlin |
| 5 m   | GraphHopper's default        |
| 10 m  | Only big hills survive       |

`gradient` is unaffected by DP (endpoints always kept); `max_gradient`, `elevation_gain`, `elevation_loss` are what change.

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
`access` (`no`, `private`, `customer`/`customers`, `emergency`, `permissive`, `permit`) without a `bicycle=yes/designated` override, or are the synthetic reverse of a
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

**Watch the CRS of the per-state files.** The Germany-wide files are `EPSG:32632` (UTM 32N), the per-state ones
are not — the Saxony DTM is `EPSG:25833` (UTM 33N). Getting `--dem-crs` wrong would otherwise be silent: the DEM
answers every query with its no-data value, which is an ordinary number, so the network would come out looking
flat rather than broken. `ElevationDataParser` therefore returns `NaN` for no-data and out-of-range readings, and
`bicycle-attributes` probes 200 spread-out nodes before doing any work — the resulting error names both CRS and
prints the DEM's own extent, which is usually enough to spot the right one.

## Limitations

### Elevation

- **Sampling follows the true OSM course when it was stored.** With `--store-original-geometry`, elevations are sampled
  along the link's real shape (the `origgeom` polyline), which is more accurate on curved and merged links — the
  straight chord would cut corners and shorten the horizontal run the gradient is measured over. Without the flag no
  geometry is stored, so sampling falls back to the straight line between the link's end nodes.
- **The Douglas-Peucker smoothing targets DEM vertical noise, not geometry — so it stays.** `--ele-noise-tolerance`
  removes spurious gradient spikes from DEM quantization, pixel-boundary jumps, and terrain-vs-road mismatch (bridges,
  cuttings). That noise is vertical and independent of the horizontal path, so sampling more accurate geometry would
  *not* remove the need for smoothing.
- **Nearest-neighbor DEM sampling.** `ElevationDataParser` reads the nearest DEM pixel; the DP filter compensates for
  most of the resulting artifacts.
- **Short links get implausible gradients.** The cause is DEM quantization between two adjacent nodes: on a link
  of a few meters, a single pixel step already reads as a steep slope, so the affected links are almost all far
  below the network's median link length. Douglas-Peucker cannot help here: it only removes intermediate points,
  and the endpoints are pinned to the node Z. This hits the SUMO path harder than the Supersonic one, whose
  simplifier produces longer links. Unfixed; a minimum length for the gradient calculation, a cap at a physically
  sensible value, or smoothing node Z across the neighbourhood would each address it.
- **Bridges and tunnels aren't flagged as such.** DP hides most of the resulting spurious gradients, but very long
  bridges can still look unrealistic.
- **Node Z is transitional and on its way out.** Today the simulation derives each link's gradient from the node Z
  coordinates (in both the speed model and scoring/routing). The intended direction is to stop relying on Z and instead
  consume the pre-computed `gradient` attribute directly, plus the richer `max_gradient` / `elevation_gain` /
  `elevation_loss` metrics. How exactly those feed into speed, scoring and routing is still an open design question. Until
  that's settled, both the Z coordinates and the gradient attributes are written.
- **Some surviving nodes may lack a Z coordinate.** A node that survives simplification but was never touched by the
  reader's `setAfterLinkCreated` callback keeps no Z. The per-link metrics are unaffected (they sample the DEM directly).
  This gap disappears once node Z is retired.

### Geometry

- **Reverse-direction links need a geometry repair.** With `--store-original-geometry`, the reader copies geometry onto
  its synthetic `*_bike-reverse` links in the wrong order; a heuristic pass (`repairReversedGeometry`) mirrors it back
  before simplification. The proper fix belongs upstream in `OsmBicycleReader`.

### Attributes & scoring

- **`type` and `origid` are not yet `osm:`-prefixed** because both carry semantics other code depends on
  (`type=service` for `ServiceLinkCleaner`; `origid` for `NetworkSimplifier` merge tracking) — and unlike
  `getSurface()` / `getCyclewaytype()`, `BicycleUtils.WAY_TYPE` reads the unprefixed `type` with no `osm:`
  fallback, so a `type` → `osm:highway` rename would silently break scoring.
- **Some attributes are inspection-only** and aren't consumed by the simulation: `average_elevation` and
  `osm:bicycle`.
