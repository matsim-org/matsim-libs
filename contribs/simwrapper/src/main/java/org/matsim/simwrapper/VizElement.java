package org.matsim.simwrapper;

import org.matsim.simwrapper.viz.Viz;

/**
 * Configure one single viz element.
 *
 * @param <T> type of the element
 */
@FunctionalInterface
public interface VizElement<T extends Viz> {

	/**
	 * Method called in order to configure the viz element and retrieve needed data paths.
	 */
	void configure(T viz, Data data);

}
