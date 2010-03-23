package playground.florian.OTFVis.tests;

import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;

public class StartControler {

	private static String config = "./test/input/playground/florian/OTFVis/config.xml";
	
	public static void main(String[] args) {
		Controler con = new Controler(config);
		con.setOverwriteFiles(true);
		con.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		con.getConfig().getQSimConfigGroup().setSnapshotFormat("otfvis");
		con.getConfig().getQSimConfigGroup().setSnapshotPeriod(60.0);
		con.getConfig().getQSimConfigGroup().setSnapshotStyle("queue");
		con.run();
	}

}
