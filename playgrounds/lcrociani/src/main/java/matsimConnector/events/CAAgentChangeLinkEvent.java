package matsimConnector.events;

import java.util.Map;

import matsimConnector.agents.Pedestrian;

import org.matsim.api.core.v01.events.Event;

public class CAAgentChangeLinkEvent extends Event {
	
	public static final String EVENT_TYPE = "CAAgentChangeLinkEvent";
	public static final String ATTRIBUTE_PERSON = "pedestrian";
	public static final String FROM_LINK_ID = "fromLink";
	public static final String TO_LINK_ID = "toLink";
	private final Pedestrian pedestrian;
	private final String fromLinkId;
	private final String toLinkId;
	
	public CAAgentChangeLinkEvent(double time, Pedestrian pedestrian, String fromLinkId, String	 toLinkId) {
		//TODO remove hacking of time LC
		super((int) time +1);
		this.pedestrian = pedestrian;
		this.fromLinkId = fromLinkId;
		this.toLinkId = toLinkId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_PERSON, pedestrian.getId().toString());
		attr.put(FROM_LINK_ID, fromLinkId);
		attr.put(TO_LINK_ID, toLinkId);
		return attr;
	}
	
	public Pedestrian getPedestrian(){
		return pedestrian;
	}
	
	public String getFromLinkId(){
		return fromLinkId;
	}
	
	public String getToLinkId(){
		return toLinkId;
	}
}
