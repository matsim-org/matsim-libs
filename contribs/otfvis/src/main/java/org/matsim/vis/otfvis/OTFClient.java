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
package org.matsim.vis.otfvis;

import com.jogamp.opengl.GLAutoDrawable;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.vis.otfvis.data.fileio.SettingsSaver;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.PreferencesDialog;
import org.matsim.vis.otfvis.interfaces.OTFServer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.utils.WGS84ToMercator;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;


/**
 * @author dgrether
 *
 */
public final class OTFClient extends JFrame {

	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(OTFClient.class);

	private OTFHostControlBar hostControlBar;

	private OTFOGLDrawer mainDrawer;

	private OTFServer server;

	private JPanel compositePanel;

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
	
	private static double log2 (double scale) {
		return Math.log(scale) / Math.log(2);
	}
	
	public OTFClient(GLAutoDrawable canvas) {
		super("MATSim OTFVis");
		this.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
		JFrame.setDefaultLookAndFeelDecorated(true);
		boolean isMac = System.getProperty("os.name").equals("Mac OS X");
		if (isMac){
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
		}
		//Make sure menus appear above JOGL Layer
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		compositePanel = new JPanel();
		compositePanel.setBackground(Color.white);
		compositePanel.setOpaque(true);
		compositePanel.setLayout(new OverlayLayout(compositePanel));
		compositePanel.add((Component) canvas);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		compositePanel.setPreferredSize(new Dimension(screenSize.width/2,screenSize.height/2));
		log.info("created MainFrame");
	}

	public void setServer(OTFServer server) {
		this.server = server;
		this.hostControlBar = new OTFHostControlBar(server);
	}

	@SuppressWarnings("serial")
	private void buildMenu(final OTFHostControlBar hostControlBar, final SettingsSaver save, final OTFServer server) {
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
				PreferencesDialog preferencesDialog = new PreferencesDialog(server, OTFClient.this, hostControlBar);
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
			}
		};
		fileMenu.add(openAction);

		Action exitAction = new AbstractAction("Quit") {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispatchEvent(new WindowEvent(OTFClient.this, WindowEvent.WINDOW_CLOSING));			}
		};
		fileMenu.add(exitAction);
		setJMenuBar(menuBar);
		SwingUtilities.updateComponentTreeUI(this);
	}

	public void addDrawerAndInitialize(final OTFOGLDrawer mainDrawer, SettingsSaver saver) {
		this.mainDrawer = mainDrawer;
		log.info("got OTFVis config");
		getContentPane().add(this.hostControlBar, BorderLayout.NORTH);
		buildMenu(hostControlBar, saver, server);
		log.info("created HostControlBar");
		OTFClientControl.getInstance().setMainOTFDrawer(mainDrawer);
		log.info("created drawer");
		getContentPane().add(compositePanel, BorderLayout.CENTER);
		hostControlBar.setDrawer(mainDrawer);
	}
	
	public void addMapViewer(TileFactory tf) {
		final JXMapViewer jMapViewer = new JXMapViewer();
		jMapViewer.setTileFactory(tf);
		compositePanel.add(jMapViewer);
		installCustomRepaintManager(compositePanel, jMapViewer);
		OTFVisConfigGroup otfVisConfig = OTFClientControl.getInstance().getOTFVisConfig();
		final CoordinateTransformation coordinateTransformation = new WGS84ToMercator.Deproject(otfVisConfig.getMaximumZoom());
		mainDrawer.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				double x = mainDrawer.getViewBoundsAsQuadTreeRect().centerX + mainDrawer.getQuad().offsetEast;
				double y = mainDrawer.getViewBoundsAsQuadTreeRect().centerY + mainDrawer.getQuad().offsetNorth;
				Coord center = coordinateTransformation.transform(new Coord(x, y));
				double scale = mainDrawer.getScale();
				int zoom = (int) log2(scale);
				jMapViewer.setCenterPosition(new GeoPosition(center.getY(), center.getX()));
				jMapViewer.setZoom(zoom);
				compositePanel.repaint();
			}

		});
	}

	public OTFHostControlBar getHostControlBar() {
		return hostControlBar;
	}

}
