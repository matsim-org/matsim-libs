package org.matsim.api.core.v01;

/**
 * Interface for arbitrarily serializable and exchangeable messages.
 */
public interface Message {

	/**
	 * Reserved type that indicates any arbitrary event.
	 */
	int ANY_TYPE = Integer.MIN_VALUE;

	default int getType() {
		return getClass().getName().hashCode();
	}

}
