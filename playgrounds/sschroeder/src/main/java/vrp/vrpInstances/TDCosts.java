package vrp.vrpInstances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.CrowFlyCosts;
import org.matsim.contrib.freight.vrp.basics.Locations;

class TDCosts implements Costs {
		
		static class CostKey {
			private String from;
			private String to;
			private double time;
			private boolean forwardInTime;
			public CostKey(String from, String to, double time,
					boolean forwardInTime) {
				super();
				this.from = from;
				this.to = to;
				this.time = time;
				this.forwardInTime = forwardInTime;
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
			public boolean isForwardInTime() {
				return forwardInTime;
			}
			
			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + (forwardInTime ? 1231 : 1237);
				result = prime * result
						+ ((from == null) ? 0 : from.hashCode());
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
				TDCosts.CostKey other = (TDCosts.CostKey) obj;
				if (forwardInTime != other.forwardInTime)
					return false;
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
		
		private static Logger log = Logger.getLogger(TDCosts.class);
		
		private List<Double> timeBins;
		
		private List<Double> speed;
		
		private CrowFlyCosts crowFly;
		
		private Map<TDCosts.CostKey, Double> travelTimes = new HashMap<TDCosts.CostKey, Double>();
		
		public TDCosts(Locations locations, List<Double> timeBins, List<Double> speedValues) {
			super();
			speed = new ArrayList<Double>(speedValues);
			this.timeBins = new ArrayList<Double>(timeBins);
			crowFly = new CrowFlyCosts(locations);
		}
			
		@Override
		public Double getGeneralizedCost(String fromId, String toId, double departureTime) {
			return getDistance(fromId,toId,departureTime) + getTransportTime(fromId,toId,departureTime);
//			return getTransportTime(fromId,toId,departureTime);
		}
		
		@Override
		public Double getBackwardGeneralizedCost(String fromId, String toId,double arrivalTime) {
			return getBackwardDistance(fromId, toId, arrivalTime) + getBackwardTransportTime(fromId, toId, arrivalTime);
//			return getBackwardTransportTime(fromId, toId, arrivalTime);
		}

		@Override
		public Double getDistance(String fromId, String toId, double departureTime) {
			double totDistance = crowFly.getDistance(fromId, toId, departureTime);
			return totDistance;
		}

		@Override
		public Double getTransportTime(String fromId, String toId, double departureTime) {
			if(fromId.equals(toId)){
				return 0.0;
			}
			TDCosts.CostKey key = new CostKey(fromId, toId, Math.round(departureTime), true);
			if(travelTimes.containsKey(key)){
				return travelTimes.get(key);
			}
			double totalTravelTime = 0.0;
			double distanceToTravel = getDistance(fromId, toId, departureTime);
			double currentTime = departureTime;
			for(int i=0;i<timeBins.size();i++){
				double timeThreshold = timeBins.get(i);
				if(currentTime < timeThreshold){
					double maxReachableDistance = (timeThreshold-currentTime)*speed.get(i);
					if(distanceToTravel > maxReachableDistance){
						distanceToTravel = distanceToTravel - maxReachableDistance;
						totalTravelTime += (timeThreshold-currentTime);
						currentTime = timeThreshold;
						continue;
					}
					else{ //<= maxReachableDistance
						totalTravelTime += distanceToTravel/speed.get(i);
						break;
					}
				}
			}
			travelTimes.put(key, totalTravelTime);
			return totalTravelTime;
		}


		@Override
		public Double getBackwardTransportTime(String fromId, String toId,double arrivalTime) {
			if(fromId.equals(toId)){
				return 0.0;
			}
			TDCosts.CostKey key = new CostKey(fromId, toId, Math.round(arrivalTime), false);
			if(travelTimes.containsKey(key)){
				return travelTimes.get(key);
			}
			double totalTravelTime = 0.0;
			double distanceToTravel = getDistance(fromId, toId, arrivalTime);
			log.debug("distance2Travel="+distanceToTravel);
			double currentTime = arrivalTime;
			for(int i=timeBins.size()-1;i>=0;i--){
				log.debug("timeBinIndex= " + i);
				double timeThreshold;
				if(i>0){
					timeThreshold = timeBins.get(i-1);
				}
				else{
					timeThreshold = 0;
				}
				log.debug("threshold=" + timeThreshold);
				log.debug("currentTime=" + currentTime);
				if(currentTime > timeThreshold){
					double maxReachableDistance = (currentTime - timeThreshold)*speed.get(i);
					if(distanceToTravel > maxReachableDistance){
						distanceToTravel = distanceToTravel - maxReachableDistance;
						totalTravelTime += (currentTime-timeThreshold);
						currentTime = timeThreshold;
						log.debug("dist>maxReachDist; distance2Travel=" + distanceToTravel + " totTravelTime=" + totalTravelTime);
						continue;
					}
					else{ //<= maxReachableDistance
						totalTravelTime += distanceToTravel/speed.get(i);
						log.debug("dist<=maxReachDis; totTravelTime=" + totalTravelTime);
						break;
					}
				}
			}
			travelTimes.put(key, totalTravelTime);
			return totalTravelTime;
		}

		@Override
		public Double getBackwardDistance(String fromId, String toId,double arrivalTime) {
			return getDistance(fromId, toId, arrivalTime);
		}
		
	}