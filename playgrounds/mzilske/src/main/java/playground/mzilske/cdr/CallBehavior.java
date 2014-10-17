package playground.mzilske.cdr;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Person;

public interface CallBehavior {

	boolean makeACall(ActivityEndEvent event);

	boolean makeACall(ActivityStartEvent event);

	boolean makeACall(Id id, double time);

	boolean makeACallAtMorningAndNight(Id<Person> id);

}
