package playground.wrashid.scoring;

import org.matsim.controler.Controler;

public class Main {
	public static void main(String[] args) {
		Controler controler = new Controler(args);
		ScoringFunctionAccumulatorFactory factory=new ScoringFunctionAccumulatorFactory();
		controler.setScoringFunctionFactory(factory);
		controler.setOverwriteFiles(true);
		controler.run();
	}
}
