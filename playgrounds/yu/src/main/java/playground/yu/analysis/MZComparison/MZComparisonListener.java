/**
 * 
 */
package playground.yu.analysis.MZComparison;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;

/**
 * @author yu
 * 
 */
public class MZComparisonListener implements IterationEndsListener,
		StartupListener {
	private MZComparisonDataIO mzcdi = new MZComparisonDataIO();

	public void notifyStartup(StartupEvent event) {
		mzcdi.readMZData(event.getControler().getConfig().findParam("mZ05",
				"inputMZ05File"));
	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		int iter = event.getIteration();
		if (iter % 100 == 0) {
			Controler ctl = event.getControler();
			MZComparisonData mzcd = new MZComparisonData(ctl.getScenario()
					.getRoadPricingScheme());
			mzcd.run(ctl.getPopulation());

			mzcdi.setData2Compare(mzcd);
			mzcdi.write(ctl.getControlerIO().getIterationFilename(iter,
					"MZ05Comparison"));
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
