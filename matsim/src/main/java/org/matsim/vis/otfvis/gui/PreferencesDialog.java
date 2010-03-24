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
import java.rmi.RemoteException;

import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The class responsible for drawing the PreferencesDialog.
 * 
 * @author dstrippgen
 *
 */
public class PreferencesDialog extends javax.swing.JDialog implements ChangeListener, ActionListener, ItemListener {

	private static final long serialVersionUID = 5778562849300898138L;
	
	private OTFVisConfig visConfig;
	
	private JComboBox rightMFunc;
	
	private JComboBox middleMFunc;
	
	private JComboBox leftMFunc;
	
	private OTFHostControlBar host = null;
	
	private JSlider agentSizeSlider = null;

	private JSlider linkWidthSlider;
	
	private JSlider delaySlider = null;

	public PreferencesDialog(final OTFFrame frame, final OTFHostControlBar mother) {
		super(frame);
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
			if(host.getOTFHostControl().isLiveHost()) {
				synchBox = new JCheckBox("show non-moving items");
				synchBox.setSelected(visConfig.isShowParking());
				synchBox.addItemListener(this);
				synchBox.setBounds(10, 20, 200, 31);
				synchBox.setVisible(true);
				panel.add(synchBox);
			}
			if((host.getOTFHostControl().isLiveHost())||((visConfig.getFileVersion()>=1) &&(visConfig.getFileMinorVersion()>=4))) {
				synchBox = new JCheckBox("show link Ids");
				synchBox.setSelected(visConfig.drawLinkIds());
				synchBox.addItemListener(this);
				synchBox.setBounds(10, 40, 200, 31);
				synchBox.setVisible(true);
				panel.add(synchBox);
			}

			synchBox = new JCheckBox("show overlays");
			synchBox.setSelected(visConfig.drawOverlays());
			synchBox.addItemListener(this);
			synchBox.setBounds(10, 60, 200, 31);
			synchBox.setVisible(true);
			panel.add(synchBox);

			synchBox = new JCheckBox("show time GL");
			synchBox.setSelected(visConfig.drawTime());
			synchBox.addItemListener(this);
			synchBox.setBounds(10, 80, 200, 31);
			panel.add(synchBox);

			synchBox = new JCheckBox("save jpg frames");
			synchBox.setSelected(visConfig.renderImages());
			synchBox.addItemListener(this);
			synchBox.setBounds(10, 100, 200, 31);
			panel.add(synchBox);
			synchBox = new JCheckBox("allow caching");
			synchBox.setSelected(visConfig.isCachingAllowed());
			synchBox.addItemListener(this);
			synchBox.setBounds(10, 120, 200, 31);
			panel.add(synchBox);
			synchBox = new JCheckBox("show scale bar");
			synchBox.setSelected(visConfig.drawScaleBar());
			synchBox.addItemListener(this);
			synchBox.setBounds(10, 140, 200, 31);
			synchBox.setVisible(true);
			panel.add(synchBox);
			if (host.getOTFHostControl().isLiveHost()) {
				synchBox = new JCheckBox("show transit facilities");
				synchBox.setSelected(visConfig.drawTransitFacilities());
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
			this.agentSizeSlider = new JSlider();
			BoundedRangeModel model = new DefaultBoundedRangeModel((int) visConfig.getAgentSize(), 0, 10, 300);
			this.agentSizeSlider.setModel(model);
			this.agentSizeSlider.setLabelTable(this.agentSizeSlider.createStandardLabels(100, 100));
			this.agentSizeSlider.setPaintLabels(true);
			this.agentSizeSlider.setBounds(90, 140, 153, 45);
			this.agentSizeSlider.addChangeListener(this);
			getContentPane().add(label);
			getContentPane().add(this.agentSizeSlider);
		}

		 //Link Width
		 {
		 JLabel label = new JLabel();
		 getContentPane().add(label);
		 label.setText("LinkWidth:");
		 label.setBounds(10, 195, 80, 31);
		 this.linkWidthSlider = new JSlider();
		 BoundedRangeModel model2 = new DefaultBoundedRangeModel((int) visConfig.getLinkWidth(),0,0,100);
		 this.linkWidthSlider.setModel(model2);
		 this.linkWidthSlider.setLabelTable(this.linkWidthSlider.
		 createStandardLabels(20, 0));
		 this.linkWidthSlider.setPaintLabels(true);
		 this.linkWidthSlider.setBounds(90, 190, 153, 45);
		 this.linkWidthSlider.addChangeListener(this);
		 getContentPane().add(label);
		 getContentPane().add(this.linkWidthSlider);
		 }

		 //Delay ms
		 {
		 JLabel label = new JLabel();
		 getContentPane().add(label);
		 label.setText("AnimSpeed:");
		 label.setBounds(10, 245, 110, 31);
		 this.delaySlider = new JSlider();
		 BoundedRangeModel model2 = new DefaultBoundedRangeModel(visConfig.getDelay_ms(),10,0,500);
		 this.delaySlider.setModel(model2);
		 this.delaySlider.setLabelTable(this.delaySlider.
		 createStandardLabels(500, 00));
		 this.delaySlider.setPaintLabels(true);
		 this.delaySlider.setBounds(120, 240, 123, 45);
		 this.delaySlider.addChangeListener(this);
		 getContentPane().add(label);
		 getContentPane().add(this.delaySlider);
		 }

	}

