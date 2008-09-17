/* *********************************************************************** *
 * project: org.matsim.*
 * PreferencesDialog2.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.utils.vis.otfvis.opengl.gui;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.matsim.utils.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.utils.vis.otfvis.gui.OTFVisConfig;
import org.matsim.utils.vis.otfvis.gui.PreferencesDialog;
import org.matsim.utils.vis.otfvis.opengl.queries.QueryToggleShowParking;


public class PreferencesDialog2 extends PreferencesDialog implements ItemListener{

	public PreferencesDialog2(JFrame frame, OTFVisConfig config, OTFHostControlBar mother) {
		super(frame, config, mother);
		// TODO Auto-generated constructor stub
	}

	private static final long serialVersionUID = 5778562849300898138L;

	@Override
	protected void initGUI() {
		// TODO Auto-generated method stub
		super.initGUI();
		{
			JPanel panel = new JPanel(null);
			getContentPane().add(panel);
			panel.setBorder(BorderFactory.createTitledBorder("Switches"));
			panel.setBounds(240, 130, 220, 80);

			JCheckBox SynchBox = new JCheckBox("show parked vehicles");
//			SynchBox.setMnemonic(KeyEvent.VK_M);
			SynchBox.setSelected(cfg.isShowParking());
			SynchBox.addItemListener(this);
			SynchBox.setBounds(10, 20, 200, 31);
			SynchBox.setVisible(true);
			//SynchBox.setMaximumSize(new Dimension(250,60));
			panel.add(SynchBox);

			SynchBox = new JCheckBox("show link Ids");
//			SynchBox.setMnemonic(KeyEvent.VK_M);
			SynchBox.setSelected(cfg.drawLinkIds());
			SynchBox.addItemListener(this);
			SynchBox.setBounds(10, 40, 200, 31);
			SynchBox.setVisible(true);
			//SynchBox.setMaximumSize(new Dimension(250,60));
			panel.add(SynchBox);
		}
	}

	public void itemStateChanged(ItemEvent e) {
		JCheckBox source = (JCheckBox)e.getItemSelectable();
		if (source.getText().equals("show parked vehicles")) {
			cfg.setShowParking(e.getStateChange() != ItemEvent.DESELECTED);
			cfg.setShowParking(!cfg.isShowParking());
			if (host != null) {
				host.doQuery(new QueryToggleShowParking());
				host.clearCaches();
				host.invalidateHandlers();
			}
		} else if (source.getText().equals("show link Ids")) {
			// toggle draw link Ids
			cfg.setDrawLinkIds(!cfg.drawLinkIds());
		}
	}
	
}
