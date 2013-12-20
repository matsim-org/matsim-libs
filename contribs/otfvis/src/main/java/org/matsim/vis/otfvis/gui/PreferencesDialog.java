/* *********************************************************************** *
 * project: org.matsim.*
 * PreferencesDialog.java
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

package org.matsim.vis.otfvis.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.interfaces.OTFServer;

/**
 * The class responsible for drawing the PreferencesDialog.
 * 
 * @author dstrippgen
 *
 */
public class PreferencesDialog extends javax.swing.JDialog implements ChangeListener, ActionListener, ItemListener {

	private static final String SHOW_TRANSIT_FACILITIES = "show transit facilities";

	private static final String SHOW_SCALE_BAR = "show scale bar";

	private static final String SHOW_TIME_GL = "show time GL";

	private static final String SAVE_JPG_FRAMES = "save jpg frames";

	private static final String SHOW_OVERLAYS = "show overlays";

	private static final String SHOW_LINK_IDS = "show link Ids";

	private static final String SHOW_NON_MOVING_ITEMS = "show non-moving items";

	private static final long serialVersionUID = 5778562849300898138L;

	private final OTFServer server;
	
	private OTFVisConfigGroup visConfig;

	private JComboBox rightMFunc;

	private JComboBox middleMFunc;

	private JComboBox leftMFunc;

	private OTFHostControlBar host = null;

	private JSpinner agentSizeSpinner = null;

	private JSpinner linkWidthSpinner;

	private JSpinner delaySpinner = null;

	public PreferencesDialog(final OTFServer server, final JFrame frame, final OTFHostControlBar mother) {
		super(frame);
		this.server = server;
		this.host = mother;
	}

