package freight;

import java.util.Collection;
import java.util.Collections;

public class ShipperPlan {
	
	private Collection<ScheduledCommodityFlow> scheduledFlows;

	public ShipperPlan(Collection<ScheduledCommodityFlow> scheduledFlows) {
		super();
		this.scheduledFlows = scheduledFlows;
	}

	public Collection<ScheduledCommodityFlow> getScheduledFlows() {
		return Collections.unmodifiableCollection(scheduledFlows);
	}
}
