package playground.mzilske.cdr;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;

public class ZoneEnterEvent extends Event {

	private final Id agentId;
	private final Id zoneId;

	public ZoneEnterEvent(double time, final Id agentId, final Id zoneId) {
		super(time);
		this.agentId = agentId;
		this.zoneId = zoneId;
	}

	@Override
	public String getEventType() {
		return "enter zone";
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put("person", this.agentId.toString());
		attr.put("zone", this.zoneId.toString());
		return attr;
	}

}
