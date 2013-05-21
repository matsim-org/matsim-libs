package playground.dziemke.teach.mz;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

public class ControlerWithOTFVis {
	
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("examples/tutorial/config/example5-config.xml");
		Controler controler = new Controler(config);
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		controler.setOverwriteFiles(true);
		controler.run();
	}

}
