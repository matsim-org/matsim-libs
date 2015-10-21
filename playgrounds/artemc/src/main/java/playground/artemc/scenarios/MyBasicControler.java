package playground.artemc.scenarios;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.run.Controler;

/**
 * Created by artemc on 21/9/15.
 */
public class MyBasicControler {

	public static void main(String[] args) {
		Controler controler = new Controler("/Users/artemc/Workspace/matsim-git/matsim/examples/equil/config.xml");
// controler.setOverwriteFiles(true);
		controler.run();
	}
}


