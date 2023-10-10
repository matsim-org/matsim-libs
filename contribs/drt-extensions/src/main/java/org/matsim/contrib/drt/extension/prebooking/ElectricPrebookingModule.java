package org.matsim.contrib.drt.extension.prebooking;

/**
 * This class sets up all required bindings for DRT with prebooking if used with
 * edrt.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ElectricPrebookingModule extends PrebookingModule {
	public ElectricPrebookingModule() {
		super(true);
	}
}
