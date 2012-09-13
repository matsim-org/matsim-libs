package playground.gregor.sim2d_v3.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.EventImpl;

public class Sim2DDebugEventImpl extends EventImpl implements Sim2DDebugEvent {

	private static final String EVENT_TYPE = "Sim2DDebugEvent";
	

	public static final String ATTRIBUTE_PERSON = "person";

	private final Id personId;


	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_PERSON, this.personId.toString());
		return attr;
	}

	public Id getPersonId() {
		return this.personId;
	}
	
	public Sim2DDebugEventImpl(double time, Id personId) {
		super(time);
		this.personId = personId;
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public double getVelocity() {
		// TODO Auto-generated method stub
		return 0;
	}

}
