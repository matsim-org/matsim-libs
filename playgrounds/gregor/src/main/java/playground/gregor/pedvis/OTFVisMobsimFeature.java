package playground.gregor.pedvis;

import java.awt.BorderLayout;
import java.util.LinkedHashMap;

import javax.swing.SwingUtilities;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.events.AdditionalTeleportationDepartureEvent;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.vis.otfvis.OTFClient;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.OnTheFlyServer;
import org.matsim.vis.otfvis.data.OTFClientQuadTree;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.data.fileio.SettingsSaver;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFQueryControl;
import org.matsim.vis.otfvis.gui.OTFQueryControlToolBar;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.layer.AgentPointDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleQuadDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleStaticNetLayer;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.VisMobsim;
import org.matsim.vis.snapshotwriters.VisMobsimFeature;

import playground.gregor.sim2d_v2.events.XYZAzimuthEvent;
import playground.gregor.sim2d_v2.events.XYZEventsHandler;

public class OTFVisMobsimFeature implements VisMobsimFeature,XYZEventsHandler{

	protected OnTheFlyServer server = null;
	private final VisMobsim queueSimulation;

	private OTFAgentsListHandler.Writer walk2dWriter;
	private final LinkedHashMap<Id, AgentSnapshotInfo> visData = new LinkedHashMap<Id, AgentSnapshotInfo>();

	public OTFVisMobsimFeature(VisMobsim queueSimulation) {
		this.queueSimulation = queueSimulation;
	}

	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		this.server = OnTheFlyServer.createInstance(this.queueSimulation.getEventsManager());
		this.server.setSimulation(this);


		this.walk2dWriter = new OTFAgentsListHandler.Writer();
		this.walk2dWriter.setSrc(this.visData.values());
		this.server.addAdditionalElement(this.walk2dWriter);

		final OTFConnectionManager connectionManager = new OTFConnectionManager();
		connectionManager.connectLinkToWriter(OTFLinkAgentsHandler.Writer.class);
		connectionManager.connectWriterToReader(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		connectionManager.connectReaderToReceiver(OTFLinkAgentsHandler.class, OGLSimpleQuadDrawer.class);
		connectionManager.connectReceiverToLayer(OGLSimpleQuadDrawer.class, OGLSimpleStaticNetLayer.class);
		connectionManager.connectReaderToReceiver(OTFLinkAgentsHandler.class, AgentPointDrawer.class);
		connectionManager.connectReceiverToLayer(AgentPointDrawer.class, OGLAgentPointLayer.class);
		connectionManager.connectWriterToReader(OTFAgentsListHandler.Writer.class, OTFAgentsListHandler.class);
		connectionManager.connectReaderToReceiver(OTFAgentsListHandler.class, AgentPointDrawer.class);

		//		connectionManager.connectWriterToReader(
		//				OTFAgentsListHandler.Writer.class,
		//				OTFAgentsListHandler.class);
		//		connectionManager.connectReaderToReceiver(
		//				OTFAgentsListHandler.class,
		//				AgentPointDrawer.class);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				OTFClient otfClient = new OTFClient();
				otfClient.setServer(OTFVisMobsimFeature.this.server);
				SettingsSaver saver = new SettingsSaver("otfsettings");
				OTFVisConfigGroup visconf = saver.tryToReadSettingsFile();
				if (visconf == null) {
					visconf = OTFVisMobsimFeature.this.server.getOTFVisConfig();
				}
				visconf.setCachingAllowed(false); // no use to cache in live mode
				OTFClientControl.getInstance().setOTFVisConfig(visconf);
				OTFServerQuadTree serverQuadTree = OTFVisMobsimFeature.this.server.getQuad(connectionManager);
				OTFClientQuadTree clientQuadTree = serverQuadTree.convertToClient(OTFVisMobsimFeature.this.server, connectionManager);
				clientQuadTree.createReceiver(connectionManager);
				clientQuadTree.getConstData();
				OTFHostControlBar hostControlBar = otfClient.getHostControlBar();
				hostControlBar.updateTimeLabel();
				OTFOGLDrawer mainDrawer = new OTFOGLDrawer(clientQuadTree, hostControlBar, visconf);
				OTFQueryControl queryControl = new OTFQueryControl(OTFVisMobsimFeature.this.server, hostControlBar, visconf);
				OTFQueryControlToolBar queryControlBar = new OTFQueryControlToolBar(queryControl, visconf);
				queryControl.setQueryTextField(queryControlBar.getTextField());
				otfClient.getFrame().getContentPane().add(queryControlBar, BorderLayout.SOUTH);
				mainDrawer.setQueryHandler(queryControl);
				otfClient.addDrawerAndInitialize(mainDrawer, saver);
				otfClient.show();
			}
		});
		this.server.pause();

	}

	@Override
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e) {
		double time = e.getSimulationTime() ;
		this.server.unblockUpdates();
		this.server.updateStatus(time);

	}

	@Override
	public void notifySimulationBeforeCleanup(SimulationBeforeCleanupEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.visData.remove(event.getPersonId());

	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(AdditionalTeleportationDepartureEvent eve) {
		// TODO Auto-generated method stub

	}

	@Override
	public VisMobsim getVisMobsim() {
		return this.queueSimulation;
	}

	@Override
	public Plan findPlan(Id agentId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addTrackedAgent(Id agentId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTrackedAgent(Id agentId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(XYZAzimuthEvent event) {
		XYZAzimuthPositionInfo info = new XYZAzimuthPositionInfo(event.getPersonId(), event.getCoordinate(), event.getVX(), event.getVY(), event.getTime());
		this.visData.put(event.getPersonId(), info);
	}

}
