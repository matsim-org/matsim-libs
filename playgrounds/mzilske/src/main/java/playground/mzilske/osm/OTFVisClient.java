package playground.mzilske.osm;

import java.awt.BorderLayout;
import java.awt.geom.Rectangle2D.Double;
import java.io.File;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
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
import org.matsim.vis.otfvis2.LinkHandler;
import org.matsim.vis.otfvis2.OTFVisLiveServer;
import org.matsim.vis.otfvis2.QueryAgentId;
import org.matsim.vis.otfvis2.QueryAgentPlan;

public final class OTFVisClient extends OTFClient {

	private boolean swing = false;
	
	private OTFConnectionManager connect = new OTFConnectionManager();

	public OTFVisClient() {
		super("dummyURL");
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

	protected OTFClientQuad getRightDrawerComponent() throws RemoteException {
		OTFConnectionManager connectR = this.connect.clone();
		OTFClientQuad clientQ2 = createNewView(null, connectR, this.hostControlBar.getOTFHostConnectionManager());
		return clientQ2;
	}

	@Override
	protected OTFDrawer createDrawer(){
		prepareConnectionManager();
		try {
			OTFTimeLine timeLine = new OTFTimeLine("time", hostControlBar.getOTFHostControl());
			frame.getContentPane().add(timeLine, BorderLayout.SOUTH);
			hostControlBar.addDrawer("timeline", timeLine);
			OTFDrawer mainDrawer;
			if (swing) {
				mainDrawer = new OTFSwingDrawerContainer(this.getRightDrawerComponent(), hostControlBar);
			} else {
				mainDrawer = new OTFOGLDrawer(this.getRightDrawerComponent(), hostControlBar);
			}
			if (hostControlBar.getOTFHostConnectionManager().isLiveHost()) {
				final OTFQueryControl queryControl = new OTFQueryControl(hostControlBar, OTFClientControl.getInstance().getOTFVisConfig());
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
				frame.getContentPane().add(queryControlBar, BorderLayout.SOUTH);
				mainDrawer.setQueryHandler(queryControl);
			}
			return mainDrawer;
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

	@Override
	protected OTFVisConfigGroup createOTFVisConfig() {
		try {
			saver = new SettingsSaver(this.masterHostControl.getAddress());
			return this.masterHostControl.getOTFServer().getOTFVisConfig();
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

	public void setSwing(boolean swing) {
		this.swing = swing;
	}
	
	public static final void playNetwork(final String filename) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(filename);
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		OTFVisLiveServer server = new OTFVisLiveServer(scenario, events);
		OTFHostConnectionManager hostConnectionManager = new OTFHostConnectionManager(filename, server);
		OTFVisClient client = new OTFVisClient();
		client.setHostConnectionManager(hostConnectionManager);
		client.setSwing(false);
		client.run();
		server.getSnapshotReceiver().finish();
	}
	
	public static void main(final String[] args) {
		play("input/network.xml", false); 
	}
	
	private static final String chooseFile() {
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter( new FileFilter() {
			@Override public boolean accept( File f ) {
				return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".xml" );
			}
			@Override public String getDescription() { return "MATSim net or config file (*.xml)"; }
		} );

		fc.setFileFilter( new FileFilter() {
			@Override public boolean accept( File f ) {
				return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".mvi" );
			}
			@Override public String getDescription() { return "OTFVis movie file (*.mvi)"; }
		} );

		int state = fc.showOpenDialog( null );
		if ( state == JFileChooser.APPROVE_OPTION ) {
			String filename = fc.getSelectedFile().getAbsolutePath();
			return filename;
		}
		System.out.println( "No file selected." );
		return null;
	}
	
	private static final void play(String filename, boolean useSwing) {
		String lowerCaseFilename = filename.toLowerCase();
		playNetwork(filename);
	}

}
