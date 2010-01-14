package playground.yu.visum.filter.finalFilters;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationWriter;

/**
 * Write a new plans-file(.xml), while reading an old plans-file(.xml). eine
 * neue "abgenommene" PlansDatei(.xml) schreiben.
 *
 * @author ychen
 */
public class NewPlansWriter extends FinalPersonFilter {
	/**
	 * The underlying PlansWriter of this NewPlansWriter.
	 */
	private PopulationWriter plansWriter;

	// ------------------------CONSTRUCTOR----------------------
	/**
	 * initialize a NewPlansWriter: create a new PlansWriter; write the head of
	 * a plans-file(.xml).
	 *
	 * @param plans -
	 *            Parameter for constructor of PlansWriter.
	 */
	public NewPlansWriter(Population plans, Network network) {
		this.plansWriter = new PopulationWriter(plans, network);
	}

	/**
	 * Write person-block (Plan, Act, Leg, Route...) into plans-file(.xml).
	 *
	 * @param person -
	 *            a Person-object transfered from another PersonFilter
	 */
	@Override
	public void run(Person person) {
		if (person != null)
			this.plansWriter.writePerson(person);
	}

	/**
	 * Writes end-block of a plans-file(.xml).
	 */
	public void writeEndPlans() {
		this.plansWriter.writeEndPlans();
	}
}
