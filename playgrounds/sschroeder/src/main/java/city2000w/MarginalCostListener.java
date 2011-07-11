package city2000w;

import java.util.HashMap;

import vrp.algorithms.ruinAndRecreate.basics.Shipment;
import vrp.algorithms.ruinAndRecreate.recreation.RecreationEvent;
import vrp.algorithms.ruinAndRecreate.recreation.RecreationListener;
import vrp.api.Customer;

public class MarginalCostListener implements RecreationListener{

	public class CostRecord {
		public double sumOfCosts = 0.0;
		public int nOfRecords = 0;
		public double avgMc = 0.0;
	}
	private HashMap<Customer, CostRecord> recorder = new HashMap<Customer, CostRecord>();
	
	@Override
	public void inform(RecreationEvent event) {
		if(recorder.containsKey(event.getShipment().getTo())){
			CostRecord costRecord = recorder.get(event.getShipment().getTo());
			costRecord.sumOfCosts += event.getCost();
			costRecord.nOfRecords++;
			costRecord.avgMc = costRecord.sumOfCosts/(double)costRecord.nOfRecords;
		}
		else{
			CostRecord costRecord = new CostRecord();
			costRecord.sumOfCosts += event.getCost();
			costRecord.nOfRecords++;
			recorder.put(event.getShipment().getTo(), costRecord);
		}
	}
	
	public HashMap<Customer,CostRecord> getCostRecorder(){
		return recorder;
	}
}
