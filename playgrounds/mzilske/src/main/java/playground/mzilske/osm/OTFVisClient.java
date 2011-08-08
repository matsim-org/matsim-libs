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
import org.matsim.vis.otfvis.data.OTFClientQuadTree;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
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
import org.matsim.vis.otfvis2.LinkHandler;
import org.matsim.vis.otfvis2.OTFVisLiveServer;
import org.openstreetmap.gui.jmapviewer.JMapViewer;

public final class OTFVisClient implements Runnable {

	private boolean swing = false;

	private OTFClient otfClient = new OTFClient();

	private OTFServerRemote server;

	double log2 (double scale) {
		return Math.log(scale) / Math.log(2);
	}

	private void createDrawer() {
		otfClient.setServer(server);
		OTFConnectionManager connect = new OTFConnectionManager();
		OTFClientControl.getInstance().setOTFVisConfig(server.getOTFVisConfig());
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
		OTFHostControlBar hostControlBar = otfClient.getHostControlBar();
		OTFTimeLine timeLine = new OTFTimeLine("time", hostControlBar.getOTFHostControl());
		otfClient.getFrame().getContentPane().add(timeLine, BorderLayout.SOUTH);
		hostControlBar.addDrawer(timeLine);
		OTFServerQuadTree servQ = server.getQuad(connect);
		OTFClientQuadTree clientQ = servQ.convertToClient(server, connect);
		clientQ.createReceiver(connect);
		clientQ.getConstData();
		hostControlBar.updateTimeLabel();
		
		final OTFDrawer mainDrawer;
		if (swing) {
			mainDrawer = new OTFSwingDrawerContainer(clientQ, hostControlBar);
		} else {
			mainDrawer = new OTFOGLDrawer(clientQ, hostControlBar);
		}
		otfClient.addDrawerAndInitialize(mainDrawer, new SettingsSaver("settings"));

		final JPanel compositePanel = otfClient.getCompositePanel();
		final JMapViewer jMapViewer = new MyJMapViewer(compositePanel);
		compositePanel.add(jMapViewer);
		
		final CoordinateTransformation coordinateTransformation = new WGS84ToOSMMercator.Deproject();

		((OTFOGLDrawer) mainDrawer).addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				if (((OTFOGLDrawer) mainDrawer).getViewBounds() != null) {
					double x = ((OTFOGLDrawer) mainDrawer).getViewBounds().centerX + mainDrawer.getQuad().offsetEast;
					double y = ((OTFOGLDrawer) mainDrawer).getViewBounds().centerY + mainDrawer.getQuad().offsetNorth;
					Coord center = coordinateTransformation.transform(new CoordImpl(x,y));
					double scale = mainDrawer.getScale();
					int zoomDiff = (int) log2(scale);
					jMapViewer.setDisplayPositionByLatLon(center.getY(), center.getX(), WGS84ToOSMMercator.SCALE - zoomDiff);
					compositePanel.repaint();
				}
			}

		});
		otfClient.show();
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

	public void setServer(OTFServerRemote server) {
		this.server = server;
	}

	public static final void playNetwork(final String filename) {
		OTFOGLDrawer.USE_GLJPANEL = true;
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(filename);
		EventsManager events = EventsUtils.createEventsManager();
		OTFVisLiveServer server = new OTFVisLiveServer(scenario, events);
		OTFVisClient client = new OTFVisClient();
		client.setServer(server);
		client.setSwing(false);
		client.run();
		server.getSnapshotReceiver().finish();
	}

	public static void main(String[] args) {
		playNetwork("input/network.xml");
	}

}
