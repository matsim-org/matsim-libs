package playground.florian.OTFVis.tests;

import org.matsim.run.OTFVis;
import org.matsim.vis.otfvis.OTFClientFile;


public class T7_MVI_ConvertEventNetwork {

	
	private static String event = "./Output/OTFVisTests/QSim/ITERS/it.1/1.events.txt.gz";
	private static String network = "./Output/OTFVisTests/QSim/output_network.xml.gz";
	private static String mviFile = "./Output/OTFVisTests/QSim/OTFVis.mvi";
	
	public static void main(String[] args) {
		String[] files = new String[5];
		files[1] = event;
		files[2] = network;
		files[3] = mviFile;
		files[4] = "60";
		OTFVis.convert(files);
		new OTFClientFile(files[3]).start();		
	}
}
