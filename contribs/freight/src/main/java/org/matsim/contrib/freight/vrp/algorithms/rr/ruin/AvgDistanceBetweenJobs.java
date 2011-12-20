package org.matsim.contrib.freight.vrp.algorithms.rr.ruin;

import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Shipment;

public class AvgDistanceBetweenJobs implements JobDistance{

	private Costs costs;
	
	public AvgDistanceBetweenJobs(Costs costs) {
		super();
		this.costs = costs;
	}

	@Override
	public double calculateDistance(Job i, Job j) {
		double avgCost = 0.0;
		if(i instanceof Shipment && j instanceof Shipment){
			if(i.equals(j)){
				avgCost = 0.0;
			}
			else{
				Shipment s_i = (Shipment)i;
				Shipment s_j = (Shipment)j;
				double cost_i1_j1 = calcDist(s_i.getFromId(),s_j.getFromId());
				double cost_i1_j2 = calcDist(s_i.getFromId(),s_j.getToId());
				double cost_i2_j1 = calcDist(s_i.getToId(),s_j.getFromId());
				double cost_i2_j2 = calcDist(s_i.getToId(),s_j.getToId());
				avgCost = (cost_i1_j1 + cost_i1_j2 + cost_i2_j1 + cost_i2_j2)/4;
			}
		}
		else{
			throw new UnsupportedOperationException("currently, this class just works with shipments. when working with services " +
					" also, implement another JobDistance");
		}
		return avgCost;
	}

	private double calcDist(String from, String to) {
		return costs.getGeneralizedCost(from, to, 0.0);
	}

}
