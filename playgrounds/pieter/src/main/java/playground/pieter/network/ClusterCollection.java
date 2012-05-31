package playground.pieter.network;

import java.util.ArrayList;

import org.matsim.api.core.v01.network.Network;

public class ClusterCollection {
	private ArrayList<LinkCluster> clusterList;
	private double clusterAverage;
	private double clusterSD;
	private Network network;
	
	
	public ClusterCollection(Network network) {
		super();
		this.network = network;
	}
	
	public ArrayList<LinkCluster> getClusterList() {
		return clusterList;
	}
	
	public void populateClusterList() {
		
	}
	
}
