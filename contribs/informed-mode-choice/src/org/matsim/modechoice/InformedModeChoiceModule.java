package org.matsim.modechoice;

import org.matsim.core.controler.AbstractModule;

/**
 *
 */
public final class InformedModeChoiceModule extends AbstractModule {

	private InformedModeChoiceModule() {
	}

	@Override
	public void install() {

	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static final class Builder {

		public InformedModeChoiceModule build() {
			return new InformedModeChoiceModule();
		}

	}
}
