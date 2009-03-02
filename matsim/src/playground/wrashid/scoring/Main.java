package playground.wrashid.scoring;

import org.matsim.controler.Controler;

public class Main {
	public static void main(String[] args) {
		Controler controler = new Controler(args);
		CharyparScoringFunctionFactory factory=new CharyparScoringFunctionFactory();
		controler.setScoringFunctionFactory(factory);
		controler.setOverwriteFiles(true);
		controler.run();
	}
}
