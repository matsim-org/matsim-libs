package matsimConnector.events;

import java.util.Map;

import matsimConnector.agents.Pedestrian;

import org.matsim.api.core.v01.events.Event;

public class CAAgentConstructEvent extends Event{

	public static final String EVENT_TYPE = "CAAgentConstructEvent";
	public static final String ATTRIBUTE_PERSON = "pedestrian";
	private final Pedestrian pedestrian;
	
	public CAAgentConstructEvent(double time, Pedestrian pedestrian) {
		super(time);
		this.pedestrian = pedestrian;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_PERSON, pedestrian.getId().toString());
		return attr;
	}

	public Pedestrian getPedestrian(){
		return pedestrian;
	}
}
