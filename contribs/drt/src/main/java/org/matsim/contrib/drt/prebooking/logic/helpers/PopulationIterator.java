package org.matsim.contrib.drt.prebooking.logic.helpers;

import java.util.Iterator;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.prebooking.logic.helpers.PopulationIterator.PersonItem;
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
	private final Iterator<? extends Person> internalIterator;

	private PopulationIterator(Population population, QSim qsim) {
		this.qsim = qsim;
		this.internalIterator = population.getPersons().values().iterator();
	}

	@Override
	public boolean hasNext() {
		return internalIterator.hasNext();
	}

	@Override
	public PersonItem next() {
		Person person = internalIterator.next();
		MobsimAgent agent = qsim.getAgents().get(person.getId());
		Plan plan = ((HasModifiablePlan) agent).getModifiablePlan();
		return new PersonItem(agent, plan);
	}

	public record PersonItem(MobsimAgent agent, Plan plan) {
	}

	static public class PopulationIteratorFactory {
		private final Population population;
		private final QSim qsim;

		public PopulationIteratorFactory(Population population, QSim qsim) {
			this.population = population;
			this.qsim = qsim;
		}

		public PopulationIterator create() {
			return new PopulationIterator(population, qsim);
		}
	}
}
