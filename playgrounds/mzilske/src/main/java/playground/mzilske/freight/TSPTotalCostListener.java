package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

public interface TSPTotalCostListener {
	
	public static class TSPCostEvent {
		public Id id;
		
		public int volume;

		
		
		public TSPCostEvent(Id id) {
			super();
			this.id = id;
		}



		public void setVolume(int volume) {
			this.volume = volume;
		}
	}
	
	public void inform(TSPCostEvent costEvent);
	
	public void reset(int iteration);
	
	public void finish();

}
