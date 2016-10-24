package playground.pieter.network.clustering;

import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class ClusterLink implements Link {
	private final Link link;
	private NodeCluster rootCluster;
	// set this to true if the link is an interlink
	private boolean isInterLink = false;
	private NodeCluster fromCluster;
	private NodeCluster toCluster;

	public String toString() {
		return "ID:" + this.getId().toString() +

		", f:" + (this.fromCluster == null ? "[]" : this.fromCluster.getId())
				+ ", t:"
				+ (this.toCluster == null ? "[]" : this.toCluster.getId())
				+ ", i:" + this.isInterLink() + ", rt: "
				+ (this.rootCluster == null ? "[]" : this.rootCluster.getId());
	}
	
	boolean isInterLink() {
		return isInterLink;
	}

	public ClusterLink(Link link) {
		this.link = link;
		this.isInterLink = false;

	}

	public int hashCode() {
		return link.hashCode();
	}

	public boolean equals(Object obj) {
		return link.equals(obj);
	}

	public final double calcDistance(Coord coord) {
		final Coord coord1 = coord;
		return CoordUtils.distancePointLinesegment(link.getFromNode().getCoord(), link.getToNode().getCoord(), coord1);
	}

	public Node getFromNode() {
		return link.getFromNode();
	}

	public final boolean setFromNode(Node node) {
		return link.setFromNode(node);
	}

	public Node getToNode() {
		return link.getToNode();
	}

	public final boolean setToNode(Node node) {
		return link.setToNode(node);
	}

	public double getFreespeedTravelTime() {
		return NetworkUtils.getFreespeedTravelTime(link) ;
	}

	public double getFreespeedTravelTime(double time) {
		final double time1 = time;
		return NetworkUtils.getFreespeedTravelTime(link, time1) ;
	}

	public double getFlowCapacity() {
		return link.getFlowCapacityPerSec();
	}

	public double getFlowCapacity(Double time) {
		return link.getFlowCapacityPerSec(time);
	}

	public final String getOrigId() {
		return NetworkUtils.getOrigId( link ) ;
	}

	public final String getType() {
		return NetworkUtils.getType(link);
	}

	public final double getEuklideanDistance() {
		return CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), link.getToNode().getCoord());
	}

	public double getCapacity() {
		return link.getCapacity();
	}

	public double getCapacityTimesLength() {
		return link.getCapacity() * link.getLength();
	}
	
	public double getCapacityTimesSpeed() {
		return link.getCapacity() * link.getFreespeed();
	}

	double getCapacity(Double time) {
		return this.getCapacity(time);
	}

	public double getCapacity(double time) {
		return link.getCapacity(time);
	}

	public void setCapacity(double capacityPerNetworkCapcityPeriod) {
		link.setCapacity(capacityPerNetworkCapcityPeriod);
	}

	public double getFreespeed() {
		return link.getFreespeed();
	}

	public double getFreespeed(double time) {
		return link.getFreespeed(time);
	}

	public void setFreespeed(double freespeed) {
		link.setFreespeed(freespeed);
	}

	public double getLength() {
		return link.getLength();
	}

	public final void setLength(double length) {
		link.setLength(length);
	}

	public double getNumberOfLanes() {
		return link.getNumberOfLanes();
	}

	public double getNumberOfLanes(double time) {
		return link.getNumberOfLanes(time);
	}

	public void setNumberOfLanes(double lanes) {
		link.setNumberOfLanes(lanes);
	}

	public final Set<String> getAllowedModes() {
		return link.getAllowedModes();
	}

	public final void setAllowedModes(Set<String> modes) {
		link.setAllowedModes(modes);
	}

	public final void setOrigId(String id) {
		final String id1 = id;
		NetworkUtils.setOrigId( link, id1 ) ;
	}

	public void setType(String type) {
		final String type1 = type;
		NetworkUtils.setType( link, type1);
	}


	public Id getId() {
		return link.getId();
	}

	public Coord getCoord() {
		return link.getCoord();
	}

//	public Network getNetwork() {
//		return link.getNetwork();
//	}
//
	public NodeCluster getRootCluster() {
		return rootCluster;
	}


	/**
	 * Once a cluster is chosen by the algorithm, it needs to be set to the new
	 * root
	 * 
	 * @param root
	 * @param isInterLink
	 */
	public void setNewRoot(NodeCluster root, boolean isInterLink) {
		this.isInterLink = isInterLink;
		if (isInterLink) {
			this.rootCluster = root;
			this.fromCluster = null;
			this.toCluster = null;
		}
	}



	public NodeCluster getFromCluster() {
		return this.fromCluster;
	}

	public NodeCluster getToCluster() {
		return this.toCluster;
	}

	public void setToCluster(NodeCluster toCluster) {
		
		this.toCluster = toCluster;
	}

	public void setFromCluster(NodeCluster fromCluster) {
		this.fromCluster = fromCluster;
	}

	public double getFlowCapacityPerSec() {
		return this.link.getFlowCapacityPerSec();
	}

	public double getFlowCapacityPerSec(double time) {
		return this.link.getFlowCapacityPerSec(time);
	}

	@Override
	public Attributes getAttributes() {
		return link.getAttributes();
	}
}
