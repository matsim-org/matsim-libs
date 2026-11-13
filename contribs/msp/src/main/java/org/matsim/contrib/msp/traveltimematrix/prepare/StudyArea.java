package org.matsim.contrib.msp.traveltimematrix.prepare;

import jakarta.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.Locale;

/**
 * The area a matrix network is cut to: "is this coordinate in it", and a name to say which area it was.
 * <p>
 * It exists because the study areas of this package are not all shape files. A one-off build and an
 * evaluation take a {@code --shp}, which is what a city boundary comes as - but
 * {@link org.matsim.contrib.msp.traveltimematrix.experiments.RunMatrixCostExperiment} cuts the
 * <b>same</b> network to a dozen radii around one centre, and asking for a dozen shape files to be
 * prepared and kept in step would be a way of introducing a mistake per radius. Both are two lines of
 * geometry behind one predicate, so they become two factories here instead of two code paths in
 * {@link StudyAreaNetwork#cut}.
 * <p>
 * {@link #toString()} is part of the contract, not a convenience: every command logs the area it cut and
 * the sweeps put it in a result column, so a row of a csv has to say what area produced it.
 *
 * @see StudyAreaNetwork#cut(Network, StudyArea, java.util.Set) what does the cutting
 */
public interface StudyArea {

	/**
	 * @param coord a coordinate in the coordinate reference system of the <b>network</b>
	 * @return whether it is inside the area
	 */
	boolean contains(Coord coord);

	/**
	 * The study area of a {@code --shp} command line option, in the coordinate reference system of the
	 * <b>network</b> - taken from the network itself when it carries one, so that a shape file in a
	 * different projection is still read correctly and a silently empty cut is avoided.
	 * <p>
	 * Every command of this package resolves its study area this way, and they have to: the congestion
	 * index, the matrix and the evaluation of the matrix all have to be built over the same area, or the
	 * numbers describe different cities.
	 *
	 * @return the area to cut with, or null when no shape file was given - which keeps the whole network
	 * and is only right when the network already is the study area
	 */
	@Nullable
	static StudyArea fromShapeFile(ShpOptions shp, Network network) {

		Logger log = LogManager.getLogger(StudyArea.class);

		if (!shp.isDefined()) {
			log.warn("No --shp given, so the whole network is used as the study area. That is only what you want "
				+ "if the network is already cut to the area the results are about.");
			return null;
		}

		String crs = ProjectionUtils.getCRS(network);
		ShpOptions.Index index = shp.createIndex(crs != null ? crs : shp.getShapeCrs(), "_");

		return new StudyArea() {
			@Override
			public boolean contains(Coord coord) {
				return index.contains(coord);
			}

			@Override
			public String toString() {
				return "shp(" + shp.getShapeFile() + ")";
			}
		};
	}

	/**
	 * A disc of a given radius around a point: the area a sweep over study area <em>size</em> cuts with.
	 * <p>
	 * Beeline radius and not network distance, which would be a different and much more expensive thing
	 * to ask for - and the wrong thing, because what is being varied is the size of the region, not the
	 * reach of a trip from its centre.
	 *
	 * @param centre in the coordinate reference system of the network
	 * @param radius [m]
	 */
	static StudyArea circle(Coord centre, double radius) {

		if (!(radius > 0)) {
			throw new IllegalArgumentException("A study area radius has to be positive, not " + radius + " m.");
		}

		return new StudyArea() {
			@Override
			public boolean contains(Coord coord) {
				return CoordUtils.calcEuclideanDistance(centre, coord) <= radius;
			}

			@Override
			public String toString() {
				return String.format(Locale.ROOT, "circle(%.0f m around %.0f, %.0f)", radius, centre.getX(),
					centre.getY());
			}
		};
	}

	/**
	 * The centroid of a shape file, in the coordinate reference system of the network: the centre a
	 * radius sweep should use when the study it belongs to already has a boundary for the city.
	 * <p>
	 * The centroid of the <b>union</b> of its features, which for a city boundary with a buffer around it
	 * is close enough to the city centre for the purpose - the radii being swept are kilometres wide, so
	 * a few hundred metres of centre do not move a row of the table.
	 */
	static Coord centreOf(ShpOptions shp, Network network) {
		String crs = ProjectionUtils.getCRS(network);
		Point centroid = shp.getGeometry(crs != null ? crs : shp.getShapeCrs()).getCentroid();
		return new Coord(centroid.getX(), centroid.getY());
	}

	/**
	 * The centre of the smallest box that holds every node of the network.
	 * <p>
	 * The default centre of a radius sweep when none is given, and a deliberately crude one: it is the
	 * middle of the <b>network</b>, which for a scenario built around one city is near that city and for
	 * one built around two is between them. A city centre is a judgement a network cannot make, so a
	 * sweep that is meant to be about a particular city should be given its coordinate - this is what
	 * keeps the command runnable without one.
	 */
	static Coord centreOf(Network network) {

		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (Node node : network.getNodes().values()) {
			minX = Math.min(minX, node.getCoord().getX());
			maxX = Math.max(maxX, node.getCoord().getX());
			minY = Math.min(minY, node.getCoord().getY());
			maxY = Math.max(maxY, node.getCoord().getY());
		}

		if (network.getNodes().isEmpty()) {
			throw new IllegalArgumentException("An empty network has no centre.");
		}

		return new Coord((minX + maxX) / 2, (minY + maxY) / 2);
	}
}
