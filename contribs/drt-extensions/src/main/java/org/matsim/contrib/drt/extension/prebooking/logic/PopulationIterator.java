package org.matsim.contrib.drt.extension.prebooking.logic;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.extension.prebooking.logic.PopulationIterator.PersonItem;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.HasModifiablePlan;

/**
 * This is a helper class that allows to loop through all persons that are
 * active in a QSim. It is used to prebook drt legs for specific legs.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class PopulationIterator implements Iterator<PersonItem> {
	private final QSim qsim;
	//private final Iterator<? extends Person> internalIterator;
	private final Iterator<Id<Person>> internalIterator;

	private PopulationIterator(Population population, QSim qsim) {
		this.qsim = qsim;
		
		List<Id<Person>> personIds = new LinkedList<>(population.getPersons().keySet());
		Collections.sort(personIds);
		
		this.internalIterator = personIds.iterator();
	}

	@Override
	public boolean hasNext() {
		return internalIterator.hasNext();
	}

	@Override
	public PersonItem next() {
		Id<Person> personId = internalIterator.next();
		MobsimAgent agent = qsim.getAgents().get(personId);
		Plan plan = ((HasModifiablePlan) agent).getModifiablePlan();
		return new PersonItem(agent, plan);
	}

	public record PersonItem(MobsimAgent agent, Plan plan) {
	}

	static public PopulationIterator create(Population population, QSim qsim) {
		return new PopulationIterator(population, qsim);
	}
}
