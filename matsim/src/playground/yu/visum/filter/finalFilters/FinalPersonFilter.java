package playground.yu.visum.filter.finalFilters;

import org.matsim.core.population.PersonImpl;

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
	public boolean judge(PersonImpl person) {
		return false;
	}
}
