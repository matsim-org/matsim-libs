package testFunDiag;

import java.io.File;
import java.util.ArrayList;

import matsimConnector.run.CASimulationRunner;
import matsimConnector.scenarioGenerator.ScenarioGenerator;
import matsimConnector.utility.Constants;


public class Corridor1Dir {
	
	public static void cleanFolder(){
		try{
			File file = new File(Constants.FD_TEST_PATH+"fd_data.csv");
    		file.delete();
     	}catch(Exception e){
     		e.printStackTrace();
     	}
	}
	
	public static void main(String [] args){
		cleanFolder();
		double maxDensity = 1./Math.pow(Constants.CA_CELL_SIDE,2);
		double tic = 0.25;
		ArrayList<String> inputs = new ArrayList<String>();
		for (int i=1; i*tic<=maxDensity;i++)
			inputs.add(""+i*tic);
		//inputs.add("6");
		for(String input : inputs){
			String [] singleArg = {input};
			ScenarioGenerator.main(singleArg);
			CASimulationRunner.main(singleArg);
		}
	}
}
