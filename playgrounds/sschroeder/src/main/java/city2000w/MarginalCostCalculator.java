package city2000w;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import vrp.algorithms.ruinAndRecreate.recreation.RecreationEvent;
import vrp.algorithms.ruinAndRecreate.recreation.RecreationListener;

public class MarginalCostCalculator implements RecreationListener {

	public class CostTableKey {
		public Id from;
		public Id to;
		public int size;

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
		public double cost = 0.0;
		
		public int number = 0;
		
		public Entry(double cost){
			number++;
			this.cost = cost;
		}
	}

	private static Logger logger = Logger.getLogger(MarginalCostCalculator.class);
	
	private Map<CostTableKey,Entry> marginalCostRecorder = new HashMap<CostTableKey, Entry>();
	
	private Map<CostTableKey,Double> marginalCostTable = new HashMap<MarginalCostCalculator.CostTableKey, Double>();
	
	private Map<CostTableKey,Double> avgShareTable = new HashMap<MarginalCostCalculator.CostTableKey, Double>();
	
	@Override
	public void inform(RecreationEvent event) {
		CostTableKey key = new CostTableKey(getFrom(event),getTo(event),getSize(event));
		
//		logger.info(key + " mc=" + event.getCost());
		if(marginalCostRecorder.containsKey(key)){
			marginalCostRecorder.get(key).number++;
			marginalCostRecorder.get(key).cost += event.getCost();
		}
		else{
			marginalCostRecorder.put(key, new Entry(event.getCost()));
		}
	}
	
	public Map<CostTableKey, Double> getMarginalCostTable() {
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
		for(CostTableKey key : marginalCostRecorder.keySet()){
			Entry entry = marginalCostRecorder.get(key);
			double avgMC = entry.cost/(double)entry.number;
			marginalCostTable.put(key, avgMC);
			sumMC += avgMC;
		}
		for(CostTableKey key : marginalCostRecorder.keySet()){
			Entry entry = marginalCostRecorder.get(key);
			double avgMC = entry.cost/(double)entry.number;
			double avgShareOfTotCosts = avgMC/sumMC;
			avgShareTable.put(key, avgShareOfTotCosts);
		}
	}
	
	public void reset(){
		marginalCostRecorder = new HashMap<MarginalCostCalculator.CostTableKey, MarginalCostCalculator.Entry>();
		avgShareTable.clear();
		marginalCostTable.clear();
	}

	public Map<CostTableKey, Double> getAvgShareTable() {
		return avgShareTable;
	}

	public void printTables() {
		logger.info("cost tables");
		for(CostTableKey key : marginalCostTable.keySet()){
			logger.info(key + " mc=" + marginalCostTable.get(key));
		}
		logger.info("cost shares");
		for(CostTableKey key : avgShareTable.keySet()){
			logger.info(key + " mc-share=" + avgShareTable.get(key));
		}
		
	}

}
