package playground.mzilske.freight.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;


public interface CarrierTotalCostHandler extends CarrierEventHandler{
	
	public static class CarrierCostEvent extends CarrierEventImpl implements Event{
		public double distance;
		public double time;
		public double costs;
		public int volume;
		public double performance;
		public double capacityUse;
		public CarrierCostEvent(Id carrierId, double distance, double time, double costs) {
			super(carrierId);
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
		@Override
		public double getTime() {
			// TODO Auto-generated method stub
			return 0;
		}
		@Override
		public Map<String, String> getAttributes() {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	public void handleEvent(CarrierCostEvent costEvent);
	
}
