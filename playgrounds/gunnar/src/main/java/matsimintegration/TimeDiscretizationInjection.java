package matsimintegration;

import org.matsim.core.config.Config;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import floetteroed.utilities.Units;
import opdytsintegration.utils.TimeDiscretization;

/**
 * Throws Gunnar's TimeDiscretization into the MATSim/Guice machinery.
 * 
 * @author Gunnar Flötteröd
 *
 */
@Singleton
public class TimeDiscretizationInjection {

	private final TimeDiscretization timeDiscr;

	@Inject
	TimeDiscretizationInjection(final Config config) {
		final int startTime_s = 0;
		final int binSize_s = config.travelTimeCalculator().getTraveltimeBinSize();
		final int binCnt = (int) Math.ceil(Units.S_PER_D / binSize_s);
		this.timeDiscr = new TimeDiscretization(startTime_s, binSize_s, binCnt);
	}

	public TimeDiscretization getInstance() {
		return this.timeDiscr;
	}
}
