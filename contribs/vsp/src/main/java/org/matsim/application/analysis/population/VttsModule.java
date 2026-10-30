package org.matsim.application.analysis.population;

import com.google.inject.Singleton;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.common.conventions.vsp.SubpopulationDefaultNames;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.population.PopulationUtils;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.simwrapper.dashboard.VttsDashboard;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Opt-in single-run VTTS calculation and dashboard. Install alongside {@link SimWrapperModule}.
 * Uses the run's existing scoring factory/parameters and installs no scoring overrides.
 * The initial integration supports the single-process experienced-plans service.
 */
public final class VttsModule extends AbstractModule {

	private final Options options;

	/**
	 * Select the VSP private-person subpopulation, identified by
	 * {@link SubpopulationDefaultNames#SUBPOP_PERSON}. Also include agents without a
	 * subpopulation attribute so that ordinary MATSim scenarios without this convention work.
	 * Other named populations, including commercial traffic, require an explicit filter.
	 */
	public VttsModule() {
		this(person -> {
			String subpopulation = PopulationUtils.getSubpopulation(person);
			return subpopulation == null || subpopulation.equals(SubpopulationDefaultNames.SUBPOP_PERSON);
		});
	}

	/**
	 * Supply a scenario-specific analysis population, for example when private persons have
	 * several regional subpopulation names. Rejected persons remain in the simulation and
	 * appear as excluded in the coverage export; filtering never changes income normalization.
	 */
	public VttsModule(Predicate<Person> personFilter) {
		options = new Options(Objects.requireNonNull(personFilter));
	}

	@Override
	public void install() {
		bind(Options.class).toInstance(options);
		// Resolve MUSE's factory and the conversion's person parameters from this controller.
		// A separate injector or a factory rebuilt from config could lose scenario scoring overrides.
		bind(MuseComputation.class);
		bind(VttsAnalysis.class);
		bind(VttsParameters.class).to(PersonScoringVttsParameters.class);
		// Event handling and final-iteration analysis must share one listener: the event handler
		// records stuck persons, and the controller callback uses that same set for exclusions.
		bind(VttsAnalysisListener.class).in(Singleton.class);
		addControllerListenerBinding().to(VttsAnalysisListener.class);
		addEventHandlerBinding().to(VttsAnalysisListener.class);
		// The listener produces CSVs; this dashboard reads them through SimWrapper's file references.
		SimWrapper.addDashboardBinding(binder()).toInstance(new VttsDashboard());
	}

	record Options(Predicate<Person> personFilter) { }
}
