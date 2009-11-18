/**
 * 
 */
package playground.yu.visum.filter;

/**
 * @author ychen
 */
public class Filter implements FilterI {

	/* -------------------MEMBER VARIABLE----------- */
	private int count = 0;

	/*
	 * -------------------NORMAL METHOD-------------
	 * 
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.playground.filters.FilterI#count()
	 */
	/**
	 * This function is called inside function: void
	 * org.matsim.playground.filters.filter.EventFilter.handleEvent(Event
	 * event) and void
	 * org.matsim.playground.filters.filter.EventFilter.handleEvent(Event
	 * event), if this Filter is not the last one.
	 */
	public void count() {
		count++;
	}

	/* ----------------GETTER--------------------- */
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.demandmodeling.filters.filter.FilterI#getCount()
	 */
	public int getCount() {
		return count;
	}

}
