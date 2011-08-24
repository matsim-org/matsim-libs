package playground.florian.OTFVis;

import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFClientFile;


public class MviStarter {

	private static String config = "./test/input/playground/florian/Equil/config_mvi.xml";
	private static String mviFile = "./src/main/java/playground/florian/Equil/Output_mvi/ITERS/it.0/0.otfvis.mvi";
	private static String mviFile2 = "./src/main/java/playground/florian/Equil/Output_mvi/ITERS/it.100/100.otfvis.mvi";


	public static void main(String[] args) {
		Controler con = new Controler(config);
		con.setOverwriteFiles(true);
//		con.getConfig().setQSimConfigGroup(new QSimConfigGroup());
//		con.getConfig().getQSimConfigGroup().setSnapshotFormat("otfvis");
//		con.getConfig().getQSimConfigGroup().setSnapshotPeriod(60.0);
//		con.getConfig().getQSimConfigGroup().setSnapshotStyle("queue");
//		con.run();
		String[] movies = new String[2];
		movies[0] = mviFile;
		movies[1]= mviFile2;
//		OTFVis.playMVI(movies);
//		OTFDoubleMVI.main(movies);
		new OTFClientFile(mviFile).run();
	}

}
