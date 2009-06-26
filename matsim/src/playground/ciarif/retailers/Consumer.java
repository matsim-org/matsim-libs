package playground.ciarif.retailers;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.population.Person;
import org.matsim.core.basic.v01.IdImpl;

public class Consumer {
	
	private final Id id;
	private final Person person;
	private Id rzId;
	
	public Consumer (int id, Person person, Id rzId) {
		this.person = person;
		this.id = new IdImpl (id);
		this.rzId = rzId;
	}

	public Id getId() {
		return this.id;
	}

	public Person getPerson() {
		return this.person;
	}

	public Id getRzId() {
		return this.rzId;
	}
}
