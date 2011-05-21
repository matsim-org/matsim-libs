package org.matsim.vis.otfvis2;

import java.awt.BorderLayout;
import java.awt.geom.Rectangle2D.Double;
import java.util.List;

import javax.swing.SwingUtilities;

import org.matsim.vis.otfvis.OTFClient;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SimpleSceneLayer;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.gui.OTFHostConnectionManager;
import org.matsim.vis.otfvis.gui.OTFQueryControl;
import org.matsim.vis.otfvis.gui.OTFQueryControl.IdResolver;
import org.matsim.vis.otfvis.gui.OTFQueryControlToolBar;
import org.matsim.vis.otfvis.gui.OTFSwingDrawerContainer;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.gui.QueryEntry;
import org.matsim.vis.otfvis.gui.SwingAgentDrawer;
import org.matsim.vis.otfvis.gui.SwingSimpleQuadDrawer;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFTimeLine;
import org.matsim.vis.otfvis.opengl.gui.SettingsSaver;
import org.matsim.vis.otfvis.opengl.layer.AgentPointDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleQuadDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleStaticNetLayer;

public final class OTFVisClient implements Runnable {

	private boolean swing = false;

	private OTFClient otfClient = new OTFClient();

	private OTFHostConnectionManager masterHostControl;

	private OTFConnectionManager connect = new OTFConnectionManager();

	public OTFVisClient() {
		super();
	}

	private void prepareConnectionManager() {
		this.connect.connectWriterToReader(LinkHandler.Writer.class, LinkHandler.class);
		this.connect.connectWriterToReader(OTFAgentsListHandler.Writer.class, OTFAgentsListHandler.class);
		if (swing) {
			this.connect.connectReaderToReceiver(LinkHandler.class, SwingSimpleQuadDrawer.class);
			this.connect.connectReaderToReceiver(OTFAgentsListHandler.class, SwingAgentDrawer.class);
			this.connect.connectReceiverToLayer(SwingSimpleQuadDrawer.class, SimpleSceneLayer.class);
			this.connect.connectReceiverToLayer(SwingAgentDrawer.class, SimpleSceneLayer.class);
		} else {
			this.connect.connectReaderToReceiver(OTFAgentsListHandler.class, AgentPointDrawer.class);
			this.connect.connectReaderToReceiver(LinkHandler.class,  OGLSimpleQuadDrawer.class);
			this.connect.connectReceiverToLayer(OGLSimpleQuadDrawer.class, OGLSimpleStaticNetLayer.class);		
			this.connect.connectReceiverToLayer(AgentPointDrawer.class, OGLAgentPointLayer.class);
		}
	}

	private OTFClientQuad getRightDrawerComponent() {
		OTFConnectionManager connectR = this.connect.clone();
		OTFClientQuad clientQ2 = otfClient.createNewView(connectR);
		return clientQ2;
	}

	private OTFDrawer createDrawer(){
		prepareConnectionManager();
		OTFTimeLine timeLine = new OTFTimeLine("time", otfClient.getHostControlBar().getOTFHostControl());
		otfClient.getFrame().getContentPane().add(timeLine, BorderLayout.SOUTH);
		otfClient.getHostControlBar().addDrawer(timeLine);
		OTFDrawer mainDrawer;
		if (swing) {
			mainDrawer = new OTFSwingDrawerContainer(this.getRightDrawerComponent(), otfClient.getHostControlBar());
		} else {
			mainDrawer = new OTFOGLDrawer(this.getRightDrawerComponent(), otfClient.getHostControlBar());
		}
		if (masterHostControl.getOTFServer().isLive()) {
			final OTFQueryControl queryControl = new OTFQueryControl(otfClient.getHostControlBar(), OTFClientControl.getInstance().getOTFVisConfig());
			queryControl.getQueries().clear();
			queryControl.getQueries().add(new QueryEntry("agentPlan", "show the current plan of an agent", QueryAgentPlan.class));
			queryControl.setAgentIdResolver(new IdResolver() {

				@Override
				public List<String> resolveId(Double origRect) {
					QueryAgentId.Result agentIdQuery = (QueryAgentId.Result) queryControl.createQuery(new QueryAgentId(origRect));
					return agentIdQuery.agentIds;
				}

			});
			OTFQueryControlToolBar queryControlBar = new OTFQueryControlToolBar(queryControl, OTFClientControl.getInstance().getOTFVisConfig());
			queryControl.setQueryTextField(queryControlBar.getTextField());
			otfClient.getFrame().getContentPane().add(queryControlBar, BorderLayout.SOUTH);
			mainDrawer.setQueryHandler(queryControl);
		}
		return mainDrawer;
	}

	private OTFVisConfigGroup createOTFVisConfig() {
		return this.masterHostControl.getOTFServer().getOTFVisConfig();
	}

	public void setSwing(boolean swing) {
		this.swing = swing;
	}

	@Override
	public final void run() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				OTFClientControl.getInstance().setOTFVisConfig(createOTFVisConfig());
				otfClient.addDrawerAndInitialize(createDrawer(), new SettingsSaver(masterHostControl.getAddress()));
				otfClient.show();
			}
		});
	}

	public void setHostConnectionManager(OTFHostConnectionManager hostConnectionManager) {
		this.masterHostControl = hostConnectionManager;
		this.otfClient.setHostConnectionManager(hostConnectionManager);
	}

}
