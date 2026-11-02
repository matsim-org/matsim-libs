package org.matsim.contrib.msp.traveltimematrix;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * How the free-speed travel time of an origin-destination pair is split over the
 * {@link RoadClassification road classes}: the weights {@code s_k(o,d)} of level 1,
 * <pre>
 *   TT(o, d, t) = TT_ff(o, d) * (1 + beta * sum_k s_k(o,d) * CI_k(t))
 * </pre>
 * A pair whose free-speed path runs 80% of its time on motorway is then slowed down by motorway
 * congestion, not by the congestion of the residential streets it never touches. The shares sum to 1 and
 * carry no absolute travel time of their own, which is what lets them be stored coarsely and reused: the
 * level of detail that matters for a travel time is in {@code TT_ff}, and the level of detail that
 * matters for the <em>mix</em> of roads is much lower.
 * <p>
 * <b>Storage.</b> One unsigned byte per class and zone pair, holding the share in 1/255. For
 * {@code Z} zones and {@code K} classes that is {@code Z * Z * K} bytes, against the {@code Z * Z * 2}
 * bytes of the zonal travel time matrix itself (which stores seconds as an unsigned short) - so the
 * default six classes roughly triple the memory of the zonal part. The bytes of a pair are normalised to
 * sum to 255 exactly, so a lookup returns shares that sum to 1 without having to renormalise per query.
 * <p>
 * <b>Resolution.</b> The shares live on the <b>zone system of the zonal matrix</b>, and a short trip that
 * the sparse node-to-node matrix answers exactly still gets the shares of its enclosing zone pair. That
 * is the deliberate asymmetry of the design: the travel time of a short trip has to be exact, the mix of
 * road classes of a short trip is almost always "local streets" anyway, and giving the shares their own
 * node-level matrix would cost more memory than the travel times do.
 *
 * @see RoadClassSharesCalculator to compute them
 */
public final class RoadClassShares {

	/**
	 * The denominator the stored bytes are shares of. 255 rather than 100 so that the full range of an
	 * unsigned byte is used; the resolution is then 0.4 percentage points, which is far below the
	 * accuracy of the congestion index it multiplies.
	 */
	static final int SCALE = 255;

	private final ZoneSystem zoneSystem;
	private final List<String> roadClasses;

	/**
	 * Zone index to the position of its row, as {@code org.matsim.contrib.zone.skims.Matrix} does it:
	 * zone ids are sparse while the matrix is dense, so the two index spaces are kept apart.
	 */
	private final int[] zoneIndex2matrixIndex;

	/**
	 * {@code [fromZone][toZone * numberOfClasses + class]}, each entry a share in 1/{@link #SCALE}. One
	 * array per row rather than a three-dimensional array, so that a row is one contiguous block.
	 */
	private final byte[][] shares;

	private final int numberOfClasses;

	RoadClassShares(ZoneSystem zoneSystem, List<String> roadClasses) {

		this.zoneSystem = zoneSystem;
		this.roadClasses = List.copyOf(roadClasses);
		this.numberOfClasses = roadClasses.size();

		this.zoneIndex2matrixIndex = new int[Id.getNumberOfIds(Zone.class)];
		Arrays.fill(this.zoneIndex2matrixIndex, -1);
		int nextIndex = 0;
		for (Zone zone : zoneSystem.getZones().values()) {
			this.zoneIndex2matrixIndex[zone.getId().index()] = nextIndex;
			nextIndex++;
		}

		int numberOfZones = zoneSystem.getZones().size();
		this.shares = new byte[numberOfZones][numberOfZones * this.numberOfClasses];
	}

