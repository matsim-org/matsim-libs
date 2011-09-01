package playground.mzilske.osm;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import org.jdesktop.swingx.mapviewer.wms.WMSService;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.config.Config;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.WGS84ToMercator;
import org.matsim.vis.otfvis.OTFClient;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.data.OTFClientQuadTree;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.data.fileio.SettingsSaver;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFTimeLine;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.interfaces.OTFServer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.layer.AgentPointDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleQuadDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleStaticNetLayer;

public final class JXMapOTFVisClient {


	public static void run(final Config config, final OTFServer server) {
		assertZoomLevel17(config);
		run(config, server, osmTileFactory());
	}

	private static void assertZoomLevel17(Config config) {
		if(config.otfVis().getMaximumZoom() != 17) {
			throw new RuntimeException("The OSM layer only works with maximumZoomLevel = 17. Please adjust your config.");
		}
	}

	public static void run(final Config config, final OTFServer server, final WMSService wms) {
		final TileFactory tf = new MyWMSTileFactory(wms, config.otfVis().getMaximumZoom());
		run(config, server, tf);
	}

	private static void run(final Config config, final OTFServer server, final TileFactory tf) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				OTFClient otfClient = new OTFClient();
				otfClient.setServer(server);
				OTFConnectionManager connect = new OTFConnectionManager();
				OTFVisConfigGroup otfVisConfig = server.getOTFVisConfig();
				otfVisConfig.setMapOverlayMode(true);
				OTFClientControl.getInstance().setOTFVisConfig(otfVisConfig);
				connect.connectLinkToWriter(OTFLinkAgentsHandler.Writer.class);
				connect.connectWriterToReader(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
				connect.connectReaderToReceiver(OTFLinkAgentsHandler.class, AgentPointDrawer.class);
				connect.connectReaderToReceiver(OTFLinkAgentsHandler.class, OGLSimpleQuadDrawer.class);
				connect.connectReceiverToLayer(OGLSimpleQuadDrawer.class, OGLSimpleStaticNetLayer.class);		
				connect.connectReceiverToLayer(AgentPointDrawer.class, OGLAgentPointLayer.class);

				OTFHostControlBar hostControlBar = otfClient.getHostControlBar();
				OTFTimeLine timeLine = new OTFTimeLine("time", hostControlBar.getOTFHostControl());
				otfClient.getFrame().getContentPane().add(timeLine, BorderLayout.SOUTH);
				OTFServerQuadTree servQ = server.getQuad(connect);
				OTFClientQuadTree clientQ = servQ.convertToClient(server, connect);
				clientQ.setConnectionManager(connect);
				clientQ.getConstData();

				final OTFOGLDrawer mainDrawer = new OTFOGLDrawer(clientQ, hostControlBar, config.otfVis());
				otfClient.addDrawerAndInitialize(mainDrawer, new SettingsSaver("settings"));

				final JPanel compositePanel = otfClient.getCompositePanel();
				final JXMapViewer jMapViewer = new JXMapViewer();
				jMapViewer.setTileFactory(tf);
				jMapViewer.setPanEnabled(false);
				jMapViewer.setZoomEnabled(false);
				compositePanel.add(jMapViewer);

				installCustomRepaintManager(compositePanel, jMapViewer);

				final CoordinateTransformation coordinateTransformation = new WGS84ToMercator.Deproject(config.otfVis().getMaximumZoom());
				mainDrawer.addChangeListener(new ChangeListener() {

					@Override
					public void stateChanged(ChangeEvent e) {
						double x = mainDrawer.getViewBoundsAsQuadTreeRect().centerX + mainDrawer.getQuad().offsetEast;
						double y = mainDrawer.getViewBoundsAsQuadTreeRect().centerY + mainDrawer.getQuad().offsetNorth;
						Coord center = coordinateTransformation.transform(new CoordImpl(x,y));
						double scale = mainDrawer.getScale();
						int zoom = (int) log2(scale);
						jMapViewer.setCenterPosition(new GeoPosition(center.getY(), center.getX()));
						jMapViewer.setZoom(zoom);
						compositePanel.repaint();
					}

				});
				otfClient.show();
			}
		});
	}

	/**
	 * This method statically installs a custom Swing RepaintManager which ties the map component to the JPanel in which it is 
	 * layered under the agent drawer. Otherwise the map would repaint itself when it has finished loading a tile, and the agent drawer
	 * would not notice and would be painted over.
	 * 
	 * This looks dirty and probably does not scale to the case where many components would do this, but it is the only way
	 * I have found, short of patching the JXMapViewer.
	 * 
	 */
	private static void installCustomRepaintManager(final JPanel compositePanel, final JXMapViewer jMapViewer) {
		RepaintManager myManager = new RepaintManager() {
			public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
				// I had the feeling I should call the *previous* RepaintManager here instead of the supertype, but that does not work.
				// So I call the supertype.
				super.addDirtyRegion(c, x, y, w, h); 
				if (c == jMapViewer) {
					addDirtyRegion(compositePanel, x, y, w, h);
				}
			}
		};
		RepaintManager.setCurrentManager(myManager);
	}

	private static TileFactory osmTileFactory() {
		final int max=17;
		TileFactoryInfo info = new TileFactoryInfo(0, 17, 17,
				256, true, true,
				"http://tile.openstreetmap.org",
				"x","y","z") {
			public String getTileUrl(int x, int y, int zoom) {
				zoom = max-zoom;
				String url = this.baseURL +"/"+zoom+"/"+x+"/"+y+".png";
				return url;
			}

		};
		TileFactory tf = new DefaultTileFactory(info);
		return tf;
	}

	private static double log2 (double scale) {
		return Math.log(scale) / Math.log(2);
	}

}
