package playground.yu.utils.qgis;

import org.matsim.core.gbl.Gbl;

import playground.yu.utils.qgis.Counts2QGIS.Counts2PolygonGraph;

public class CountsS2QGIS implements X2QGIS {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();
		String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		String countsFilenameBase = "../matsimTests/Calibration/counts/countsIVTCH.";
		for (int i = 0; i < 10; i++) {
			Counts2QGIS c2q = new Counts2QGIS();
			c2q.readNetwork(netFilename);
			c2q.setCrs(ch1903);
			c2q.setN2g(new Counts2PolygonGraph(c2q.getNetwork(), c2q.crs, c2q
					.readCounts(countsFilenameBase + i + ".xml")));
			c2q.writeShapeFile(countsFilenameBase + i + ".shp");
		}
		Gbl.printElapsedTime();
	}
}
