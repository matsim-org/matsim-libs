package playground.gregor;

import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

public class Startup {

	public static int SIZE = 10000000;

	public static void main(String [] args) {
		String [] argsII = {"/Users/laemmel/devel/burgdorf2d/input/sim2d_network_env1.xml.gz","/Users/laemmel/tmp/vis/networkL.shp","/Users/laemmel/tmp/vis/networkP.shp","EPSG:3395"};
		Links2ESRIShape.main(argsII);
		
	}


}
