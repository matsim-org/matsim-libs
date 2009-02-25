package playground.yu.visum.filter.finalFilters;

import org.matsim.interfaces.core.v01.Person;

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