	public void stateChanged(final ChangeEvent e) {
		if (e.getSource() == this.agentSizeSlider) {
			this.visConfig.setAgentSize(this.agentSizeSlider.getValue());
			if (this.host != null)
				this.host.invalidateDrawers();
		 } else if (e.getSource() == this.linkWidthSlider) {
			 this.visConfig.setLinkWidth(this.linkWidthSlider.getValue());
			 if (this.host != null){
				host.redrawDrawers();
			 }
		 } else if (e.getSource() == this.delaySlider) {
			 this.visConfig.setDelay_ms(this.delaySlider.getValue());
			 if (this.host != null){
				host.redrawDrawers();
			 }
		}
	}

	

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
						this.host.invalidateDrawers();
					}
				}
			}
			if (e.getActionCommand() == "networkColor") {
				JPanel frame = new JPanel();
				Color c = JColorChooser.showDialog(frame, "Choose the network color", this.visConfig.getNetworkColor());
				if (c != null) {
					this.visConfig.setNetworkColor(c);
					if (this.host != null) {
						this.host.invalidateDrawers();
					}
				}
			}
		}
	}

	public void itemStateChanged(ItemEvent e) {
		JCheckBox source = (JCheckBox)e.getItemSelectable();
		if (source.getText().equals("show non-moving items")) {
			visConfig.setShowParking(e.getStateChange() != ItemEvent.DESELECTED);
			visConfig.setShowParking(!visConfig.isShowParking());
			if (host != null) {
				try {
					host.getOTFHostControl().getOTFServer().toggleShowParking();
				} catch (RemoteException e1) {
					throw new RuntimeException(e1);
				}
				host.clearCaches();
				host.redrawDrawers();
			}
		} else if (source.getText().equals("show link Ids")) {
			// toggle draw link Ids
			visConfig.setDrawLinkIds(!visConfig.drawLinkIds());
		} else if (source.getText().equals("show overlays")) {
			// toggle draw Overlays
			visConfig.setDrawOverlays(!visConfig.drawOverlays());
		} else if (source.getText().equals("save jpg frames")) {
			// toggle save jpgs
			visConfig.setRenderImages(!visConfig.renderImages());
		} else if (source.getText().equals("show time GL")) {
			// toggle draw time in GL
			visConfig.setDrawTime(!visConfig.drawTime());
		} else if (source.getText().equals("allow caching")) {
			// toggle caching allowed
			visConfig.setCachingAllowed(!visConfig.isCachingAllowed());
		} else if (source.getText().equals("show scale bar")) {
			// toggle draw Overlays
			visConfig.setDrawScaleBar(!visConfig.drawScaleBar());
		} else if (source.getText().equals("show transit facilities")) {
			// toggle draw Overlays
			visConfig.setDrawTransitFacilities(!visConfig.drawTransitFacilities());
		} 
		}

	public void setVisConfig(OTFVisConfig visConfig) {
		this.visConfig = visConfig;
		initGUI();
	}
	
}
