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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadI;
import org.matsim.vis.otfvis.gui.OTFFrame;
import org.matsim.vis.otfvis.gui.OTFHostConnectionManager;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.gui.PreferencesDialog;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.gui.SettingsSaver;


/**
 * @author dgrether
 *
 */
public class OTFClient {

	private static final Logger log = Logger.getLogger(OTFClient.class);

	// Keine Ahnung.
	private static final String id = "id";

	private OTFFrame frame;

	private OTFHostControlBar hostControlBar;

	private OTFHostConnectionManager hostConnectionManager;

	private JPanel compositePanel;

	private OTFDrawer mainDrawer;


	public OTFClient() {
		this.frame = new OTFFrame("MATSim OTFVis");
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.frame.setSize(screenSize.width/2,screenSize.height/2);
		log.info("created MainFrame");
	}

	public final void setHostConnectionManager(OTFHostConnectionManager otfHostConnectionManager) {
		this.hostConnectionManager = otfHostConnectionManager;
		this.hostControlBar = new OTFHostControlBar(otfHostConnectionManager);
	}

	public final OTFClientQuad createNewView(OTFConnectionManager connect) {
		log.info("Getting Quad id " + id);
		OTFServerQuadI servQ = hostConnectionManager.getOTFServer().getQuad(id, connect);
		log.info("Converting Quad");
		OTFClientQuad clientQ = servQ.convertToClient(id, hostConnectionManager.getOTFServer(), connect);
		log.info("Creating receivers...");
		clientQ.createReceiver(connect);
		log.info("Reading data...");
		clientQ.getConstData();
		this.hostControlBar.updateTimeLabel();
		log.info("Created OTFClientQuad!");
		return clientQ;
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

	public void addDrawerAndInitialize(OTFDrawer mainDrawer, SettingsSaver saver) {
		this.mainDrawer = mainDrawer;
		log.info("got OTFVis config");
		frame.getContentPane().add(this.hostControlBar, BorderLayout.NORTH);
		buildMenu(frame, hostControlBar, saver);
		log.info("created HostControlBar");
		OTFClientControl.getInstance().setMainOTFDrawer(mainDrawer);
		log.info("created drawer");
		compositePanel = new JPanel();
		compositePanel.setLayout(new OverlayLayout(compositePanel));
		compositePanel.add(mainDrawer.getComponent());
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(compositePanel, BorderLayout.CENTER);
		this.frame.getContentPane().add(panel);
		hostControlBar.addDrawer(mainDrawer);
	}

	public void show() {
		mainDrawer.redraw();
		frame.setVisible(true);
	}

	public JPanel getCompositePanel() {
		return compositePanel;
	}

	public OTFHostControlBar getHostControlBar() {
		return hostControlBar;
	}

	public OTFFrame getFrame() {
		return frame;
	}

}
