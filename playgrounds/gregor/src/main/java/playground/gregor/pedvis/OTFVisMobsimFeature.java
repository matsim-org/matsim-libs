package playground.gregor.pedvis;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.LinkedHashMap;

import javax.swing.SwingUtilities;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.config.groups.OTFVisConfigGroup;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
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
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleQuadDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleStaticNetLayer;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.VisData;
import org.matsim.vis.snapshotwriters.VisMobsim;

import playground.gregor.sim2d_v3.events.XYVxVyEvent;
import playground.gregor.sim2d_v3.events.XYVxVyEventsHandler;


public class OTFVisMobsimFeature implements XYVxVyEventsHandler, MobsimInitializedListener, MobsimAfterSimStepListener, MobsimBeforeCleanupListener,
AgentArrivalEventHandler {

	protected OnTheFlyServer server = null;
	private final VisMobsim queueSimulation;

	private OTFAgentsListHandler.Writer walk2dWriter;
	private final LinkedHashMap<Id, AgentSnapshotInfo> visData = new LinkedHashMap<Id, AgentSnapshotInfo>();
	private final EventsManager eventsManager;
	private Scenario scenario;

	public OTFVisMobsimFeature(VisMobsim queueSimulation, Scenario scenario, EventsManager eventsManager) {
		this.queueSimulation = queueSimulation;
		this.eventsManager = eventsManager;
		this.scenario = scenario;
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		this.server = OnTheFlyServer.createInstance(this.scenario, this.eventsManager);
		this.server.setSimulation(queueSimulation);


		this.walk2dWriter = new OTFAgentsListHandler.Writer();
		this.walk2dWriter.setSrc(new VisData() {

			@Override
			public Collection<AgentSnapshotInfo> getAgentSnapshotInfo(
					Collection<AgentSnapshotInfo> positions) {
				return visData.values();
			}
			
		});
		this.server.addAdditionalElement(this.walk2dWriter);

		final OTFConnectionManager connectionManager = new OTFConnectionManager();
		connectionManager.connectLinkToWriter(OTFLinkAgentsHandler.Writer.class);
		connectionManager.connectWriterToReader(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		connectionManager.connectReaderToReceiver(OTFLinkAgentsHandler.class, OGLSimpleQuadDrawer.class);
		connectionManager.connectReceiverToLayer(OGLSimpleQuadDrawer.class, OGLSimpleStaticNetLayer.class);
		connectionManager.connectWriterToReader(OTFAgentsListHandler.Writer.class, OTFAgentsListHandler.class);

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
				OTFClientControl.getInstance().setOTFVisConfig(visconf);
				OTFServerQuadTree serverQuadTree = OTFVisMobsimFeature.this.server.getQuad(connectionManager);
				OTFClientQuadTree clientQuadTree = serverQuadTree.convertToClient(OTFVisMobsimFeature.this.server, connectionManager);
				clientQuadTree.getConstData();
				OTFHostControlBar hostControlBar = otfClient.getHostControlBar();
				OTFOGLDrawer mainDrawer = new OTFOGLDrawer(clientQuadTree, hostControlBar, visconf);
				OTFQueryControl queryControl = new OTFQueryControl(OTFVisMobsimFeature.this.server, hostControlBar, visconf);
				OTFQueryControlToolBar queryControlBar = new OTFQueryControlToolBar(queryControl, visconf);
				queryControl.setQueryTextField(queryControlBar.getTextField());
				otfClient.getContentPane().add(queryControlBar, BorderLayout.SOUTH);
				mainDrawer.setQueryHandler(queryControl);
				otfClient.addDrawerAndInitialize(mainDrawer, saver);
				otfClient.show();
			}
		});
		this.server.pause();

	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		double time = e.getSimulationTime() ;
		this.server.unblockUpdates();
		this.server.updateStatus(time);

	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
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
	public void handleEvent(XYVxVyEvent event) {
		XYZAzimuthPositionInfo info = new XYZAzimuthPositionInfo(event.getPersonId(), event.getCoordinate(), event.getVX(), event.getVY(), event.getTime());
		this.visData.put(event.getPersonId(), info);
	}

}
