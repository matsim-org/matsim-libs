package playground.toronto;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.ConfigConsistencyCheckerImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * A basic class which opens a config.xml file using a JFileChooser and executes a MATSim run.
 * 
 * Defaults the Controler to overwriting folder files. 
 * 
 * @author pkucirek
 *
 */
public class RunToronto {

	private static final Logger log = Logger.getLogger(RunToronto.class);
	
	public static void main(String[] args) throws IOException{
		
		String file;
		if (args.length != 1){
			file = "";
			JFileChooser fc = new JFileChooser();
			fc.setFileFilter(new FileFilter() {
				@Override
				public String getDescription() {return "MATSim config file in *.xml format";}
				@Override
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".xml" );
				}
			});
			int state = fc.showOpenDialog(null);
			if (state == JFileChooser.APPROVE_OPTION){
				file = fc.getSelectedFile().getAbsolutePath();
			}else if (state == JFileChooser.CANCEL_OPTION) return;
			if (file == "" || file == null) return;	
		}else{
			file = args[0];
		}
		
		Config config = ConfigUtils.loadConfig(file);
		config.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());
		config.checkConsistency();
		Scenario scenario = ScenarioUtils.loadScenario(config);
	
		Controler controller = new Controler(scenario);
		controller.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		controller.run();
				
	}
}
