package playground.wrashid.scoring;

import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;


public class Main {
	public static void main(String[] args) {
		Controler controler = new Controler(args);
		CharyparNagelScoringFunctionFactory factory = new CharyparNagelScoringFunctionFactory(Gbl.getConfig().charyparNagelScoring());
		controler.setScoringFunctionFactory(factory);
		controler.setOverwriteFiles(true);
		controler.run();
	}
}
