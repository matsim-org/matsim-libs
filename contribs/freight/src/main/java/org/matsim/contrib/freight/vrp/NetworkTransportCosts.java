package org.matsim.contrib.freight.vrp;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.vrp.basics.CarrierCostParams;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.Counter;

public class NetworkTransportCosts implements Costs{
	
	static class CostKey {
		private String from;
		private String to;
		private double time;

		public CostKey(String from, String to, double time) {
			super();
			this.from = from;
			this.to = to;
			this.time = time;
		}
		public String getFrom() {
			return from;
		}
		public String getTo() {
			return to;
		}
		public double getTime() {
			return time;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((from == null) ? 0 : from.hashCode());
			long temp;
			temp = Double.doubleToLongBits(time);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((to == null) ? 0 : to.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CostKey other = (CostKey) obj;
			if (from == null) {
				if (other.from != null)
					return false;
			} else if (!from.equals(other.from))
				return false;
			if (Double.doubleToLongBits(time) != Double
					.doubleToLongBits(other.time))
				return false;
			if (to == null) {
				if (other.to != null)
					return false;
			} else if (!to.equals(other.to))
				return false;
			return true;
		}
	}
	
	static class TransportData {
		double transportCosts;
		double transportTime;
		public TransportData(double transportCosts, double transportTime) {
			super();
			this.transportCosts = transportCosts;
			this.transportTime = transportTime;
		}
		
	}
	
	private static Logger logger = Logger.getLogger(NetworkTransportCosts.class);
	
	private LeastCostPathCalculator router;
	
	private Network network;
	
	private Map<CostKey, TransportData> transportCosts = new HashMap<CostKey, TransportData>();
	
	private int timeSliceWidth;
	
	private CarrierCostParams costParams;
	
	private Counter ttMemorizedCounter;
	
	private Counter ttRequestedCounter;

	public NetworkTransportCosts(LeastCostPathCalculator pathCalculator, CarrierCostParams costParams, Network network, int timeSliceWidth) {
		super();
		this.router = pathCalculator;
		this.costParams = costParams;
		this.network = network;
		this.timeSliceWidth = timeSliceWidth;
		this.ttMemorizedCounter = new Counter("numTravelTimes memorized ");
		this.ttRequestedCounter = new Counter("numTravelTimes requested ");
	}
	
	@Override
	public Double getTransportCost(String fromId, String toId, double time) {
		int timeSlice = getTimeSlice(time);
		CostKey costKey = makeKey(fromId,toId,timeSlice);
		if(transportCosts.containsKey(costKey)){
			return transportCosts.get(costKey).transportCosts;
		}
		Id fromLinkId = new IdImpl(fromId);
		Id toLinkId = new IdImpl(toId);
		Link fromLink = network.getLinks().get(fromLinkId);
		Link toLink = network.getLinks().get(toLinkId);
		Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(), time);
		memorize(fromId,toId,time,path,costKey);
		return path.travelCost;
	}

	@Override
	public Double getTransportTime(String fromId, String toId, double time) {
//		ttRequestedCounter.incCounter();
		int timeSlice = getTimeSlice(time);
		CostKey costKey = makeKey(fromId,toId,timeSlice);
		if(transportCosts.containsKey(costKey)){
			return transportCosts.get(costKey).transportTime;
		}
		Id fromLinkId = new IdImpl(fromId);
		Id toLinkId = new IdImpl(toId);
		Link fromLink = network.getLinks().get(fromLinkId);
		Link toLink = network.getLinks().get(toLinkId);
		Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(), time);
		memorize(fromId,toId,time,path,costKey);
		return path.travelTime;
	}

	private int getTimeSlice(double time) {
		int timeSlice = (int) time/(int) timeSliceWidth;
		return timeSlice;
	}

	@Override
	public Double getBackwardTransportCost(String fromId, String toId, double arrivalTime) {
		double freeFlowTime = getTransportTime(fromId, toId, 0.0);
		return getTransportCost(fromId, toId, arrivalTime-freeFlowTime);
	}

	@Override
	public Double getBackwardTransportTime(String fromId, String toId, double arrivalTime) { 
		double freeFlowTime = getTransportTime(fromId, toId, 0.0);
		return getTransportTime(fromId, toId, arrivalTime-freeFlowTime);
	}

	private void memorize(String fromId, String toId, double time, Path path, CostKey costKey) {
		transportCosts.put(costKey, new TransportData(path.travelCost,path.travelTime));
//		this.ttMemorizedCounter.incCounter();
	}

	private CostKey makeKey(String fromId, String toId, long time) {
		return new CostKey(fromId,toId,time);
	}

	@Override
	public CarrierCostParams getCostParams() {
		return costParams;
	}

}
