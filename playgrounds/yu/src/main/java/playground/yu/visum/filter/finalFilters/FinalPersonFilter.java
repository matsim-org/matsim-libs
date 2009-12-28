package playground.yu.visum.filter.finalFilters;

import org.matsim.api.core.v01.population.Person;

import playground.yu.visum.filter.PersonFilterA;

/**
 * @author ychen
 * 
 */
public class FinalPersonFilter extends PersonFilterA {
	/**
	 * does nothing
	 * 
	 * @param person -
	 *            a person transfered from another PersonFilter
	 */
	@Override
	public boolean judge(Person person) {
		return false;
	}
}
