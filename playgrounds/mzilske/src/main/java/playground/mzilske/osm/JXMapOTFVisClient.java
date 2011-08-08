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
import org.matsim.vis.otfvis.OTFClient;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.data.OTFClientQuadTree;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFTimeLine;
import org.matsim.vis.otfvis.opengl.gui.SettingsSaver;
import org.matsim.vis.otfvis.opengl.layer.AgentPointDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleQuadDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleStaticNetLayer;

public final class JXMapOTFVisClient {

	public static void run(final Config config, final OTFServerRemote server) {
		final CoordinateTransformation coordinateTransformation = new WGS84ToOSMMercator.Deproject();
		run(server, osmTileFactory(), coordinateTransformation);
	}
	
	public static void run(final Config config, final OTFServerRemote server, final WMSService wms, final CoordinateTransformation coordinateTransformation) {
		final TileFactory tf = new MyWMSTileFactory(wms);
		run(server, tf, coordinateTransformation);
	}

	private static void run(final OTFServerRemote server, final TileFactory tf, final CoordinateTransformation coordinateTransformation) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				OTFClient otfClient = new OTFClient();
				OTFOGLDrawer.USE_GLJPANEL = true;
				otfClient.setServer(server);
				OTFConnectionManager connect = new OTFConnectionManager();
				OTFClientControl.getInstance().setOTFVisConfig(server.getOTFVisConfig());
				connect.connectLinkToWriter(OTFLinkAgentsHandler.Writer.class);
				connect.connectWriterToReader(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
				connect.connectReaderToReceiver(OTFLinkAgentsHandler.class, AgentPointDrawer.class);
				connect.connectReaderToReceiver(OTFLinkAgentsHandler.class, OGLSimpleQuadDrawer.class);
				connect.connectReceiverToLayer(OGLSimpleQuadDrawer.class, OGLSimpleStaticNetLayer.class);		
				connect.connectReceiverToLayer(AgentPointDrawer.class, OGLAgentPointLayer.class);
				
				OTFHostControlBar hostControlBar = otfClient.getHostControlBar();
				OTFTimeLine timeLine = new OTFTimeLine("time", hostControlBar.getOTFHostControl());
				otfClient.getFrame().getContentPane().add(timeLine, BorderLayout.SOUTH);
				hostControlBar.addDrawer(timeLine);
				OTFServerQuadTree servQ = server.getQuad(connect);
				OTFClientQuadTree clientQ = servQ.convertToClient(server, connect);
				clientQ.createReceiver(connect);
				clientQ.getConstData();
				hostControlBar.updateTimeLabel();

				final OTFDrawer mainDrawer = new OTFOGLDrawer(clientQ, hostControlBar);
				otfClient.addDrawerAndInitialize(mainDrawer, new SettingsSaver("settings"));

				final JPanel compositePanel = otfClient.getCompositePanel();
				final JXMapViewer jMapViewer = new JXMapViewer();
				jMapViewer.setTileFactory(tf);
				jMapViewer.setPanEnabled(false);
				jMapViewer.setZoomEnabled(false);
				compositePanel.add(jMapViewer);

				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						RepaintManager myManager = new RepaintManager() {
							public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
								super.addDirtyRegion(c, x, y, w, h);
								if (c == jMapViewer) {
									addDirtyRegion(compositePanel, x, y, w, h);
								}
							}
						};
						RepaintManager.setCurrentManager(myManager);
					}

				});

				((OTFOGLDrawer) mainDrawer).addChangeListener(new ChangeListener() {

					@Override
					public void stateChanged(ChangeEvent e) {
						if (((OTFOGLDrawer) mainDrawer).getViewBounds() != null) {
							double x = ((OTFOGLDrawer) mainDrawer).getViewBounds().centerX + mainDrawer.getQuad().offsetEast;
							double y = ((OTFOGLDrawer) mainDrawer).getViewBounds().centerY + mainDrawer.getQuad().offsetNorth;
							Coord center = coordinateTransformation.transform(new CoordImpl(x,y));
							double scale = mainDrawer.getScale();
							int zoomDiff = (int) log2(scale);
							jMapViewer.setCenterPosition(new GeoPosition(center.getY(), center.getX()));
							jMapViewer.setZoom((17 - (WGS84ToOSMMercator.SCALE - zoomDiff)));
							compositePanel.repaint();
						}
					}

				});
				otfClient.show();
			}
		});
	}

	private static TileFactory osmTileFactory() {
		final int max=17;
		TileFactoryInfo info = new TileFactoryInfo(1,max-2,max,
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