	/**
	 * Sets the shares of one zone pair from absolute free-speed travel times per class. The times are
	 * normalised here, so the caller does not have to: it passes the seconds it summed along the path and
	 * gets the rounding - including the largest remainder correction that makes the bytes sum to
	 * {@link #SCALE} exactly - done for it.
	 *
	 * @param travelTimeByRoadClass seconds spent on each class, indexed as
	 * {@link RoadClassification#getRoadClasses()}. All zero means "nothing is known about this pair",
	 * which is stored as "all of it on the first class"; see {@link #get(Node, Node)}.
	 */
	void set(Zone fromZone, Zone toZone, double[] travelTimeByRoadClass) {

		if (travelTimeByRoadClass.length != this.numberOfClasses) {
			throw new IllegalArgumentException("Expected " + this.numberOfClasses + " road classes, got "
				+ travelTimeByRoadClass.length + ".");
		}

		byte[] row = this.shares[matrixIndex(fromZone)];
		int offset = matrixIndex(toZone) * this.numberOfClasses;

		double total = 0;
		for (double time : travelTimeByRoadClass) {
			total += time;
		}
		if (total <= 0) {
			// a pair the routing found no path for, or a zone to itself: there is no path to split, so
			// everything goes on the first class. The travel time it multiplies is 0 either way.
			row[offset] = (byte) SCALE;
			for (int k = 1; k < this.numberOfClasses; k++) {
				row[offset + k] = 0;
			}
			return;
		}

		// largest remainder: round every share down, then hand the leftover units to the classes with
		// the largest fractional parts, so that the row sums to SCALE exactly and no class is
		// systematically favoured by the rounding
		int[] floors = new int[this.numberOfClasses];
		double[] remainders = new double[this.numberOfClasses];
		int assigned = 0;
		for (int k = 0; k < this.numberOfClasses; k++) {
			double exact = SCALE * travelTimeByRoadClass[k] / total;
			floors[k] = (int) Math.floor(exact);
			remainders[k] = exact - floors[k];
			assigned += floors[k];
		}
		for (int leftover = SCALE - assigned; leftover > 0; leftover--) {
			int best = 0;
			for (int k = 1; k < this.numberOfClasses; k++) {
				if (remainders[k] > remainders[best]) {
					best = k;
				}
			}
			floors[best]++;
			remainders[best] = -1;
		}

		for (int k = 0; k < this.numberOfClasses; k++) {
			row[offset + k] = (byte) floors[k];
		}
	}

	/**
	 * The shares of the zone pair the two nodes fall into.
	 *
	 * @return an array as long as {@link #getRoadClasses()}, summing to 1. A fresh array per call - the
	 * caller is free to keep it.
	 * @throws NoSuchElementException if a node is outside the zone system, which means the matrix and
	 * the query disagree about the network
	 */
	public double[] get(Node fromNode, Node toNode) {
		Zone fromZone = this.zoneSystem.getZoneForNodeId(fromNode.getId())
			.orElseThrow(() -> noZone(fromNode));
		Zone toZone = this.zoneSystem.getZoneForNodeId(toNode.getId())
			.orElseThrow(() -> noZone(toNode));
		return get(fromZone, toZone);
	}

	/**
	 * @see #get(Node, Node)
	 */
	public double[] get(Zone fromZone, Zone toZone) {
		byte[] row = this.shares[matrixIndex(fromZone)];
		int offset = matrixIndex(toZone) * this.numberOfClasses;
		double[] result = new double[this.numberOfClasses];
		for (int k = 0; k < this.numberOfClasses; k++) {
			result[k] = Byte.toUnsignedInt(row[offset + k]) / (double) SCALE;
		}
		return result;
	}

	/**
	 * Weighs a congestion index with the shares of one zone pair, which is the inner loop of level 1 and
	 * the reason {@link #get(Node, Node)} is not the only accessor: this does not allocate.
	 *
	 * @param index the index per class, as long as and indexed like {@link #getRoadClasses()}
	 * @return {@code sum_k s_k * index_k}
	 */
	public double weigh(Node fromNode, Node toNode, double[] index) {
		Zone fromZone = this.zoneSystem.getZoneForNodeId(fromNode.getId()).orElseThrow(() -> noZone(fromNode));
		Zone toZone = this.zoneSystem.getZoneForNodeId(toNode.getId()).orElseThrow(() -> noZone(toNode));
		byte[] row = this.shares[matrixIndex(fromZone)];
		int offset = matrixIndex(toZone) * this.numberOfClasses;
		double weighted = 0;
		for (int k = 0; k < this.numberOfClasses; k++) {
			weighted += Byte.toUnsignedInt(row[offset + k]) * index[k];
		}
		return weighted / SCALE;
	}

	/**
	 * @return the road classes a share vector is indexed by
	 */
	public List<String> getRoadClasses() {
		return this.roadClasses;
	}

	/**
	 * @return the zone system the shares are stored per pair of
	 */
	public ZoneSystem getZoneSystem() {
		return this.zoneSystem;
	}

	/**
	 * @return how many bytes the shares occupy, for the memory report of a run
	 */
	public long getSizeInBytes() {
		return (long) this.shares.length * this.shares.length * this.numberOfClasses;
	}

	private int matrixIndex(Zone zone) {
		int index = this.zoneIndex2matrixIndex[zone.getId().index()];
		if (index < 0) {
			throw new IllegalArgumentException("The road class shares were not computed for zone " + zone.getId()
				+ ".");
		}
		return index;
	}

	private static NoSuchElementException noZone(Node node) {
		return new NoSuchElementException("Node " + node.getId() + " is in no zone of the zone system the road class "
			+ "shares were computed over. The matrix and the query have to use the same network.");
	}
}
