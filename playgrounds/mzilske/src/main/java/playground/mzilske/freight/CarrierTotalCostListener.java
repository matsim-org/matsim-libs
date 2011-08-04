package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

public interface CarrierTotalCostListener {
	
	public static class CarrierCostEvent{
		public double distance;
		public double time;
		public double costs;
		public int volume;
		public double performance;
		public double capacityUse;
		public CarrierCostEvent(double distance, double time, double costs) {
			super();
			this.distance = distance;
			this.time = time;
			this.costs = costs;
		}
		public void setVolume(int volume) {
			this.volume = volume;
		}
		public void setPerformance(double performance) {
			this.performance = performance;
		}
		public void setCapacityUsage(double capacityUse) {
			this.capacityUse = capacityUse;
		}
	}
	
	public void inform(Id carrierId, CarrierCostEvent costEvent);
	
	public void reset(int iteration);
	
	public void finish();
}
