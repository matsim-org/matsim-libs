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
public class MZComparisonListener implements IterationEndsListener, StartupListener {
	private MZComparisonDataIO mzcdi = new MZComparisonDataIO();

	public void notifyStartup(StartupEvent event) {
		mzcdi.readMZData(event.getControler().getConfig().findParam("mZ05",
				"inputMZ05File"));
	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		if (event.getIteration() % 100 == 0) {
			MZComparisonData mzcd = new MZComparisonData(event.getControler().getScenario()
					.getRoadPricingScheme());
			mzcd.run(event.getControler().getPopulation());

			mzcdi.setData2Compare(mzcd);
			event.getControler();
			mzcdi.write(event.getControler().getControlerIO().getIterationFilename(event.getControler().getIterationNumber(), "MZ05Comparison"));
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
