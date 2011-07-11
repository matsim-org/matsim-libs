package city2000w;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import vrp.algorithms.ruinAndRecreate.recreation.RecreationEvent;
import vrp.algorithms.ruinAndRecreate.recreation.RecreationListener;

public class MarginalCostCalculator implements RecreationListener {

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
			return "fromId=" + from.toString() + " toId=" + to.toString() + " size=" + size;
		}
	}
	
	class Entry {
		public double sum = 0.0;
		
		public int number = 0;
		
		public Entry(double cost){
			number++;
			sum = cost;
		}
	}

	private static Logger logger = Logger.getLogger(MarginalCostCalculator.class);
	
	private Map<CostTableKey,Entry> marginalCostTable = new HashMap<CostTableKey, Entry>();
	
	@Override
	public void inform(RecreationEvent event) {
		CostTableKey key = new CostTableKey(getFrom(event),getTo(event),getSize(event));
		
		logger.info(key + " mc=" + event.getCost());
		if(marginalCostTable.containsKey(key)){
			marginalCostTable.get(key).number++;
			marginalCostTable.get(key).sum += event.getCost();
		}
		else{
			marginalCostTable.put(key, new Entry(event.getCost()));
		}
	}
	
	public Map<CostTableKey, Entry> getMarginalCostTable() {
		return marginalCostTable;
	}

	private Id getFrom(RecreationEvent event){
		return event.getShipment().getFrom().getLocation().getId();
	}
	
	private Id getTo(RecreationEvent event){
		return event.getShipment().getTo().getLocation().getId();
	}
	
	private int getSize(RecreationEvent event){
		return event.getShipment().getFrom().getDemand();
	}
	
	public void finish(){
		logger.info("finish");
		double sumMC = 0.0;
		for(CostTableKey key : marginalCostTable.keySet()){
			Entry entry = marginalCostTable.get(key);
			double avgMC = entry.sum/(double)entry.number;
			sumMC += avgMC;
			logger.info(key + " avgMarginalCost=" + avgMC);
		}
		for(CostTableKey key : marginalCostTable.keySet()){
			Entry entry = marginalCostTable.get(key);
			double avgMC = entry.sum/(double)entry.number;
			logger.info(key + " shareOfTotalCosts=" + avgMC/sumMC);
		}
	}
	
	public void reset(){
		marginalCostTable = new HashMap<MarginalCostCalculator.CostTableKey, MarginalCostCalculator.Entry>();
	}

}
