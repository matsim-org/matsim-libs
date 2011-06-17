package vrp.basics;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import vrp.api.Costs;
import vrp.api.Node;


/**
 * 
 * @author stefan schroeder
 *
 */

public class CostsImpl implements Costs{

	private static class CostMatrixKey {
		private Id fromNodeId;
		private Id toNodeId;
		
		public CostMatrixKey(Id fromNodeId, Id toNodeId) {
			super();
			this.fromNodeId = fromNodeId;
			this.toNodeId = toNodeId;
		}
		
		@Override
		public boolean equals(Object obj) {
			CostMatrixKey other = (CostMatrixKey) obj;
			return fromNodeId.equals(other.fromNodeId) && toNodeId.equals(other.toNodeId);
		}

		@Override
		public int hashCode() {
			return fromNodeId.hashCode() + toNodeId.hashCode();
		}
		
	}
	
	private static class CostValues {
		private double time;
		private double distance;
		private double cost;
		
		public CostValues(double time, double distance, double cost) {
			super();
			this.time = time;
			this.distance = distance;
			this.cost = cost;
		}
		
		public double getTime() {
			return time;
		}
		
		public double getDistance() {
			return distance;
		}
		
		public double getCost() {
			return cost;
		}
		
	}
	
	private static Logger logger = Logger.getLogger(CostsImpl.class);
	
	private Map<CostMatrixKey,CostValues> costMatrix = new HashMap<CostMatrixKey, CostValues>();
	
	public Double getCost(Node from, Node to) {
		CostMatrixKey costMatrixKey = new CostMatrixKey(from.getId(), to.getId());
		if(costMatrix.containsKey(costMatrixKey)){
			return costMatrix.get(costMatrixKey).getCost();
		}
		else{
			if(from.getId().equals(to.getId())){
				return 0.0;
			}
			else{
				throw new IllegalStateException("no cost data availabe for " + costMatrixKey);
			}
			
		}
	}

	public Double getDistance(Node from, Node to) {
		CostMatrixKey costMatrixKey = new CostMatrixKey(from.getId(), to.getId());
		if(costMatrix.containsKey(costMatrixKey)){
			return costMatrix.get(costMatrixKey).getDistance();
		}
		else{
			if(from.getId().equals(to.getId())){
				return 0.0;
			}
			else{
				throw new IllegalStateException("no cost data availabe for " + costMatrixKey);
			}
		}
	}

	public Double getTime(Node from, Node to) {
		CostMatrixKey costMatrixKey = new CostMatrixKey(from.getId(), to.getId());
		if(costMatrix.containsKey(costMatrixKey)){
			return costMatrix.get(costMatrixKey).getTime();
		}
		else{
			if(from.getId().equals(to.getId())){
				return 0.0;
			}
			else{
				throw new IllegalStateException("no cost data availabe for " + costMatrixKey);
			}
		}
	}
	
	public void addCostValues(Id from, Id to, Double time, Double distance, Double cost){
		CostMatrixKey costMatrixKey = new CostMatrixKey(from,to);
		CostValues costValues = new CostValues(time,distance,cost);
		if(!costMatrix.containsKey(costMatrixKey)){
			costMatrix.put(costMatrixKey, costValues);
		}
		else{
			logger.warn("key already exist " + costMatrixKey + " and has been overwrited");
			costMatrix.put(costMatrixKey, costValues);
		}
	}
}
