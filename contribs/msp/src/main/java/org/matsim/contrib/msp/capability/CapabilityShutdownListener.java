package org.matsim.contrib.msp.capability;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacilities;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacilityExtractor;
import org.matsim.contrib.msp.capability.prepare.CapabilityPersonFilter;
import org.matsim.contrib.msp.capability.prepare.DefaultCapabilityPersonFilter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.facilities.ActivityFacilities;

import java.nio.file.Path;

/**
 * Runs the {@link CapabilityAnalysis} once the run has finished.
 * <p>
 * The places for the discretionary activities have to be bound for this to do anything - see
 * {@link CapabilityModule#withPlaces}. Without them there is nothing to insert into the plans, so the
 * analysis is skipped with a warning rather than writing an empty file.
 * <p>
 * Which persons are analysed is up to {@link DefaultCapabilityPersonFilter}, without a study area: a run
 * does not know one, so the home location is not checked. Bind a {@link CapabilityPersonFilter} of your
 * own - one carrying a study area, for instance - to override that.
 *
 * @see CapabilityModule
 */
final class CapabilityShutdownListener implements ShutdownListener {

	private static final Logger log = LogManager.getLogger(CapabilityShutdownListener.class);

	@Inject
	private Scenario scenario;

	@Inject(optional = true)
	@CapabilityModule.CapabilityPlaces
	private ActivityFacilities places;

	@Inject(optional = true)
	private CapabilityPersonFilter filter;

	@Override
	public void notifyShutdown(ShutdownEvent event) {

		if (event.getException().isPresent()) {
			// the run crashed, so the plans are not meaningful
			return;
		}

		if (this.places == null) {
			log.warn("No places for discretionary activities are bound, so the capability analysis is skipped. Use "
				+ "CapabilityModule.withPlaces(...) to provide them.");
			return;
		}

		CapabilityConfigGroup config = ConfigUtils.addOrGetModule(this.scenario.getConfig(), CapabilityConfigGroup.class);

		DiscretionaryActivityFacilities places = new DiscretionaryActivityFacilityExtractor(config).extract(this.places);

		CapabilityPersonFilter personFilter = this.filter != null
			? this.filter
			: new DefaultCapabilityPersonFilter(config, null);

		new CapabilityAnalysis(config, personFilter)
			.run(this.scenario, places, Path.of(event.getServices().getControllerIO().getOutputPath()));
	}
}
