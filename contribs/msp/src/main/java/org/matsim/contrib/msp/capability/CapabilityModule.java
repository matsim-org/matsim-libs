package org.matsim.contrib.msp.capability;

import com.google.inject.BindingAnnotation;
import org.matsim.contrib.msp.capability.prepare.DiscretionaryActivityFacilityCsv;
import org.matsim.core.controler.AbstractModule;
import org.matsim.facilities.ActivityFacilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Chains the {@link CapabilityAnalysis} to the end of a MATSim run:
 * <pre>
 *   ActivityFacilities places = new DiscretionaryActivityFacilityCsv(capabilityConfig).read("places.csv");
 *   controler.addOverridingModule(CapabilityModule.withPlaces(places));
 * </pre>
 * The places are kept separate from the scenario's own facilities on purpose, see
 * {@link RunCapabilityAnalysis}. The analysis lands in the output directory of the run.
 */
public final class CapabilityModule extends AbstractModule {

	private final ActivityFacilities places;

	public CapabilityModule() {
		this(null);
	}

	private CapabilityModule(ActivityFacilities places) {
		this.places = places;
	}

	/**
	 * @param places the facilities of the discretionary activities, e.g. read with
	 * {@link DiscretionaryActivityFacilityCsv}. They are snapped to the network of the run before the analysis.
	 */
	public static CapabilityModule withPlaces(ActivityFacilities places) {
		return new CapabilityModule(places);
	}

	@Override
	public void install() {
		if (this.places != null) {
			bind(ActivityFacilities.class).annotatedWith(CapabilityPlaces.class).toInstance(this.places);
		}
		addControllerListenerBinding().to(CapabilityShutdownListener.class);
	}

	/**
	 * Marks the facilities of the discretionary activities, to tell them from the scenario's own.
	 */
	@BindingAnnotation
	@Retention(RetentionPolicy.RUNTIME)
	public @interface CapabilityPlaces {
	}
}
