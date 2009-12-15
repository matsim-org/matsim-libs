package playground.anhorni.choiceSetGeneration.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;

	public class Test {
	
		private Controler controler = null;
		private final static Logger log = Logger.getLogger(Test.class);
		private String matsimRunConfigFile = null;
		
		public static void main(String[] args) {		
			// for the moment hard-coding
			String inputFile = "./input/input.txt";
			Test generator = new Test();
			generator.readInputFile(inputFile);
			generator.run();	
		}
		
		private void readInputFile(String inputFile) {
			try {
				FileReader fileReader = new FileReader(inputFile);
				BufferedReader bufferedReader = new BufferedReader(fileReader);

				this.matsimRunConfigFile = bufferedReader.readLine();				
				log.info("MATSim config file: " + this.matsimRunConfigFile);

				bufferedReader.close();
				fileReader.close();
			
			} catch (IOException e) {
				Gbl.errorMsg(e);
			}			
		}
				
		public void run() {			
			String configArgs [] = {this.matsimRunConfigFile};	
			Gbl.createConfig(configArgs);												
			this.controler = new Controler(this.matsimRunConfigFile);
			ExtractChoiceSetsRoutingTest listenerCar = new ExtractChoiceSetsRoutingTest(this.controler);
			controler.addControlerListener(listenerCar);
			controler.run();
		}
}