	private void initGUI() {
		getContentPane().setLayout(null);
		this.setResizable(false);
		setSize(480, 400);

		// Mouse Buttons
		{
			JPanel panel = new JPanel(null);
			getContentPane().add(panel);
			panel.setBorder(BorderFactory.createTitledBorder("Mouse Buttons"));
			panel.setBounds(10, 10, 220, 120);
			{
				JLabel label = new JLabel("Left:", JLabel.RIGHT);
				panel.add(label);
				label.setBounds(10, 20, 55, 27);
			}
			{
				JLabel label = new JLabel("Middle:", JLabel.RIGHT);
				panel.add(label);
				label.setBounds(10, 50, 55, 27);
			}
			{
				JLabel label = new JLabel("Right:", JLabel.RIGHT);
				panel.add(label);
				label.setBounds(10, 80, 55, 27);
			}
			{
				ComboBoxModel leftMFuncModel = new DefaultComboBoxModel(new String[] { "Zoom", "Pan", "Select" });
				leftMFuncModel.setSelectedItem(this.visConfig.getLeftMouseFunc());
				this.leftMFunc = new JComboBox();
				panel.add(this.leftMFunc);
				this.leftMFunc.setModel(leftMFuncModel);
				this.leftMFunc.setBounds(70, 20, 120, 27);
				this.leftMFunc.addActionListener(this);
			}
			{
				ComboBoxModel jComboBox1Model = new DefaultComboBoxModel(new String[] { "Zoom", "Pan", "Select" });
				jComboBox1Model.setSelectedItem(this.visConfig.getMiddleMouseFunc());
				this.middleMFunc = new JComboBox();
				panel.add(this.middleMFunc);
				this.middleMFunc.setModel(jComboBox1Model);
				this.middleMFunc.setBounds(70, 50, 120, 27);
				this.middleMFunc.addActionListener(this);
			}
			{
				ComboBoxModel jComboBox2Model = new DefaultComboBoxModel(new String[] { "Menu", "Zoom", "Pan", "Select" });
				jComboBox2Model.setSelectedItem(this.visConfig.getRightMouseFunc());
				this.rightMFunc = new JComboBox();
				panel.add(this.rightMFunc);
				this.rightMFunc.setModel(jComboBox2Model);
				this.rightMFunc.setBounds(70, 80, 120, 27);
				this.rightMFunc.addActionListener(this);
			}		
		}
		{
			JPanel panel = new JPanel(null);
			getContentPane().add(panel);
			panel.setBorder(BorderFactory.createTitledBorder("Switches"));
			panel.setBounds(250, 130, 220, 200);

			JCheckBox synchBox; 
			if(server.isLive()) {
				synchBox = new JCheckBox(SHOW_NON_MOVING_ITEMS);
				synchBox.setSelected(visConfig.isDrawNonMovingItems());
				synchBox.addItemListener(this);
				synchBox.setBounds(10, 20, 200, 31);
				synchBox.setVisible(true);
				panel.add(synchBox);
			}
			synchBox = new JCheckBox(SHOW_LINK_IDS);
			synchBox.setSelected(visConfig.isDrawingLinkIds());
			synchBox.addItemListener(this);
			synchBox.setBounds(10, 40, 200, 31);
			synchBox.setVisible(true);
			panel.add(synchBox);
			
			synchBox = new JCheckBox(SHOW_OVERLAYS);
			synchBox.setSelected(visConfig.drawOverlays());
			synchBox.addItemListener(this);
			synchBox.setBounds(10, 60, 200, 31);
			synchBox.setVisible(true);
			panel.add(synchBox);

			synchBox = new JCheckBox(SHOW_TIME_GL);
			synchBox.setSelected(visConfig.drawTime());
			synchBox.addItemListener(this);
			synchBox.setBounds(10, 80, 200, 31);
			panel.add(synchBox);

			synchBox = new JCheckBox(SAVE_JPG_FRAMES);
			synchBox.setSelected(visConfig.renderImages());
			synchBox.addItemListener(this);
			synchBox.setBounds(10, 100, 200, 31);
			panel.add(synchBox);
			
			synchBox = new JCheckBox(SHOW_SCALE_BAR);
			synchBox.setSelected(visConfig.drawScaleBar());
			synchBox.addItemListener(this);
			synchBox.setBounds(10, 140, 200, 31);
			synchBox.setVisible(true);
			panel.add(synchBox);
			if (server.isLive()) {
				synchBox = new JCheckBox(SHOW_TRANSIT_FACILITIES);
				synchBox.setSelected(visConfig.isDrawTransitFacilities());
				synchBox.addItemListener(this);
				synchBox.setBounds(10, 160, 200, 31);
				synchBox.setVisible(true);
				panel.add(synchBox);
			}
		}


		// Colors
		{
			JPanel panel = new JPanel(null);
			getContentPane().add(panel);
			panel.setBorder(BorderFactory.createTitledBorder("Colors"));
			panel.setBounds(250, 10, 220, 120);

			{
				JButton jButton = new JButton("Set Background...");
				panel.add(jButton);
				jButton.setBounds(10, 20, 200, 31);
				jButton.addActionListener(this);
				jButton.setActionCommand("backgroundColor");
			}

			{
				JButton jButton = new JButton("Set Network...");
				panel.add(jButton);
				jButton.setBounds(10, 50, 200, 31);
				jButton.addActionListener(this);
				jButton.setActionCommand("networkColor");
			}

		}

		// Agent size
		{
			JLabel label = new JLabel();
			getContentPane().add(label);
			label.setText("AgentSize:");
			label.setBounds(10, 145, 80, 31);
			this.agentSizeSpinner = new JSpinner();
			SpinnerNumberModel model = new SpinnerNumberModel((int) visConfig.getAgentSize(), 0.1, Double.MAX_VALUE, 10);
			this.agentSizeSpinner.setModel(model);
			this.agentSizeSpinner.setBounds(90, 145, 153, 31);
			this.agentSizeSpinner.addChangeListener(this);
			getContentPane().add(label);
			getContentPane().add(this.agentSizeSpinner);
		}

		//Link Width
		{
			JLabel label = new JLabel();
			getContentPane().add(label);
			label.setText("LinkWidth:");
			label.setBounds(10, 195, 80, 31);
			this.linkWidthSpinner = new JSpinner();
			SpinnerNumberModel model2 = new SpinnerNumberModel((int) visConfig.getLinkWidth(), 0.1, Double.MAX_VALUE, 10);
			this.linkWidthSpinner.setModel(model2);
			this.linkWidthSpinner.setBounds(90, 195, 153, 31);
			this.linkWidthSpinner.addChangeListener(this);
			getContentPane().add(label);
			getContentPane().add(this.linkWidthSpinner);
		}

		//Delay ms
		{
			JLabel label = new JLabel();
			getContentPane().add(label);
			label.setText("AnimSpeed:");
			label.setBounds(10, 245, 110, 31);
			this.delaySpinner = new JSpinner();
			SpinnerNumberModel model2 = new SpinnerNumberModel(visConfig.getDelay_ms(), 0, Double.MAX_VALUE, 10);
			this.delaySpinner.setModel(model2);
			this.delaySpinner.setBounds(90, 245, 153, 31);
			this.delaySpinner.addChangeListener(this);
			getContentPane().add(label);
			getContentPane().add(this.delaySpinner);
		}

	}

