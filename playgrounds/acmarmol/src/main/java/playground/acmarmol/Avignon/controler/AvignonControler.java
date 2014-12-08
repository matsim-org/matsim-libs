package playground.acmarmol.Avignon.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.run.Controler;

public class AvignonControler {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String inputBase = "C:/local/marmolea/input/Avignon/";
		String configFile =  inputBase + "config_1.xml";
		
		Controler controler = new Controler(configFile) ;
		
		Scenario sc = controler.getScenario() ;
		Config cf = sc.getConfig() ;
		ConfigGroup aa = cf.getModule("planscalcroute");
		System.out.println(aa.getParams().toString());
		
		controler.setOverwriteFiles(true) ;
		controler.run() ;
		
		
		String dir = cf.controler().getOutputDirectory();
		System.out.println("Output is in " + dir + ".  Use otfvis (preferably hardware-accelerated) to play movies." ) ; 
		
		

	}

}
