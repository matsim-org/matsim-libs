package playground.gregor;

import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

public class Startup {


	public static void main(String [] args) {

		String [] argsII = {"/Users/laemmel/devel/dfg/input/network.xml","/Users/laemmel/devel/dfg/raw_input/networkL.shp","/Users/laemmel/devel/dfg/raw_input/networkP.shp","EPSG:3395"};
		Links2ESRIShape.main(argsII);


	}

}
