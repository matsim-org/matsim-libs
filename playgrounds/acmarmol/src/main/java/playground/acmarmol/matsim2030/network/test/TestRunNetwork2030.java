package playground.acmarmol.matsim2030.network.test;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;

import org.matsim.run.Controler;

public class TestRunNetwork2030 {
	
public static void main(String[] args) {
		
		String inputBase = "C:/local/marmolea/input/UVEK-Network2030/test/";
		String configFile =  inputBase + "config.xml";
		
		Controler controler = new Controler(configFile) ;
		
		Scenario sc = controler.getScenario() ;
		Config cf = sc.getConfig() ;
	
		controler.setOverwriteFiles(true) ;
		controler.run() ;
		
		
		String dir = cf.controler().getOutputDirectory();
		System.out.println("Output is in " + dir + ".  Use otfvis (preferably hardware-accelerated) to play movies." ) ; 
		
	}
}