	@Override
	public void stateChanged(final ChangeEvent e) {
		if (e.getSource() == this.agentSizeSpinner) {
			this.visConfig.setAgentSize(((Double)(this.agentSizeSpinner.getValue())).floatValue());
			if (this.host != null)
				this.host.getOTFHostControl().invalidateDrawers();
		} else if (e.getSource() == this.linkWidthSpinner) {
			this.visConfig.setLinkWidth(((Double)(this.linkWidthSpinner.getValue())).floatValue());
			if (this.host != null){
				host.redrawDrawers();
			}
		} else if (e.getSource() == this.delaySpinner) {
			this.visConfig.setDelay_ms(((Double)(this.delaySpinner.getValue())).intValue());
			if (this.host != null){
				host.redrawDrawers();
			}
		}
	}



	@Override
	public void actionPerformed(final ActionEvent e) {
		if (e.getSource() instanceof JComboBox) {
			JComboBox cb = (JComboBox) e.getSource();
			String newFunc = (String) cb.getSelectedItem();

			if (cb == this.leftMFunc) {
				this.visConfig.setLeftMouseFunc(newFunc);
			} else if (cb == this.middleMFunc) {
				this.visConfig.setMiddleMouseFunc(newFunc);
			} else if (cb == this.rightMFunc) {
				this.visConfig.setRightMouseFunc(newFunc);
			}
		} else if (e.getSource() instanceof JButton) {
			if (e.getActionCommand().equals("backgroundColor")) {
				JPanel frame = new JPanel();
				Color c = JColorChooser.showDialog(frame, "Choose the background color", this.visConfig.getBackgroundColor());
				if (c != null) {
					this.visConfig.setBackgroundColor(c);
					if (this.host != null) {
						this.host.getOTFHostControl().invalidateDrawers();
					}
				}
			}
			if (e.getActionCommand() == "networkColor") {
				JPanel frame = new JPanel();
				Color c = JColorChooser.showDialog(frame, "Choose the network color", this.visConfig.getNetworkColor());
				if (c != null) {
					this.visConfig.setNetworkColor(c);
					if (this.host != null) {
						this.host.getOTFHostControl().invalidateDrawers();
					}
				}
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		JCheckBox source = (JCheckBox)e.getItemSelectable();
		if (source.getText().equals(SHOW_NON_MOVING_ITEMS)) {
			visConfig.setDrawNonMovingItems(!visConfig.isDrawNonMovingItems());
			if (host != null) {
				server.setShowNonMovingItems(visConfig.isDrawNonMovingItems());
				host.clearCaches();
				host.redrawDrawers();
			}
		} else if (source.getText().equals(SHOW_LINK_IDS)) {
			// toggle draw link Ids
			visConfig.setDrawLinkIds(!visConfig.isDrawingLinkIds());
		} else if (source.getText().equals(SHOW_OVERLAYS)) {
			// toggle draw Overlays
			visConfig.setDrawOverlays(!visConfig.drawOverlays());
		} else if (source.getText().equals(SAVE_JPG_FRAMES)) {
			// toggle save jpgs
			visConfig.setRenderImages(!visConfig.renderImages());
		} else if (source.getText().equals(SHOW_TIME_GL)) {
			// toggle draw time in GL
			visConfig.setDrawTime(!visConfig.drawTime());
		} else if (source.getText().equals(SHOW_SCALE_BAR)) {
			// toggle draw Overlays
			visConfig.setDrawScaleBar(!visConfig.drawScaleBar());
		} else if (source.getText().equals(SHOW_TRANSIT_FACILITIES)) {
			// toggle draw Overlays
			visConfig.setDrawTransitFacilities(!visConfig.isDrawTransitFacilities());
		} 
	}

	public void setVisConfig(OTFVisConfigGroup visConfig) {
		this.visConfig = visConfig;
		initGUI();
	}

}
