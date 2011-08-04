package playground.mzilske.freight;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class CostMemoryImpl implements CostMemory {

	public class CostTableKey {

		private Id from;
		private Id to;
		private int size;

		public CostTableKey(Id from, Id to, int size) {
			this.from = from;
			this.to = to;
			this.size = size;
		}

		@Override
		public boolean equals(Object obj) {
			CostTableKey other = (CostTableKey) obj;
			return from.equals(other.from) && to.equals(other.to) && size == other.size;
		}

		@Override
		public int hashCode() {
			return from.hashCode() + to.hashCode() + size;
		}
		
		@Override
		public String toString() {
			return "from=" + from + "; to=" + to + "; size=" + size;
		}

	}
	
	private Map<CostTableKey,Double> costMap = new HashMap<CostTableKey, Double>();
	
	public static double learningRate = 0.5;
	
	@Override
	public Double getCost(Id from, Id to, int size) {
		Double cost = costMap.get(makeKey(from,to,size));
		return cost;
	}

	private CostTableKey makeKey(Id from, Id to, int size) {
		CostTableKey key = new CostTableKey(from, to, size);
		return key;
	}

	@Override
	public void memorizeCost(Id from, Id to, int size, double cost) {
		CostTableKey key = makeKey(from,to,size);
		if(costMap.containsKey(key)){
			double memorizedCost = costMap.get(key);
			if(cost < memorizedCost){
				costMap.put(key, cost);
			}
			else{
				costMap.put(key, (memorizedCost*(1-learningRate)+cost*learningRate));
			}
//			
		}
		else{
			costMap.put(key, cost);
		}
	}
	
	Map<CostTableKey,Double> getCostMap(){
		return costMap;
	}
	
	
}
