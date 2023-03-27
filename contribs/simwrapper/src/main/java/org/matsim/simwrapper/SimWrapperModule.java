package org.matsim.simwrapper;

import org.matsim.core.controler.AbstractModule;

/**
 * Install the SimWrapper Extension into MATSim.
 */
public final class SimWrapperModule extends AbstractModule {

	private final SimWrapper simWrapper;

	/**
	 * Create module with existing simwrapper instance.
	 */
	public SimWrapperModule(SimWrapper simWrapper) {
		this.simWrapper = simWrapper;
	}

	/**
	 * Constructor with a newly initialized {@link SimWrapper} instance.
	 */
	public SimWrapperModule() {
		this(SimWrapper.create());
	}

	/**
	 * Get the {@link SimWrapper} instance.
	 */
	public SimWrapper getInstance() {
		return simWrapper;
	}

	@Override
	public void install() {
		bind(SimWrapper.class).toInstance(simWrapper);

		addControlerListenerBinding().to(SimWrapperListener.class);

	}

	// TODO: config group?
	// param set of shape files

}
