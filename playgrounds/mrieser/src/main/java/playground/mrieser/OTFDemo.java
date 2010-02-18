/* *********************************************************************** *
 * project: org.matsim.*
 * OTFDemo.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.mrieser;

import org.matsim.pt.otfvis.FacilityDrawer;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultNodeHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsNoParkingHandler;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;




public class OTFDemo {

	public static void main(final String[] args) {
		// WU base case
//		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/VSP/cvs/vsp-cvs/runs/run465/500.T.veh.gz", "/Volumes/Data/VSP/cvs/vsp-cvs/studies/schweiz-ivtch/network/ivtch-osm.xml"});

		// WU base case:
//		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/ETH/cvs/ivt/studies/switzerland/results/westumfahrung/run365/it.150/T.veh.gz", "/Volumes/Data/ETH/cvs/ivt/studies/switzerland/networks/ivtch-changed/network.xml"});

		// WU with new roads
//		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/ETH/cvs/ivt/studies/switzerland/results/westumfahrung/run370/it.220/T.veh.gz", "/Volumes/Data/ETH/cvs/ivt/studies/switzerland/networks/ivtch-changed-wu/network.xml"});

		// WU with new roads and with FlaMa
//		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/ETH/cvs/ivt/studies/switzerland/results/westumfahrung/run374/it.240/T.veh.gz", "/Volumes/Data/ETH/cvs/ivt/studies/switzerland/networks/ivtch-changed-wu-flama/network.xml"});

		//
//		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/VSP/runs/osm-swiss/200.T.veh.gz", "/Volumes/Data/VSP/svn/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch-osm.xml"});
//		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/VSP/cvs/vsp-cvs/runs/run616/it.550/550.events.mvi"});

		// other cases
//		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/VSP/runs/itm3/100.T.veh.gz", "/Volumes/Data/VSP/cvs/vsp-cvs/studies/schweiz-ivtch/network/ivtch-osm.xml"});
//		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/VSP/runs/gauteng2b/100.visualsizer.mvi"});
//		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/VSP/runs/gauteng-data/config.xml"});
//		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/VSP/runs/gauteng3/100.visualization.mvi"});

		//		org.matsim.run.OTFVis.main(new String[] {"-convert", "/Volumes/Data/VSP/runs/gauteng3/100.events.txt.gz", "/Volumes/Data/VSP/runs/gauteng-data/routes_network_1000mplus.xml.gz", "/Volumes/Data/VSP/runs/gauteng3/100.visualization.mvi", "300"});

		// BVG-Demo
//		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/VSP/cvs/vsp-cvs/runs/run487/it.500/500.events.mvi"});
//		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/VSP/cvs/vsp-cvs/runs/run628/it.500/500.events.mvi"});
//		org.matsim.run.OTFVis.main(new String[] {"/Users/cello/10.events.mvi"});
//		org.matsim.run.OTFVis.main(new String[] {"output/0.otfvis.mvi"});
//		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/VSP/talks/20081002_BVG/zurich/config.xml"});
//		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/VSP/talks/20081002_BVG/berlin/config.xml"});
//		org.matsim.run.OTFVis.main(new String[] {"../mystudies/osmnet/switzerland-20090313.xml"});
//		org.matsim.run.OTFVis.main(new String[] {"otfvis3.mvi"});
//		org.matsim.run.OTFVis.main(new String[] {"../mystudies/osmnet/zueri-20080410.xml"});
//		org.matsim.run.OTFVis.main(new String[] {"../mystudies/osmnet/zueri-20080410-full.xml"});

//		org.matsim.run.OTFVis.main(new String[] {"../shared-svn/studies/schweiz-ivtch/pt-experimental/config_otfvis.xml"});
//		org.matsim.run.OTFVis.main(new String[] {"test/scenarios/berlin/config.xml"});
//		org.matsim.run.OTFVis.main(new String[] {"test/input/playground/marcel/pt/config.xml"});

		org.matsim.run.OTFVis.main(new String[] {});

//		OTFDemo.ptConnect("OTFServer_Transit");
	}

	public static void ptConnect(final String servername) {

		OTFConnectionManager connect = new OTFConnectionManager();
		connect.connectWriterToReader(OTFDefaultLinkHandler.Writer.class, OTFDefaultLinkHandler.class);
		connect.connectWriterToReader(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect.connectWriterToReader(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
		connect.connectWriterToReader(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect.connectQLinkToWriter(OTFLinkLanesAgentsNoParkingHandler.Writer.class);
		connect.connectWriterToReader(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect.connectWriterToReader(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		connect.connectReaderToReceiver(OTFLinkAgentsHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connect.connectReaderToReceiver(OTFLinkLanesAgentsNoParkingHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connect.connectReceiverToLayer(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
		connect.connectReaderToReceiver(OTFLinkAgentsHandler.class,  AgentPointDrawer.class);
		connect.connectReaderToReceiver(OTFLinkLanesAgentsNoParkingHandler.class,  AgentPointDrawer.class);
		connect.connectWriterToReader(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);
		connect.connectReceiverToLayer(AgentPointDrawer.class, OGLAgentPointLayer.class);
		connect.connectReaderToReceiver(OTFAgentsListHandler.class,  AgentPointDrawer.class);

		connect.connectWriterToReader(FacilityDrawer.DataWriter_v1_0.class, FacilityDrawer.DataReader_v1_0.class);
		connect.connectReaderToReceiver(FacilityDrawer.DataReader_v1_0.class, FacilityDrawer.DataDrawer.class);

//		new OnTheFlyClientQuad("rmi:127.0.0.1:4019:OTFServer_Transit", connect).start();
		new OTFClientLive("rmi:127.0.0.1:4019:" + servername, connect).start();
//		new OnTheFlyClientQuad("rmi:127.0.0.1:4019", connect).start();



	}

}
