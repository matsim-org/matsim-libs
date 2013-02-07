package playground.vsp.pipeline;

import org.matsim.api.core.v01.population.Person;

public interface PersonSink {
	
	public void process(Person person);
	
	public void complete();

}
