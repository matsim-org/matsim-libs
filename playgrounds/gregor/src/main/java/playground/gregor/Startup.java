package playground.gregor;

import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

public class Startup {

	public static int SIZE = 10000000;

	public static void main(String [] args) {
		String [] argsII = {"/Users/laemmel/devel/burgdorf2d2/raw_input/raw_network2d_-1.xml","/Users/laemmel/devel/burgdorf2d2//tmp/networkL.shp","/Users/laemmel/devel/burgdorf2d2/tmp/networkP.shp","EPSG:21781"};
		Links2ESRIShape.main(argsII);
		
	}


}
