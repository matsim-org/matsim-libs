package playground.wrashid.scoring;

import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;

public class Main {
	public static void main(String[] args) {
		Controler controler = new Controler(args);
		CharyparNagelScoringFunctionFactory factory = new CharyparNagelScoringFunctionFactory(Gbl.getConfig().charyparNagelScoring());
		controler.setScoringFunctionFactory(factory);
		controler.setOverwriteFiles(true);
		controler.run();
	}
}
