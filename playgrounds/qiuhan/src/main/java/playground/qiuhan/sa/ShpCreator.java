package playground.qiuhan.sa;

import playground.yu.utils.qgis.MATSimNet2QGIS;
import playground.yu.utils.qgis.X2QGIS;

public class ShpCreator {
	public static void main(String[] args) {
		// uncleaned version
		// String networkFile = "output/matsimNetwork/networkBerlin2.xml", //
		// outputShapeFile = "output/matsimNetwork/networkBerlin2.shp";
		// a_nm version
		String networkFile = "input/A_NM/network.gz", //
		outputShapeFile = "output/matsimNetwork/A_NM_network.shp";

		MATSimNet2QGIS mn2q = new MATSimNet2QGIS(networkFile, X2QGIS.gk4);
		mn2q.writeShapeFile(outputShapeFile);

	}
}
