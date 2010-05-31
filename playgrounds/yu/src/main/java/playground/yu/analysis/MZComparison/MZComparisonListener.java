/**
 * 
 */
package playground.yu.analysis.MZComparison;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerIO;
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
		mzcdi.readMZData(event.getControler().getConfig().findParam(
				"vspExperimental", "inputMZ05File"));
	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		int iter = event.getIteration();
		if (iter % 100 == 0) {

			Controler ctl = event.getControler();
			ControlerIO ctlIO = ctl.getControlerIO();
			Population pop = ctl.getPopulation();

			MZComparisonData mzcd = new MZComparisonData(ctl.getScenario()
					.getRoadPricingScheme());
			mzcd.run(pop);
			mzcdi.setData2Compare(mzcd);
			mzcdi.write(ctlIO.getIterationFilename(iter, "MZ05Comparison"));

			// GeometricDistanceExtractor lde = new
			// GeometricDistanceExtractor(ctl
			// .getRoadPricing().getRoadPricingScheme(), ctlIO
			// .getIterationFilename(iter, "geoDistKanton"));
			// lde.run(pop);
			// lde.write();
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
