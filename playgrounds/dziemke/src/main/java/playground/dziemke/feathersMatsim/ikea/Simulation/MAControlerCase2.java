package playground.dziemke.feathersMatsim.ikea.Simulation;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;

public class MAControlerCase2 {

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("C:/Users/jeffw_000/Desktop/Dropbox/Uni/Master/Masterarbeit/MT/workspace new/ikeaStudy/input/Case2/configCase2_fc_4.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		Controler controler = new Controler(config);
		controler.run();
	}
}
