/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.contrib.bicycle.network;

import java.awt.image.Raster;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Position2D;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * Reads a DEM GeoTIFF via GeoTools and samples elevation at arbitrary world
 * coordinates, transforming them from the scenario CRS into the DEM CRS as
 * needed. Sampling is nearest-neighbor.
 *
 * <p>Coordinates outside the raster, and pixels holding the DEM's no-data value,
 * yield {@link Double#NaN} rather than a number — see {@link #getElevation(Coord)}.
 * Callers that need a DEM covering their whole area should say so up front with
 * {@link #requireCoverageOf}; getting the CRS wrong is otherwise hard to notice,
 * because a no-data value looks like an ordinary reading to everything downstream.
 *
 * <p>DEM sources we know about:
 * <ul>
 *   <li>Sonny's DTMs — https://sonny.4lima.de/ (CC BY 4.0). LiDAR-based and
 *       markedly better than SRTM or EU-DEM; Germany comes as a 20 m and a 50 m
 *       raster, both in {@code EPSG:32632}. Careful: the per-state files from the
 *       same source are <em>not</em> in that CRS.</li>
 *   <li>For future reference, see also GraphHopper PR #3287
 *       (https://github.com/graphhopper/graphhopper/pull/3287) which switched
 *       to Sonny.</li>
 * </ul>
 *
 * @author smetzler, dziemke
 */
public class ElevationDataParser {

	private static final Logger log = LogManager.getLogger(ElevationDataParser.class);

	/**
	 * Readings outside this range are not terrain. The Dead Sea shore is about
	 * -430 m and Mount Everest 8849 m, so anything beyond is a no-data marker the
	 * file did not declare -- -32767 and -9999 are the common ones.
	 */
	private static final double MIN_PLAUSIBLE_ELEVATION = -500;
	private static final double MAX_PLAUSIBLE_ELEVATION = 9000;

	private final String tiffFile;
	private final String scenarioCRS;
	private final String demCRS;

	private final GridCoverage2D grid;
	private final Raster gridData;
	private final CoordinateTransformation ct;
	private final double[] declaredNoData;

	private long samples;
	private long missing;
	private boolean warned;


	public ElevationDataParser(String tiffFile, String scenarioCRS, String demCRS) {
		this.tiffFile = tiffFile;
		this.scenarioCRS = scenarioCRS;
		this.demCRS = demCRS;
		this.ct = TransformationFactory.getCoordinateTransformation(scenarioCRS, demCRS);
		try {
			GeoTiffReader reader = new GeoTiffReader(tiffFile);
			this.grid = reader.read(null);
			this.gridData = grid.getRenderedImage().getData();
		} catch (IOException e) {
			// DataSourceException extends IOException, one catch is enough.
			throw new RuntimeException("Failed to read DEM from " + tiffFile, e);
		}
		this.declaredNoData = readNoDataValues(grid);
	}

	private static double[] readNoDataValues(GridCoverage2D grid) {
		try {
			double[] values = grid.getSampleDimension(0).getNoDataValues();
			return values != null ? values : new double[0];
		} catch (RuntimeException e) {
			// Not every GeoTIFF declares them, and not every driver exposes them.
			// The plausibility range below covers the undeclared case anyway.
			return new double[0];
		}
	}


	public double getElevation(double x, double y) {
		return getElevation(CoordUtils.createCoord(x, y));
	}


	/**
	 * Elevation in meters at that scenario coordinate, or {@link Double#NaN} when the
	 * DEM has nothing there — either because the point falls outside the raster or
	 * because the pixel holds a no-data value.
	 *
	 * <p>NaN rather than an exception, so a DEM that ends a little short of the network
	 * does not abort a run; but NaN rather than the raw pixel value, because a no-data
	 * marker such as -32767 is indistinguishable from a real reading once it has been
	 * averaged into a gradient. If every point comes back NaN, the CRS is wrong —
	 * {@link #requireCoverageOf} turns that into a message that says so.
	 */
	public double getElevation(Coord coord) {

		samples++;

		Coord transformed;
		try {
			transformed = ct.transform(coord);
		} catch (RuntimeException e) {
			// A coordinate far outside the DEM projection's area of validity fails to
			// project at all. That is still just "no elevation here" to the caller, and
			// letting it through would hide the far more common wrong-CRS case behind a
			// projection stack trace.
			return missing(coord, coord, "cannot be projected into the DEM CRS");
		}

		GridCoordinates2D posGrid;
		try {
			posGrid = grid.getGridGeometry()
				.worldToGrid(new Position2D(transformed.getX(), transformed.getY()));
		} catch (TransformException | InvalidGridGeometryException e) {
			throw new RuntimeException("Failed to read elevation at " + coord, e);
		}

		if (!contains(posGrid)) {
			return missing(coord, transformed, "outside the raster");
		}

		double[] pixel = new double[1];
		double value = gridData.getPixel(posGrid.x, posGrid.y, pixel)[0];

		if (isNoData(value)) {
			return missing(coord, transformed, "no-data pixel (" + value + ")");
		}
		return value;
	}

	private boolean contains(GridCoordinates2D posGrid) {
		return posGrid.x >= gridData.getMinX()
			&& posGrid.y >= gridData.getMinY()
			&& posGrid.x < gridData.getMinX() + gridData.getWidth()
			&& posGrid.y < gridData.getMinY() + gridData.getHeight();
	}

	private boolean isNoData(double value) {
		if (Double.isNaN(value)) return true;
		if (value < MIN_PLAUSIBLE_ELEVATION || value > MAX_PLAUSIBLE_ELEVATION) return true;
		for (double noData : declaredNoData) {
			if (value == noData) return true;
		}
		return false;
	}

	private double missing(Coord coord, Coord transformed, String reason) {
		missing++;
		if (!warned) {
			warned = true;
			log.warn("No DEM data at {} ({}). {} Further occurrences are counted, not logged.\n{}",
				transformed, reason, "Scenario coordinate was " + coord + ".", describe());
		}
		return Double.NaN;
	}

	/**
	 * Fails unless the DEM actually covers the given points, which is the cheap way to
	 * catch a wrong {@code demCRS} before it turns into a network full of zero gradients.
	 *
	 * @param probe    coordinates in the scenario CRS, e.g. a sample of network nodes
	 * @param minRatio share of them that has to yield a reading, between 0 and 1
	 * @throws IllegalArgumentException naming both CRS and both extents when it does not
	 */
	public void requireCoverageOf(Collection<Coord> probe, double minRatio) {

		if (probe.isEmpty()) return;

		long covered = probe.stream().filter(c -> !Double.isNaN(getElevation(c))).count();
		double ratio = (double) covered / probe.size();

		if (ratio < minRatio) {
			throw new IllegalArgumentException(String.format(Locale.ROOT,
				"The DEM covers only %d of %d sampled network coordinates (%.1f %%, required %.0f %%). "
					+ "The most likely cause is a wrong --dem-crs: the coordinates are transformed from "
					+ "%s to %s and then land outside the raster.%n%s",
				covered, probe.size(), 100 * ratio, 100 * minRatio, scenarioCRS, demCRS, describe()));
		}
		log.info("DEM coverage check: {} of {} sampled coordinates have data.", covered, probe.size());
	}

	/** Human-readable extent of the DEM, for error messages. */
	public String describe() {
		return String.format(Locale.ROOT,
			"  DEM file:   %s%n  DEM CRS:    %s%n  DEM extent: %s%n  raster:     %d x %d px",
			tiffFile, demCRS, grid.getEnvelope2D(), gridData.getWidth(), gridData.getHeight());
	}

	/** Number of samples taken so far. */
	public long getSampleCount() {
		return samples;
	}

	/** Of those, how many had no data. */
	public long getMissingCount() {
		return missing;
	}
}
