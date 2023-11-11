package org.matsim.contrib.drt.extension.prebooking;

import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.prebooking.dvrp.PrebookingModeModule;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.controler.AbstractModule;

import com.google.common.base.Verify;

/**
 * This class sets up all required bindings for DRT with prebooking. It should
 * be added using addOverridingModule to the controler *after* the standard DRT
 * has been set up.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class PrebookingModule extends AbstractModule {
	public PrebookingModule() {
		this(false);
	}

	private final boolean isElectric;

	PrebookingModule(boolean isElectric) {
		this.isElectric = isElectric;
	}

	@Override
	public void install() {
		MultiModeDrtConfigGroup drtConfig = MultiModeDrtConfigGroup.get(getConfig());

		for (DrtConfigGroup modeConfig : drtConfig.getModalElements()) {
			if (modeConfig instanceof DrtWithExtensionsConfigGroup) {
				DrtWithExtensionsConfigGroup extensionsConfig = (DrtWithExtensionsConfigGroup) modeConfig;

				if (extensionsConfig.getDrtPrebookingParams().isPresent()) {
					install(new PrebookingModeModule(modeConfig.getMode(), isElectric));
				}
			}
		}
	}
}
