package playground.gregor.flooding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.core.api.network.Network;
import org.matsim.core.api.network.Node;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.gregor.MY_STATIC_STUFF;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class NodeRiskCostsFromNetcdf {

	private final static double BASE_COST = 30 * 3600;

	private final Network network;
	private String netcdf;
	private HashMap<Node, Double> nft = null;
	private FloodingReader fr;
	private FloodingLine fl;
	private QuadTree<FloodingInfo> fiQuad;
	private final Map<Integer, QuadTree<Coordinate>> floodLines = new HashMap<Integer, QuadTree<Coordinate>>();

	public NodeRiskCostsFromNetcdf(Network network, String netcdf) {
		this.netcdf = netcdf;
		this.network = network;
	}

	public NodeRiskCostsFromNetcdf(NetworkLayer network, FloodingReader fr) {
		this.network = network;
		this.fr = fr;
	}

	private void init() {
		this.nft = new HashMap<Node, Double>();
		if (this.fr == null) {
			this.fr = new FloodingReader(this.netcdf, true);
		}
		this.fl = new FloodingLine(this.fr);
		Envelope e = this.fr.getEnvelope();
		this.fiQuad = new QuadTree<FloodingInfo>(e.getMinX(), e.getMinY(), e
				.getMaxX(), e.getMaxY());
		for (FloodingInfo fi : this.fr.getFloodingInfos()) {
			this.fiQuad.put(fi.getCoordinate().x, fi.getCoordinate().y, fi);
		}
	}

	public double getNodeRiskCost(Node node) {
		if (this.nft == null) {
			init();
		}
		Double ret = this.nft.get(node);
		if (ret == null) {
			ret = calculateNodeRiskCost(node);
		}

		return ret;

	}

	private Double calculateNodeRiskCost(Node node) {
		FloodingInfo fi = this.fiQuad.get(node.getCoord().getX(), node
				.getCoord().getY());
		double dist = fi.getCoordinate().distance(
				MGC.coord2Coordinate(node.getCoord()));
		if (dist > MY_STATIC_STUFF.BUFFER_SIZE) {
			this.nft.put(node, 0.);
			return 0.;
		}

		if (dist > MY_STATIC_STUFF.FLOODED_DIST_THRESHOLD) {
			Double cost = (BASE_COST / 2)
					* (1 - (dist / MY_STATIC_STUFF.BUFFER_SIZE));
			this.nft.put(node, cost);
			return cost;
		}

		int time = (int) fi.getFloodingTime();
		if (time == 0) {
			this.nft.put(node, BASE_COST);
			return BASE_COST;
		}
		QuadTree<Coordinate> q1 = getFloodlineQuad(time - 1);
		QuadTree<Coordinate> q2 = getFloodlineQuad(time);
		Coordinate c1 = q1.get(node.getCoord().getX(), node.getCoord().getY());
		Coordinate c2 = q2.get(node.getCoord().getX(), node.getCoord().getY());
		double d1 = c1.distance(MGC.coord2Coordinate(node.getCoord()));
		double d2 = c2.distance(MGC.coord2Coordinate(node.getCoord()));
		double realTime = (d1 * (time - 1) + d2 * time) / (d1 + d2);
		Double cost = BASE_COST - 60 * realTime;
		this.nft.put(node, cost);
		return cost;
	}

	public FloodingInfo getNearestFloodingInfo(Node node) {
		if (this.nft == null) {
			init();
		}
		return this.fiQuad.get(node.getCoord().getX(), node.getCoord().getY());

	}

	private QuadTree<Coordinate> getFloodlineQuad(int time) {
		QuadTree<Coordinate> q = this.floodLines.get(time);
		if (q == null) {
			List<ArrayList<Coordinate>> coords = this.fl.getFloodLine(time);
			Envelope e = this.fr.getEnvelope();
			q = new QuadTree<Coordinate>(e.getMinX(), e.getMinY(), e.getMaxX(),
					e.getMaxY());
			for (ArrayList<Coordinate> list : coords) {
				for (Coordinate c : list) {
					q.put(c.x, c.y, c);
				}

			}
			this.floodLines.put(time, q);
		}
		return q;
	}

}
