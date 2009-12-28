package playground.yu.visum.filter;

import org.matsim.api.core.v01.population.Person;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * @author yu
 */
public class PersonFilterAlgorithm extends AbstractPersonAlgorithm implements
		PersonFilterI {
	private PersonFilterI nextFilter = null;

	private int count = 0;

	/**
	 * @return the count.
	 */
	public int getCount() {
		return this.count;
	}

	@Override
	public void run(Person person) {
		count();
		this.nextFilter.run(person);
	}

	/**
	 * it's a virtual judge-function, all persons shall be allowed to pass or
	 * leave
	 * 
	 * @param person -
	 *            a person to be judge
	 * @return true if the Person meets the criterion
	 */
	public boolean judge(Person person) {
		return true;
	}

	public void count() {
		this.count++;
	}

	/**
	 * @param nextFilter -
	 *            The nextFilter to set.
	 */
	public void setNextFilter(PersonFilterI nextFilter) {
		this.nextFilter = nextFilter;
	}
}
