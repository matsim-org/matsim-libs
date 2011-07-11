package playground.mzilske.osm;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.vis.otfvis.OTFClient;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SimpleSceneLayer;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.gui.OTFHostConnectionManager;
import org.matsim.vis.otfvis.gui.OTFSwingDrawerContainer;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.gui.SwingAgentDrawer;
import org.matsim.vis.otfvis.gui.SwingSimpleQuadDrawer;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFTimeLine;
import org.matsim.vis.otfvis.opengl.gui.SettingsSaver;
import org.matsim.vis.otfvis.opengl.gui.VisGUIMouseHandler;
import org.matsim.vis.otfvis.opengl.layer.AgentPointDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleQuadDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleStaticNetLayer;
import org.matsim.vis.otfvis2.LinkHandler;
import org.matsim.vis.otfvis2.OTFVisLiveServer;
import org.openstreetmap.gui.jmapviewer.JMapViewer;

public final class OTFVisClient implements Runnable {

	private boolean swing = false;

	private OTFClient otfClient = new OTFClient();

	private OTFHostConnectionManager masterHostControl;

	private OTFConnectionManager connect = new OTFConnectionManager();

	public OTFVisClient() {
		super();
	}

	double log2 (float scale) {
		return Math.log(scale) / Math.log(2);
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

	private void createDrawer(){
		OTFClientControl.getInstance().setOTFVisConfig(createOTFVisConfig());
		prepareConnectionManager();
		OTFTimeLine timeLine = new OTFTimeLine("time", otfClient.getHostControlBar().getOTFHostControl());
		otfClient.getFrame().getContentPane().add(timeLine, BorderLayout.SOUTH);
		otfClient.getHostControlBar().addDrawer(timeLine);
		final OTFDrawer mainDrawer;
		if (swing) {
			mainDrawer = new OTFSwingDrawerContainer(this.getRightDrawerComponent(), otfClient.getHostControlBar());
		} else {
			mainDrawer = new OTFOGLDrawer(this.getRightDrawerComponent(), otfClient.getHostControlBar());
		}
		otfClient.addDrawerAndInitialize(mainDrawer, new SettingsSaver(masterHostControl.getAddress()));

		final JPanel compositePanel = otfClient.getCompositePanel();
		final JMapViewer jMapViewer = new MyJMapViewer(compositePanel);

		final CoordinateTransformation coordinateTransformation = new WGS84ToOSMMercator.Deproject();


		compositePanel.add(jMapViewer);

		((OTFOGLDrawer) mainDrawer).addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				if (((OTFOGLDrawer) mainDrawer).getViewBounds() != null) {
					double x = ((OTFOGLDrawer) mainDrawer).getViewBounds().centerX + mainDrawer.getQuad().offsetEast;
					double y = ((OTFOGLDrawer) mainDrawer).getViewBounds().centerY + mainDrawer.getQuad().offsetNorth;
					Coord center = coordinateTransformation.transform(new CoordImpl(x,y));
					float scale = mainDrawer.getScale();
					int zoomDiff = (int) log2(scale);
					jMapViewer.setDisplayPositionByLatLon(center.getY(), center.getX(), WGS84ToOSMMercator.SCALE - zoomDiff);
					compositePanel.repaint();
				}
			}

		});
		otfClient.show();
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
				createDrawer();
			}
		});
	}

	public void setHostConnectionManager(OTFHostConnectionManager hostConnectionManager) {
		this.masterHostControl = hostConnectionManager;
		this.otfClient.setHostConnectionManager(hostConnectionManager);
	}

	public static final void playNetwork(final String filename) {
		VisGUIMouseHandler.ORTHO = true;
		OTFOGLDrawer.USE_GLJPANEL = true;
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(filename);
		EventsManager events = EventsUtils.createEventsManager();
		OTFVisLiveServer server = new OTFVisLiveServer(scenario, events);
		OTFHostConnectionManager hostConnectionManager = new OTFHostConnectionManager(filename, server);
		OTFVisClient client = new OTFVisClient();
		client.setHostConnectionManager(hostConnectionManager);
		client.setSwing(false);
		client.run();
		server.getSnapshotReceiver().finish();
	}

	public static void main(String[] args) {
		playNetwork("input/network.xml");
	}

}
