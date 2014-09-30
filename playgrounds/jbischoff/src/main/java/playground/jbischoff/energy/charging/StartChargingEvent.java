package playground.jbischoff.energy.charging;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;

public class StartChargingEvent extends Event implements HasPersonId {

	public static final String EVENT_TYPE = "charge";

	public static final String ATTRIBUTE_PERSON = "person";
	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_LEGMODE = "legMode";

	private final Id<Link> linkId;

	private final Id<Person> personId;

	public StartChargingEvent(final double time, final Id<Person> agentId, final Id<Link> linkId) {
		super(time);
		this.linkId = linkId;
		this.personId = agentId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_PERSON, this.personId.toString());
		attr.put(ATTRIBUTE_LINK, (this.linkId == null ? null : this.linkId.toString()));
		
		return attr;
	}

	public Id<Person> getPersonId() {
		return this.personId;
	}


	public Id<Link> getLinkId() {
		return this.linkId;
	}

	public String getEventType() {
		return EVENT_TYPE;
	}

}
