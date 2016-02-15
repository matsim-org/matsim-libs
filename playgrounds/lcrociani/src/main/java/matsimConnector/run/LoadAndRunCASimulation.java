package matsimConnector.run;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import matsimConnector.scenarioGenerator.ScenarioGenerator;
import matsimConnector.utility.Constants;

public class LoadAndRunCASimulation {

	public static void main(String[] args) {
		reverseFile(new File(Constants.RESOURCE_PATH+"/"+Constants.ENVIRONMENT_FILE));
		if(args.length != 0 && Boolean.parseBoolean(args[0])){
			Constants.MARGINAL_SOCIAL_COST_OPTIMIZATION = Boolean.parseBoolean(args[0]);
			Constants.OUTPUT_PATH = Constants.DEBUG_TEST_PATH+"/outputSO";
			Constants.INPUT_PATH = Constants.DEBUG_TEST_PATH+"/inputSO";
		}else{
			Constants.OUTPUT_PATH = Constants.DEBUG_TEST_PATH+"/outputNE";
			Constants.INPUT_PATH = Constants.DEBUG_TEST_PATH+"/inputNE";
		}		
		
		Constants.ENVIRONMENT_FILE = "env_inverse.csv";
		
		ScenarioGenerator.main(new String[0]);
		CASimulationRunner.main(new String[0]);
	}
	
	public static void reverseFile(File envFile){
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(envFile));
			ArrayList<String> fileRows = new ArrayList<String>();
			String line = br.readLine();
			while(line!=null){
				fileRows.add(line+"\n");
				line = br.readLine();
				System.out.println(line);
			} 
			br.close();
			File file = new File(Constants.RESOURCE_PATH+"/env_inverse.csv");
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			for(int i=0;i<fileRows.size();i++){
				bw.write(fileRows.get(fileRows.size()-1-i));
			}		
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	

}
