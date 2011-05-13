/* *********************************************************************** *
 * project: org.matsim.*
 * OTFClient
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.mzilske.osm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadI;
import org.matsim.vis.otfvis.gui.OTFFrame;
import org.matsim.vis.otfvis.gui.OTFHostConnectionManager;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.gui.PreferencesDialog;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFLiveServerRemote;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.SettingsSaver;
import org.openstreetmap.gui.jmapviewer.JMapViewer;



/**
 * @author dgrether
 *
 */
public abstract class OTFClient implements Runnable {

	private static final Logger log = Logger.getLogger(OTFClient.class);

	protected String url;

	protected OTFFrame frame;

	protected JSplitPane pane = null;

	protected OTFHostControlBar hostControlBar = null;

	protected SettingsSaver saver;

	protected OTFHostConnectionManager masterHostControl;

	private OTFLiveServerRemote server;

	public OTFClient(OTFLiveServerRemote otfServer) {
		this.server = otfServer;
	}
	
	public OTFClient(String url) {
		this.url = url;
	}

	@Override
	public void run() {
		if (this.masterHostControl == null) {
			setHostConnectionManager(new OTFHostConnectionManager(this.url, this.server));
		}
		createMainFrame();
		log.info("created MainFrame");
		OTFVisConfigGroup visconf = createOTFVisConfig();
		OTFClientControl.getInstance().setOTFVisConfig(visconf);
		log.info("got OTFVis config");
		createHostControlBar();
		log.info("created HostControlBar");
		final OTFDrawer mainDrawer = createDrawer();
		OTFClientControl.getInstance().setMainOTFDrawer(mainDrawer);
		log.info("created drawer");
		final JPanel compositePanel = new JPanel();
		compositePanel.setLayout(new OverlayLayout(compositePanel));
		compositePanel.add(mainDrawer.getComponent());
		final JMapViewer jMapViewer = new MyJMapViewer(compositePanel);
		// final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation("EPSG:3395", TransformationFactory.WGS84);

		jMapViewer.setZoom(WGS84ToOSMMercator.SCALE);
		
		final CoordinateTransformation coordinateTransformation = new WGS84ToOSMMercator.Deproject();


		compositePanel.add(jMapViewer);

		((OTFOGLDrawer) mainDrawer).addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				double x = ((OTFOGLDrawer) mainDrawer).getViewBounds().centerX + mainDrawer.getQuad().offsetEast;
				double y = ((OTFOGLDrawer) mainDrawer).getViewBounds().centerY + mainDrawer.getQuad().offsetNorth;
				Coord center = coordinateTransformation.transform(new CoordImpl(x,y));
				//System.out.println("Center: " + center.getX() + "," + center.getY() );
				jMapViewer.setDisplayPositionByLatLon(center.getY(), center.getX(), jMapViewer.getZoom());
				compositePanel.repaint();
			}

		});


		


		mainDrawer.getComponent().addMouseWheelListener(new MouseWheelListener() {

			boolean firstTime = true;

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				
				if (firstTime) {
					Rect viewBounds = ((OTFOGLDrawer) mainDrawer).getViewBounds();
					double minX = (viewBounds.minX + mainDrawer.getQuad().offsetEast);
					double minY = (viewBounds.minY + mainDrawer.getQuad().offsetNorth);
					final Coord topLeft = new CoordImpl(minX,minY);
					double maxX = (viewBounds.maxX + mainDrawer.getQuad().offsetEast);
					double maxY = (viewBounds.maxY + mainDrawer.getQuad().offsetNorth);
					final Coord bottomRight = new CoordImpl(maxX,maxY);
					double xRatio = jMapViewer.getWidth() / (bottomRight.getX() - topLeft.getX());
					double yRatio = jMapViewer.getHeight() / (bottomRight.getY() - topLeft.getY());
					System.out.println("firsttime: "+(maxX-minX) + " " + (maxY - minY));
					System.out.println(jMapViewer.getWidth() +" " + jMapViewer.getHeight());
					System.out.println("Ratio (x,y): " + xRatio+","+yRatio);
					mainDrawer.setScale((float) yRatio);
					// mainDrawer.setScale(0.8f * (float)yRatio);
					firstTime = false;
				} else {
					if (e.getWheelRotation() < 0) {
						jMapViewer.setZoom(jMapViewer.getZoom() - e.getWheelRotation(), e.getPoint());
						mainDrawer.setScale(mainDrawer.getScale() * 0.5f);
					} else if (e.getWheelRotation() > 0) {
						jMapViewer.setZoom(jMapViewer.getZoom() - e.getWheelRotation(), e.getPoint());
						mainDrawer.setScale(mainDrawer.getScale() * 2.0f);
					}
				}
				
				compositePanel.repaint();
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						Rect viewBounds = ((OTFOGLDrawer) mainDrawer).getViewBounds();
						double minX = (viewBounds.minX + mainDrawer.getQuad().offsetEast);
						double minY = (viewBounds.minY + mainDrawer.getQuad().offsetNorth);
						double maxX = (viewBounds.maxX + mainDrawer.getQuad().offsetEast);
						double maxY = (viewBounds.maxY + mainDrawer.getQuad().offsetNorth);
						System.out.println((maxX-minX) + " " + (maxY - minY));
					}
					
				});
				
			}

		});


		JPanel panel = new JPanel(new BorderLayout());
		panel.add(compositePanel, BorderLayout.CENTER);
		pane.setRightComponent(panel);
		pane.validate();
		this.hostControlBar.addDrawer(mainDrawer);
		mainDrawer.redraw();
		frame.setVisible(true);
		log.info("OTFVis finished init");
	}

	public void setHostConnectionManager(OTFHostConnectionManager otfHostConnectionManager) {
		this.masterHostControl = otfHostConnectionManager;
	}

	public OTFClientQuad createNewView(String id, OTFConnectionManager connect, OTFHostConnectionManager hostControl) {
		log.info("Getting Quad id " + id);
		OTFServerQuadI servQ = hostControl.getOTFServer().getQuad(id, connect);
		log.info("Converting Quad");
		OTFClientQuad clientQ = servQ.convertToClient(id, hostControl.getOTFServer(), connect);
		log.info("Creating receivers...");
		clientQ.createReceiver(connect);
		log.info("Reading data...");
		clientQ.getConstData();
		this.hostControlBar.updateTimeLabel();
		log.info("Created OTFClientQuad!");
		return clientQ;
	}

	protected void createHostControlBar() {
		this.hostControlBar = new OTFHostControlBar(masterHostControl);
		frame.getContentPane().add(this.hostControlBar, BorderLayout.NORTH);
		PreferencesDialog preferencesDialog = new PreferencesDialog(frame, hostControlBar);
		preferencesDialog.setVisConfig(OTFClientControl.getInstance().getOTFVisConfig());
		buildMenu(frame, hostControlBar, saver);
	}

	@SuppressWarnings("serial")
	private static void buildMenu(final OTFFrame frame, final OTFHostControlBar hostControlBar, final SettingsSaver save) {
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		Action prefAction = new AbstractAction() {
			{
				putValue(Action.NAME, "Preferences...");
				putValue(Action.MNEMONIC_KEY, 0);
			}

			@Override
			public void actionPerformed(final ActionEvent e) {
				PreferencesDialog preferencesDialog = new PreferencesDialog(frame, hostControlBar);
				preferencesDialog.setVisConfig(OTFClientControl.getInstance().getOTFVisConfig());
				preferencesDialog.setVisible(true);
			}
		};
		fileMenu.add(prefAction);
		Action saveAsAction = new AbstractAction() {
			{
				putValue(Action.NAME, "Save Settings as...");
				putValue(Action.MNEMONIC_KEY, 1);
			}

			@Override
			public void actionPerformed(final ActionEvent e) {
				save.saveSettingsAs(OTFClientControl.getInstance().getOTFVisConfig());
			}
		};
		fileMenu.add(saveAsAction);

		Action openAction = new AbstractAction() {
			{
				putValue(Action.NAME, "Open Settings...");
				putValue(Action.MNEMONIC_KEY, 1);
			}

			@Override
			public void actionPerformed(final ActionEvent e) {
				OTFVisConfigGroup visConfig = save.chooseAndReadSettingsFile();
				OTFClientControl.getInstance().setOTFVisConfig(visConfig);
				OTFClientControl.getInstance().getMainOTFDrawer().redraw();
			}
		};
		fileMenu.add(openAction);

		Action exitAction = new AbstractAction("Quit") {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.endProgram(0);
			}
		};
		fileMenu.add(exitAction);
		frame.setJMenuBar(menuBar);
		SwingUtilities.updateComponentTreeUI(frame);
	}

	protected void createMainFrame(){
		this.frame = new OTFFrame("MATSim OTFVis");
		this.pane = frame.getSplitPane();
		this.pane.setDividerLocation(0.5);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.frame.setSize(screenSize.width/2,screenSize.height/2);
	}

	protected abstract OTFDrawer createDrawer();

	protected abstract OTFVisConfigGroup createOTFVisConfig();

}
