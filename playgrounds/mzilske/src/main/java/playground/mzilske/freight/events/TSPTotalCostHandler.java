package playground.mzilske.freight.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;


public interface TSPTotalCostHandler extends TSPEventHandler{
	
	public static class TSPCostEvent extends TSPEventImpl implements Event{
		
		private int volume;

		public int getVolume() {
			return volume;
		}

		public TSPCostEvent(Id id, int volume) {
			super(id);
			this.volume = volume;
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
	
	public void handleEvent(TSPCostEvent event);
	
	public void reset(int iteration);
	
	public void finish();

}
