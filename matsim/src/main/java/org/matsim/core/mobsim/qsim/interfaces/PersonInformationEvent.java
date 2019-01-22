package org.matsim.core.mobsim.qsim.interfaces;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

public final class PersonInformationEvent extends Event implements HasPersonId{

	public static final String EVENT_TYPE = "AgentInformation";
	public static final String ATTRIBUTE_PERSON = "person";

	private final Id<Person> personId;
	private final Map<String,String> message ;

	public PersonInformationEvent(final double time, final Id<Person> personId, final Map<String,String> message ) {
		super(time);
		this.personId = personId;
		this.message = message ;
	}

	@Override
	public Id<Person> getPersonId() {
		return this.personId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attrs = super.getAttributes();
		attrs.put(ATTRIBUTE_PERSON, this.personId.toString());
		for( Map.Entry<String, String> entry : message.entrySet() ){
			attrs.put( entry.getKey(), entry.getValue() ) ;
		}
		return attrs;
	}


}
