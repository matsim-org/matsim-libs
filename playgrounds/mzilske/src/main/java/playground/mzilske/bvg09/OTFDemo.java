
package playground.mzilske.bvg09;

import org.matsim.core.config.Config;
import org.matsim.pt.otfvis.FacilityDrawer;
import org.matsim.ptproject.qsim.QLink;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDataReader;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDataWriter;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDrawer;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsLayer;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsNoParkingHandler;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;

public class OTFDemo {

	public static void ptConnect(final String servername, Config config) {

		OTFConnectionManager connect = new OTFConnectionManager();
		connect.add(QLink.class, OTFLinkLanesAgentsNoParkingHandler.Writer.class);
		connect.add(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
		connect.add(OTFLinkLanesAgentsNoParkingHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connect.add(OTFLinkLanesAgentsNoParkingHandler.class,  AgentPointDrawer.class);
		connect.add(OTFLinkAgentsHandler.class,  AgentPointDrawer.class);
		connect.add(OTFAgentsListHandler.class,  AgentPointDrawer.class);
		connect.add(AgentPointDrawer.class, OGLAgentPointLayer.class);

		connect.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect.add(OTFLinkAgentsHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connect.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);

		connect.add(OTFDefaultLinkHandler.Writer.class, OTFDefaultLinkHandler.class);
		connect.add(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect.add(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);

		connect.add(FacilityDrawer.DataWriter_v1_0.class, FacilityDrawer.DataReader_v1_0.class);
		connect.add(FacilityDrawer.DataReader_v1_0.class, FacilityDrawer.DataDrawer.class);
		
//		connect.add(OTFTeleportAgentsDataWriter.class, OTFTeleportAgentsDataReader.class);
//		connect.add(OTFTeleportAgentsDataReader.class, OTFTeleportAgentsDrawer.class);
//		connect.add(OTFTeleportAgentsDrawer.class, OTFTeleportAgentsLayer.class);
		
		OTFClientLive client = new OTFClientLive("rmi:127.0.0.1:4019:" + servername, connect);
		client.setConfig(config.otfVis());
		client.start();

	}

}
