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
import org.matsim.vis.otfvis.gui.OTFFrame;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.gui.PreferencesDialog;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;
import org.matsim.vis.otfvis.opengl.gui.SettingsSaver;


/**
 * @author dgrether
 *
 */
public final class OTFClient {

	private static final Logger log = Logger.getLogger(OTFClient.class);
	
	private OTFFrame frame;

	private OTFHostControlBar hostControlBar;

	private JPanel compositePanel;

	private OTFDrawer mainDrawer;

	private OTFServerRemote server;

	public OTFClient() {
		this.frame = new OTFFrame("MATSim OTFVis");
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.frame.setSize(screenSize.width/2,screenSize.height/2);
		log.info("created MainFrame");
	}

	public void setServer(OTFServerRemote server) {
		this.server = server;
		this.hostControlBar = new OTFHostControlBar(server);
	}

	@SuppressWarnings("serial")
	private static void buildMenu(final OTFFrame frame, final OTFHostControlBar hostControlBar, final SettingsSaver save, final OTFServerRemote server) {
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
				PreferencesDialog preferencesDialog = new PreferencesDialog(server, frame, hostControlBar);
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
		buildMenu(frame, hostControlBar, saver, server);
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
