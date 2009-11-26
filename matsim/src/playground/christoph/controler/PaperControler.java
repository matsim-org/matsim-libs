package playground.christoph.controler;

import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.CharyparNagelOpenTimesScoringFunctionFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;

/*
 * Uses a Scoring Function that gets the Facilities Opening Times from 
 * the Facilities instead of the Config File.
 */
public class PaperControler extends Controler{

	public PaperControler(String[] args)
	{
		super(args);
	}

	@Override
	protected ScoringFunctionFactory loadScoringFunctionFactory()
	{
		return new CharyparNagelOpenTimesScoringFunctionFactory(this.config.charyparNagelScoring());
//		return new CharyparNagelScoringFunctionFactory(this.config.charyparNagelScoring());
	}

	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final PaperControler controler = new PaperControler(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}
}
