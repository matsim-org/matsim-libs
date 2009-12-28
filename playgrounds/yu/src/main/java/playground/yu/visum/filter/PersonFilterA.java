/**
 * 
 */
package playground.yu.visum.filter;

import org.matsim.api.core.v01.population.Person;

/**
 * This class must be subclassed, the subclass usually provides implementations
 * for the abstract method: boolean
 * org.matsim.playground.filters.filter.PersonFilterA.judge(Person person) in
 * its parent class. However, if it does not, the subclass must also be declared
 * abstract.
 * 
 * @author yu chen
 */
public abstract class PersonFilterA extends Filter implements PersonFilterI {
	/* -------------------MEMBER VARIRABLE----------------------
	 */
	private PersonFilterI nextPersonFilter = null;

	/* ---------------------------SETTER---------------------------*/
	/**
	 * This function is normally called in TestRun-Function, e.g. void
	 * org.matsim.playground.filters.test.PersonFilterTest.testRunIDandActTypeundDepTimeFilter()
	 * 
	 * @param nextFilter
	 *            The nextFilter to set.
	 */
	public void setNextFilter(PersonFilterI nextFilter) {
		this.nextPersonFilter = nextFilter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.demandmodeling.plans.algorithms.PersonAlgorithm#run(org.matsim.demandmodeling.plans.Person)
	 *      ----------------------------RUNNER--------------------------
	 *      000000000000000000000000000000000000000000000000000000000000
	 *      @Attention: the last PersonFilterA must override the run(Person
	 *      person) function
	 *      000000000000000000000000000000000000000000000000000000000000
	 */
	public void run(Person person) {
		if (judge(person)) {
			count();
			nextPersonFilter.run(person);
		}
	}

	/*
	 * -----------------------ABSTRACT METHODE-----------------------
	 */
	/**
	 * This function is called inside the function: void
	 * org.matsim.playground.filters.filter.PersonFilterA.run(Person person). It
	 * must be the first function called there, if the PersonFilterA is not a
	 * "FinalFilter".
	 * 
	 * This function must be implemented by subclass, inside which some special
	 * criteria will be provided.
	 * 
	 * @param person A person to be judged.
	 */
	public abstract boolean judge(Person person);
}
