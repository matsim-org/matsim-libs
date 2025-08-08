package org.matsim.core.scoring;

import com.google.inject.name.Names;
import jakarta.inject.Singleton;
import org.matsim.core.config.groups.TasteVariationsConfigParameterSet;
import org.matsim.core.controler.AbstractModule;

/**
 * This class is a Guice module for the {@link PseudoRandomScorer}.
 */
public class PseudoRandomScoringModule extends AbstractModule {

	/**
	 * Constant for binding the distribution configuration.
	 */
	static final String TRIP = "trip";

	private final DistributionConfig tripConfig;

	public PseudoRandomScoringModule(TasteVariationsConfigParameterSet.VariationType tripDistribution, double tripScale) {
		this.tripConfig = new DistributionConfig(tripDistribution, tripScale);
	}

	@Override
	public void install() {

		bind(PseudoRandomTripError.class).to(DefaultPseudoRandomTripError.class).in(Singleton.class);
		bind(PseudoRandomScorer.class).in(Singleton.class);

		bind(DistributionConfig.class).annotatedWith(Names.named(TRIP)).toInstance(tripConfig);
	}

}
