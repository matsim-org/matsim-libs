package playground.sergioo.GTFS;
import java.io.File;


public class GTFS2MATSim {

	public static void transformGTSF2MATSim(File root) {
		File fileStops = new File(root.getPath()+"/stops.txt");
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		transformGTSF2MATSim(new File("./data/buses"));
	}

}
