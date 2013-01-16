package d4d;

import java.util.Comparator;

import org.matsim.api.core.v01.Id;

public class Sighting {

	public Id getAgentId() {
		return agentId;
	}

	public static class StartTimeComparator implements Comparator<Sighting> {
		
		@Override
		public int compare(Sighting o1, Sighting o2) {
			return Double.compare(o1.getDateTime(), o2.getDateTime());
		}
		
	}

	private long dateTime;
	private String cellTowerId;
	private Id agentId;

	public long getDateTime() {
		return dateTime;
	}

	public Sighting(Id agentId, long timeInSeconds, String cellTowerId) {
		super();
		this.agentId = agentId;
		this.dateTime = timeInSeconds;
		this.cellTowerId = cellTowerId;
	}

	public String getCellTowerId() {
		return cellTowerId;
	}

}
