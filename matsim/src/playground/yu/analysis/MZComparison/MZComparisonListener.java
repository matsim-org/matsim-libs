/**
 * 
 */
package playground.yu.analysis.MZComparison;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;

/**
 * @author yu
 * 
 */
public class MZComparisonListener implements IterationStartsListener,
		IterationEndsListener, StartupListener {
	private MZComparisonDataIO mzcdi = new MZComparisonDataIO();
	private MZComparisonData mzcd = null;

	public void notifyStartup(StartupEvent event) {
		mzcdi.readMZData(event.getControler().getConfig().findParam("mZ05",
				"inputMZ05File"));
	}

	public void notifyIterationStarts(IterationStartsEvent event) {
		if (event.getIteration() % 10 == 0) {
			Controler ctl = event.getControler();
			mzcd = new MZComparisonData(ctl.getRoadPricing()
					.getRoadPricingScheme());
			ctl.getPopulation().addAlgorithm(mzcd);
		}
	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		if (event.getIteration() % 10 == 0) {
			event.getControler().getPopulation().runAlgorithms();
			mzcdi.setData2Compare(mzcd);
			event.getControler();
			mzcdi.write(Controler.getIterationFilename("MZ05Comparison"));
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final Controler controler = new Controler(args);
		controler.addControlerListener(new MZComparisonListener());
		controler.run();
	}
}
