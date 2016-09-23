package matsimintegration;

import org.matsim.core.config.Config;

import floetteroed.utilities.Units;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TimeDiscretizationFactory {

	private TimeDiscretizationFactory() {
	}

	public static TimeDiscretization newInstance(final Config config) {
		final int startTime_s = 0;
		final int binSize_s = config.travelTimeCalculator().getTraveltimeBinSize();
		final int binCnt = (int) Math.ceil(Units.S_PER_D / binSize_s);
		return new TimeDiscretization(startTime_s, binSize_s, binCnt);
	}
	
}
