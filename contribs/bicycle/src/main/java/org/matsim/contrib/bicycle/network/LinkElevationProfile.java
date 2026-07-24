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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * Samples a link's elevation profile from an elevation source and derives
 * elevation metrics.
 *
 * <p>Samples are taken every {@code sampleStep} meters along the link's course,
 * including both endpoints. When the link carries a stored original geometry
 * ({@code origgeom}, written by {@code --store-original-geometry}), the samples
 * follow that true OSM shape -- more accurate on curved and merged links, where
 * the straight chord would cut corners and shorten the horizontal run the
 * gradient is measured over. Without stored geometry the samples fall back to
 * the straight line from the link's fromNode to its toNode. The raw samples are
 * then simplified by a 1-D Douglas-Peucker filter with a vertical tolerance
 * {@code noiseToleranceM}: any intermediate point whose height deviates less
 * than the tolerance from the straight line between its kept neighbours is
 * removed. The metrics are computed on the simplified profile.
 *
 * <p>Why the filter matters — a DEM has two sources of noise: quantisation
 * (Sonny's DTM is 0.1 m vertical) and projection mismatch between terrain and
 * paved road (embankments, bridges, cuttings). Both produce spurious spikes
 * over short distances — 0.5 m over 5 m is already a 10 % "gradient" that
 * isn't really there. The DP filter strips those spikes while keeping real
 * hills (anything > tolerance) intact. This is the same approach GraphHopper
 * uses for its elevation profiles (PR #1953).
 *
 * <p>All gradients are signed in the direction of travel (fromNode → toNode):
 * positive = uphill, negative = downhill. For a bidirectional network
 * where each direction is a separate Link, the reverse link sees the opposite
 * sign — which is exactly what bicycle disutility models expect.
 *
 * <p>If a link's fromNode and toNode carry Z coordinates already, the first
 * and last samples are pinned to those values. This keeps metrics consistent
 * with the per-node elevation attribute and means those endpoints are never
 * removed by DP.
 *
 * @author smetzler, esarikaya
 */
public final class LinkElevationProfile {

	/**
	 * Five metrics derived from the simplified elevation profile along a link.
	 */
	public record Metrics(
		double averageElevation,  // meters a.s.l., mean over the simplified profile
		double gradient,          // signed mean gradient, e.g. +0.032 = 3.2% uphill
		double maxGradient,       // max signed gradient over any simplified segment
		double elevationGain,     // cumulative meters climbed (on simplified profile)
		double elevationLoss      // cumulative meters descended (as positive number)
	) {
	}

	private LinkElevationProfile() {
	}

	public static Metrics compute(Link link, double sampleStep, double noiseToleranceM,
							   ElevationDataParser elevation) {
		return compute(link, sampleStep, noiseToleranceM, elevation::getElevation);
	}

	/**
	 * Function-interface variant — mainly useful for unit tests that mock the elevation source.
	 */
	@FunctionalInterface
	public interface ElevationSource {
		double at(Coord coord);
	}

	public static Metrics compute(Link link, double sampleStep, double noiseToleranceM,
							   ElevationSource elevation) {
		// The link's course to sample along: its true OSM shape when the geometry
		// was stored (--store-original-geometry), otherwise just [fromNode, toNode]
		// -- i.e. the straight chord, exactly as before.
		List<Coord> shape = shapeOf(link);
		double length = shapeLength(shape);

		int numSamples = Math.max(2, (int) Math.ceil(length / sampleStep) + 1);
		double[] distances = new double[numSamples];
		double[] heights = new double[numSamples];

		for (int i = 0; i < numSamples; i++) {
			double t = (double) i / (numSamples - 1);
			distances[i] = t * length;
			heights[i] = elevation.at(pointAtDistance(shape, distances[i], length));
		}

		// Pin the endpoints to the node Z (support points carry none). This keeps
		// the metrics consistent with the per-node elevation attribute and stops DP
		// from ever removing the endpoints.
		Coord from = link.getFromNode().getCoord();
		Coord to = link.getToNode().getCoord();
		if (from.hasZ()) heights[0] = from.getZ();
		if (to.hasZ()) heights[numSamples - 1] = to.getZ();

		// Apply Douglas-Peucker on (distance, height). Endpoints are always kept.
		boolean[] keep = computeDouglasPeucker(distances, heights, noiseToleranceM);

		return computeMetrics(distances, heights, keep, length);
	}

	/**
	 * The support points to sample along, endpoints included.
	 * {@link NetworkUtils#getOriginalGeometry} returns the link's true OSM course
	 * ({@code fromNode}, stored support points, {@code toNode}) when a geometry is
	 * stored, and just {@code [fromNode, toNode]} otherwise.
	 */
	private static List<Coord> shapeOf(Link link) {
		List<Node> nodes = NetworkUtils.getOriginalGeometry(link);
		List<Coord> shape = new ArrayList<>(nodes.size());
		for (Node n : nodes) shape.add(n.getCoord());
		return shape;
	}

	/** Planar (x/y) length of the polyline; support points carry no Z. */
	private static double shapeLength(List<Coord> shape) {
		double length = 0;
		for (int i = 1; i < shape.size(); i++) {
			length += planarDistance(shape.get(i - 1), shape.get(i));
		}
		return length;
	}

	/**
	 * The point at planar distance {@code target} along the polyline, linearly
	 * interpolated within the segment it falls in and clamped to the endpoints.
	 */
	private static Coord pointAtDistance(List<Coord> shape, double target, double length) {
		if (target <= 0 || shape.size() < 2) return shape.get(0);
		if (target >= length) return shape.get(shape.size() - 1);

		double acc = 0;
		for (int i = 1; i < shape.size(); i++) {
			Coord a = shape.get(i - 1);
			Coord b = shape.get(i);
			double seg = planarDistance(a, b);
			if (acc + seg >= target) {
				double f = seg == 0 ? 0 : (target - acc) / seg;
				return new Coord(a.getX() + f * (b.getX() - a.getX()),
					a.getY() + f * (b.getY() - a.getY()));
			}
			acc += seg;
		}
		return shape.get(shape.size() - 1);
	}

	private static double planarDistance(Coord a, Coord b) {
		return Math.hypot(b.getX() - a.getX(), b.getY() - a.getY());
	}

	/**
	 * Iterative Douglas-Peucker on a polyline (distances, heights). Returns a
	 * mask indicating which samples to keep. Endpoints are always kept.
	 * <p>
	 * Iterative rather than recursive to avoid stack overflow on very long links
	 * with many samples.
	 */
	static boolean[] computeDouglasPeucker(double[] distances, double[] heights, double tolerance) {
		int n = distances.length;
		boolean[] keep = new boolean[n];
		if (n <= 2) {
			Arrays.fill(keep, true);
			return keep;
		}
		keep[0] = true;
		keep[n - 1] = true;

		Deque<int[]> stack = new ArrayDeque<>();
		stack.push(new int[]{0, n - 1});

		while (!stack.isEmpty()) {
			int[] range = stack.pop();
			int lo = range[0];
			int hi = range[1];
			if (hi - lo < 2) continue;

			// Find the point between lo and hi with the largest vertical deviation
			// from the straight line (lo, hi) in elevation-over-distance space.
			double x0 = distances[lo], y0 = heights[lo];
			double x1 = distances[hi], y1 = heights[hi];
			double dxSeg = x1 - x0;

			int maxIdx = -1;
			double maxDev = 0.0;
			for (int i = lo + 1; i < hi; i++) {
				double expected;
				if (dxSeg == 0) {
					// Degenerate: x0 == x1. Compare heights directly.
					expected = (y0 + y1) / 2;
				} else {
					double t = (distances[i] - x0) / dxSeg;
					expected = y0 + t * (y1 - y0);
				}
				double dev = Math.abs(heights[i] - expected);
				if (dev > maxDev) {
					maxDev = dev;
					maxIdx = i;
				}
			}

			if (maxIdx != -1 && maxDev > tolerance) {
				keep[maxIdx] = true;
				stack.push(new int[]{lo, maxIdx});
				stack.push(new int[]{maxIdx, hi});
			}
		}
		return keep;
	}

	private static Metrics computeMetrics(double[] distances, double[] heights, boolean[] keep, double length) {
		int n = distances.length;

		double sum = 0;
		double gain = 0;
		double loss = 0;
		double maxGradSigned = 0;
		double maxGradAbs = -1;

		int lastKept = -1;
		for (int i = 0; i < n; i++) {
			if (!keep[i]) continue;
			sum += heights[i];
			if (lastKept >= 0) {
				double dh = heights[i] - heights[lastKept];
				if (dh > 0) gain += dh;
				else loss += -dh;

				double segLen = distances[i] - distances[lastKept];
				if (segLen >= 1.0) {
					double grad = dh / segLen;
					if (Math.abs(grad) > maxGradAbs) {
						maxGradAbs = Math.abs(grad);
						maxGradSigned = grad;
					}
				}
			}
			lastKept = i;
		}

		int keptCount = 0;
		for (boolean b : keep) if (b) keptCount++;

		double avg = keptCount > 0 ? sum / keptCount : heights[0];
		double meanGradient = (length > 1.0)
			? (heights[n - 1] - heights[0]) / length
			: 0.0;

		return new Metrics(avg, meanGradient, maxGradSigned, gain, loss);
	}
}
