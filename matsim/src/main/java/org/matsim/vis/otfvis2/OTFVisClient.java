package org.matsim.vis.otfvis2;

import java.awt.BorderLayout;

import javax.swing.SwingUtilities;

import org.matsim.vis.otfvis.OTFClient;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SimpleSceneLayer;
import org.matsim.vis.otfvis.data.OTFClientQuadTree;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.gui.OTFSwingDrawerContainer;
import org.matsim.vis.otfvis.gui.SwingAgentDrawer;
import org.matsim.vis.otfvis.gui.SwingSimpleQuadDrawer;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFTimeLine;
import org.matsim.vis.otfvis.opengl.gui.SettingsSaver;
import org.matsim.vis.otfvis.opengl.layer.AgentPointDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleQuadDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleStaticNetLayer;

public final class OTFVisClient implements Runnable {

	private boolean swing = false;

	private OTFServerRemote server;

	private void createDrawer() {
		OTFClient otfClient = new OTFClient();
		otfClient.setServer(server);
		OTFConnectionManager connect = new OTFConnectionManager();
		connect.connectWriterToReader(LinkHandler.Writer.class, LinkHandler.class);
		connect.connectWriterToReader(OTFAgentsListHandler.Writer.class, OTFAgentsListHandler.class);
		if (swing) {
			connect.connectReaderToReceiver(LinkHandler.class, SwingSimpleQuadDrawer.class);
			connect.connectReaderToReceiver(OTFAgentsListHandler.class, SwingAgentDrawer.class);
			connect.connectReceiverToLayer(SwingSimpleQuadDrawer.class, SimpleSceneLayer.class);
			connect.connectReceiverToLayer(SwingAgentDrawer.class, SimpleSceneLayer.class);
		} else {
			connect.connectReaderToReceiver(OTFAgentsListHandler.class, AgentPointDrawer.class);
			connect.connectReaderToReceiver(LinkHandler.class,  OGLSimpleQuadDrawer.class);
			connect.connectReceiverToLayer(OGLSimpleQuadDrawer.class, OGLSimpleStaticNetLayer.class);		
			connect.connectReceiverToLayer(AgentPointDrawer.class, OGLAgentPointLayer.class);
		}
		OTFTimeLine timeLine = new OTFTimeLine("time", otfClient.getHostControlBar().getOTFHostControl());
		otfClient.getFrame().getContentPane().add(timeLine, BorderLayout.SOUTH);
		otfClient.getHostControlBar().addDrawer(timeLine);
		OTFServerQuadTree servQ = server.getQuad(connect);
		OTFClientQuadTree clientQuadTree = servQ.convertToClient(server, connect);
		clientQuadTree.createReceiver(connect);
		clientQuadTree.getConstData();
		otfClient.getHostControlBar().updateTimeLabel();
		OTFClientControl.getInstance().setOTFVisConfig(server.getOTFVisConfig());
		OTFDrawer mainDrawer;
		if (swing) {
			mainDrawer = new OTFSwingDrawerContainer(clientQuadTree, otfClient.getHostControlBar());
		} else {
			mainDrawer = new OTFOGLDrawer(clientQuadTree, otfClient.getHostControlBar());
		}
		if (server.isLive()) {
//			final OTFQueryControl queryControl = new OTFQueryControl(server, otfClient.getHostControlBar(), OTFClientControl.getInstance().getOTFVisConfig());
//			queryControl.getQueries().clear();
//			queryControl.getQueries().add(new QueryEntry("agentPlan", "show the current plan of an agent", QueryAgentPlan.class));
//			queryControl.setAgentIdResolver(new IdResolver() {
//
//				@Override
//				public List<String> resolveId(Double origRect) {
//					QueryAgentId.Result agentIdQuery = (QueryAgentId.Result) queryControl.createQuery(new QueryAgentId(origRect));
//					return agentIdQuery.agentIds;
//				}
//
//			});
//			OTFQueryControlToolBar queryControlBar = new OTFQueryControlToolBar(queryControl, OTFClientControl.getInstance().getOTFVisConfig());
//			queryControl.setQueryTextField(queryControlBar.getTextField());
//			otfClient.getFrame().getContentPane().add(queryControlBar, BorderLayout.SOUTH);
//			mainDrawer.setQueryHandler(queryControl);
		}
		
		otfClient.addDrawerAndInitialize(mainDrawer, new SettingsSaver("settings"));
		otfClient.show();
	}

	public void setSwing(boolean swing) {
		this.swing = swing;
	}

	@Override
	public void run() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				createDrawer();
			}
		});
	}

	public void setServer(OTFServerRemote server) {
		this.server = server;
	}

}
