package playground.agarwalamit.patnaIndia;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

public class MyFirstControler {

	public static void main(String[] args) {
		
		String configFile = //args[0];
				"./patnaOutput/configMixTrfc.xml";
		Config config = ConfigUtils.loadConfig(configFile);
		final Controler myController = new Controler(config);		
		myController.setOverwriteFiles(true) ;
		myController.setCreateGraphs(true);
		myController.setMobsimFactory(new PatnaQSimFactory()); 
//		myController.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		myController.setDumpDataAtEnd(true);
		myController.run();
	}
}