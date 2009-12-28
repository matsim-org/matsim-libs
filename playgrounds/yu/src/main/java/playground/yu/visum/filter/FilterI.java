/**
 * 
 */
package playground.yu.visum.filter;

/**
 * this interface offers the basic functions for
 * org.matsim.playground.filters.filter.Filter und its subclasses.
 * 
 * @author ychen
 * 
 */
public interface FilterI {
	/**
	 * Counts, how many persons (org.matsim.demandmodeling.plans.Person) or
	 * events(org.matsim.demandmodeling.events.Event) were selected
	 */
	void count();

	/**
	 * Returns how many persons (org.matsim.demandmodeling.plans.Person) or
	 * events(org.matsim.demandmodeling.events.Event) were selected
	 * @return how many persons (org.matsim.demandmodeling.plans.Person) or
	 * events(org.matsim.demandmodeling.events.Event) were selected
	 */
	int getCount();
}
