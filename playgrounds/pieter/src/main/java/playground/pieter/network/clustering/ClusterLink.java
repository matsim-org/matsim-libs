package playground.pieter.network.clustering;

import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;

class ClusterLink implements Link {
	private final LinkImpl link;
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

	public ClusterLink(LinkImpl link) {
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
		return link.calcDistance(coord);
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
		return link.getFreespeedTravelTime();
	}

	public double getFreespeedTravelTime(double time) {
		return link.getFreespeedTravelTime(time);
	}

	public double getFlowCapacity() {
		return link.getFlowCapacityPerSec();
	}

	public double getFlowCapacity(Double time) {
		return link.getFlowCapacityPerSec(time);
	}

	public final String getOrigId() {
		return link.getOrigId();
	}

	public final String getType() {
		return link.getType();
	}

	public final double getEuklideanDistance() {
		return link.getEuklideanLength();
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
		link.setOrigId(id);
	}

	public void setType(String type) {
		link.setType(type);
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
}
