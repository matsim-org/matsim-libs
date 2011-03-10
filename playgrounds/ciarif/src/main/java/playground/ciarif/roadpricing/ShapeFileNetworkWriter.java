package playground.ciarif.roadpricing;
	/* *********************************************************************** *
	 * project: org.matsim.*
	 *                                                                         *
	 * *********************************************************************** *
	 *                                                                         *
	 * copyright       : (C) 2007 by the members listed in the COPYING,        *
	 *                   LICENSE and WARRANTY file.                            *
	 * email           : info at matsim dot org                                *
	 *                                                                         *
	 * *********************************************************************** *
	 *                                                                         *
	 *   This program is free software; you can redistribute it and/or modify  *
	 *   it under the terms of the GNU General Public License as published by  *
	 *   the Free Software Foundation; either version 2 of the License, or     *
	 *   (at your option) any later version.                                   *
	 *   See also COPYING, LICENSE and WARRANTY file                           *
	 *                                                                         *
	 * *********************************************************************** */


		import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;


	/**
	 * @author dgrether
	 *
	 */
	public class ShapeFileNetworkWriter {
		
		String base = "/Volumes/data/work/cvsRep/vsp-cvs/runs/run";
		 
		static String network = "/Volumes/data/work/vspSvn/studies/schweiz-ivtch/baseCase/network/ivtch-osm.xml";

		static String outputNetwork = "/Volumes/data/work/vspSvn/studies/schweiz-ivtch/baseCase/network/ivtch-osm.shp";
		
		public ShapeFileNetworkWriter() {
			
		}
		
		public void writeNetwork(String network, String outfile) {
			ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
			NetworkImpl net = scenario.getNetwork();
			MatsimNetworkReader reader = new MatsimNetworkReader(scenario);
			reader.readFile(network);
			writeNetwork(net, outfile);
		}
		
		
		public void writeNetwork(NetworkImpl network, String outfile) {
			//uncomment the following lines to let the tool work
//			MATSimNet2QGIS mn2q = new MATSimNet2QGIS();
//			MATSimNet2QGIS.setFlowCapFactor(0.1);
//			mn2q.setNetwork(network);
//			mn2q.setCrs(X2QGIS.ch1903);
//			mn2q.writeShapeFile(outfile);
//			System.out.println("Network written to " + outfile);
		}
		
		
		public static void main(String[] args) {
			
			new ShapeFileNetworkWriter().writeNetwork(network, outputNetwork);
		}
		

}
