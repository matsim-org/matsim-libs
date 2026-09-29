# Test DEM: `sonny-dtm-50m-berlin-cutout.tif`

A small Berlin cutout of Sonny's DTM Germany 50 m, shipped so that
`ElevationDataParserTest` can run without a multi-hundred-megabyte download.

## Source and license

| | |
|---|---|
| Source | Sonny's LiDAR Digital Terrain Models for Europe — https://sonny.4lima.de/ |
| Dataset | DTM Germany 50 m, version v3b |
| Author | Sonny |
| License | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) |

**Attribution:** DTM data © Sonny (https://sonny.4lima.de/), licensed under CC BY 4.0.

**Modifications:** this file is *not* the original dataset. It was cropped from the
full Germany DTM to the Berlin area; the pixel values themselves are unchanged.
CC BY 4.0 requires that such changes are indicated, which is what this section does.

## What's in it

| Property | Value |
|---|---|
| CRS | `EPSG:32632` (WGS 84 / UTM zone 32N) |
| Size | 911 × 734 px at 50 m → 45.55 km × 36.7 km |
| Extent (UTM 32N) | 777989.24, 5808857.77 → 823539.24, 5845557.77 |
| Extent (WGS 84) | ~13.083 E, 52.334 N → ~13.785 E, 52.689 N |
| Pixel type | `Float32`, elevation in m above sea level |
| Value range | 27.5 m (Müggelsee) … 118.9 m (Teufelsberg area) |
| NoData | `-32767` |
| Compression | DEFLATE, predictor 2 → ~507 KB on disk |

The extent covers the Berlin city area including all reference points asserted in
`ElevationDataParserTest`. Coordinates outside it have no data — see the caveat below.

## Used by

`ElevationDataParserTest` (`contribs/bicycle/src/test/java/.../network/`) reads this
file by default, at the path relative to the contrib root:

```
test/input/org/matsim/contrib/bicycle/network/sonny-dtm-50m-berlin-cutout.tif
```

The test is skipped via `assumeTrue` when the file is missing, so it stays safe in CI.
To run against a different DTM — e.g. the full Germany file — pass an override:

```bash
mvn -pl contribs/bicycle test -Dtest=ElevationDataParserTest \
    -Ddem.path="C:/path/to/sonny-germany-50m.tif"
```
