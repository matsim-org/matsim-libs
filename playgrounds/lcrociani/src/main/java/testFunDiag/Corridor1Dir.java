package testFunDiag;

import java.util.ArrayList;

import matsimConnector.run.CASimulationRunner;
import matsimConnector.scenarioGenerator.ScenarioGenerator;


public class Corridor1Dir {
	
	public static void main(String [] args){
		double maxDensity = 6.25;
		double tic = 0.25;
		ArrayList<String> inputs = new ArrayList<String>();
		for (int i=1; i*tic<=maxDensity;i++)
			inputs.add(""+i*tic);
		for(String input : inputs){
			String [] singleArg = {input};
			ScenarioGenerator.main(singleArg);
			CASimulationRunner.main(singleArg);
		}
	}
}
