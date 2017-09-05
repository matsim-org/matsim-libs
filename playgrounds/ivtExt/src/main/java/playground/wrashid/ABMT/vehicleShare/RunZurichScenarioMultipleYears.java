package playground.wrashid.ABMT.vehicleShare;

import java.io.File;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;

public class RunZurichScenarioMultipleYears {

	public static void main(String[] args) {
		
		String configFile = args[ 0 ];

		Config config = ConfigUtils.loadConfig(configFile);
		
		String originalOutputDir=config.getParam("controler", "outputDirectory");
		
		for (int i=0;i<=10;i++){
			GlobalTESFParameters.currentYear=i;
			
			config = ConfigUtils.loadConfig(configFile);
			String yearFolderName=originalOutputDir + "//year" + i+"//";
			File file=new File(yearFolderName);
			file.mkdir();
			
			config.setParam("controler", "outputDirectory", yearFolderName);
			
			ConfigWriter cw=new ConfigWriter(config);
			String newConfigFile=configFile + "_temp";
			cw.writeFileV2(newConfigFile);
			
			Controler controler=RunZurichScenario.startZHScenario(newConfigFile);
			
			controler.run();
		}
		
		
	}
	
}
