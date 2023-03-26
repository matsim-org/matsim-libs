package org.matsim.simwrapper;

import org.matsim.simwrapper.viz.Viz;

@FunctionalInterface
public interface RowElement<T extends Viz> {

	void configure(T viz, Data data);


}
