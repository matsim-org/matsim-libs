package playground.dziemke.feathersMatsim.ikea.Simulation;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;

public class MAControlerCase1 {

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("C:/Users/jeffw_000/Desktop/Dropbox/Uni/Master/Masterarbeit/MT/workspace new/ikeaStudy/input/configCase1.xml");
///		Config config = ConfigUtils.loadConfig("../../../../SVN/shared-svn/projects/hasselt/jeff/input/Case1/configCase1_adapted.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		Controler controler = new Controler(config);
		controler.run();
	}
}