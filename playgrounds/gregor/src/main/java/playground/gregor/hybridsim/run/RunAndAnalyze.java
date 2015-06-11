package playground.gregor.hybridsim.run;

import playground.gregor.scenariogen.hybrid.Analyzer;
import playground.gregor.scenariogen.hybrid.Constants;
import playground.gregor.scenariogen.hybrid.ScenGen;

public class RunAndAnalyze {
	
	public static void main(String [] args) {
		Constants.EXP_90_DEG = true;
		ScenGen.main(args);
		RunMyHybridSim.main(args);
		
		Analyzer.main(args);
	}

}
