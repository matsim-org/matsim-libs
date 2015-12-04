package playground.mzilske.teach;

import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

public class ControlerWithOTFVis {
	
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("examples/tutorial/config/example5-config.xml");
		Controler controler = new Controler(config);
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();
	}

}
