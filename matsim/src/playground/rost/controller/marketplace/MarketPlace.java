package playground.rost.controller.marketplace;

import java.util.Collection;

public interface MarketPlace<E> {
	/**
	 * @param element the element to add to the collection
	 * @return the id to access the element
	 */
	public String addElement(E element);

	public boolean removeElement(String id);
	
	public E getElement(String id);
	
	public Collection<String> getIds();
}
