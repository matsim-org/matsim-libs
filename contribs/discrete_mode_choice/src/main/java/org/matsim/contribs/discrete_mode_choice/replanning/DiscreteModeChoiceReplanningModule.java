package org.matsim.contribs.discrete_mode_choice.replanning;

import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;

import com.google.inject.Provider;

/**
 * This replanning module creates new instances of the
 * DiscreteModeChoiceAlgorithm.
 * 
 * @author sebhoerl
 */
public class DiscreteModeChoiceReplanningModule extends AbstractMultithreadedModule {
	public static final String NAME = "DiscreteModeChoice";

	final private Provider<DiscreteModeChoiceModel> modelProvider;
	final private Provider<TripListConverter> converterProvider;

	final private PopulationFactory populationFactory;

	public DiscreteModeChoiceReplanningModule(GlobalConfigGroup globalConfigGroup,
			Provider<DiscreteModeChoiceModel> modeChoiceModelProvider, Provider<TripListConverter> converterProvider,
			PopulationFactory populationFactory) {
		super(globalConfigGroup);

		this.modelProvider = modeChoiceModelProvider;
		this.converterProvider = converterProvider;
		this.populationFactory = populationFactory;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		DiscreteModeChoiceModel choiceModel = modelProvider.get();
		TripListConverter converter = converterProvider.get();

		return new DiscreteModeChoiceAlgorithm(MatsimRandom.getLocalInstance(), choiceModel, populationFactory,
				converter);
	}
}
