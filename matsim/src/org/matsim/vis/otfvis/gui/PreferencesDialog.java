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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.matsim.core.gbl.Gbl;
import org.matsim.vis.otfvis.interfaces.OTFSettingsSaver;
import org.matsim.vis.otfvis.opengl.OnTheFlyClientFileQuad;
import org.matsim.vis.otfvis.opengl.gui.PreferencesDialog2;

/**
 * The class responsible for drawing the PreferencesDialog.
 * This class is not used anymore, but the PreferencesDialog2 that inherits from this class.
 * 
 * @author dstrippgen
 *
 */
public class PreferencesDialog extends javax.swing.JDialog implements ChangeListener, ActionListener {

	public static Class preDialogClass = PreferencesDialog2.class;

	protected transient OTFVisConfig cfg;
	private JComboBox rightMFunc;
	private JComboBox middleMFunc;
	private JComboBox leftMFunc;
	protected OTFHostControlBar host = null;
	private JSlider agentSizeSlider = null;

	private JSlider linkWidthSlider;
	private JSlider delaySlider = null;

	/**
	 * Auto-generated main method to display this JDialog
	 */
	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame();
				PreferencesDialog inst = new PreferencesDialog(frame, new OTFVisConfig(), null);
				inst.setVisible(true);
			}
		});
	}

	public PreferencesDialog(final JFrame frame, final OTFVisConfig config, final OTFHostControlBar mother) {
		super(frame);
		this.cfg = config;
		this.host = mother;
		initGUI();
	}

	protected void initGUI() {
		getContentPane().setLayout(null);
		this.setResizable(false);
		setSize(480, 340);

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
				leftMFuncModel.setSelectedItem(this.cfg.getLeftMouseFunc());
				this.leftMFunc = new JComboBox();
				panel.add(getLeftMFunc());
				this.leftMFunc.setModel(leftMFuncModel);
				this.leftMFunc.setBounds(70, 20, 120, 27);
				this.leftMFunc.addActionListener(this);
			}
			{
				ComboBoxModel jComboBox1Model = new DefaultComboBoxModel(new String[] { "Zoom", "Pan", "Select" });
				jComboBox1Model.setSelectedItem(this.cfg.getMiddleMouseFunc());
				this.middleMFunc = new JComboBox();
				panel.add(this.middleMFunc);
				this.middleMFunc.setModel(jComboBox1Model);
				this.middleMFunc.setBounds(70, 50, 120, 27);
				this.middleMFunc.addActionListener(this);
			}
			{
				ComboBoxModel jComboBox2Model = new DefaultComboBoxModel(new String[] { "Menu", "Zoom", "Pan", "Select" });
				jComboBox2Model.setSelectedItem(this.cfg.getRightMouseFunc());
				this.rightMFunc = new JComboBox();
				panel.add(this.rightMFunc);
				this.rightMFunc.setModel(jComboBox2Model);
				this.rightMFunc.setBounds(70, 80, 120, 27);
				this.rightMFunc.addActionListener(this);
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
			BoundedRangeModel model = new DefaultBoundedRangeModel((int) cfg.getAgentSize(), 0, 10, 300);
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
		 BoundedRangeModel model2 = new DefaultBoundedRangeModel(30,0,0,100);
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
		 BoundedRangeModel model2 = new DefaultBoundedRangeModel(cfg.getDelay_ms(),10,0,500);
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
		this.cfg = (OTFVisConfig)Gbl.getConfig().getModule("otfvis");
		if (e.getSource() == this.agentSizeSlider) {
			this.cfg.setAgentSize(this.agentSizeSlider.getValue());
			if (this.host != null)
				this.host.invalidateHandlers();
			System.out.println("val: " + this.agentSizeSlider.getValue());
		 } else if (e.getSource() == this.linkWidthSlider) {
			 this.cfg.setLinkWidth(this.linkWidthSlider.getValue());
			 if (this.host != null){
				host.redrawHandlers();
			 }
		 } else if (e.getSource() == this.delaySlider) {
			 this.cfg.setDelay_ms(this.delaySlider.getValue());
			 if (this.host != null){
				host.redrawHandlers();
			 }
			 //System.out.println("val: "+ this.linkWidthSlider.getValue());
		}
	}

	public JComboBox getLeftMFunc() {
		return this.leftMFunc;
	}

	public JComboBox getRightMFunc() {
		return this.rightMFunc;
	}

	public JComboBox getMiddleMFunc() {
		return this.middleMFunc;
	}

	public static PreferencesDialog buildMenu(final JFrame frame, final OTFVisConfig config, final OTFHostControlBar host, final OTFSettingsSaver save) {
		PreferencesDialog preferencesDialog = new PreferencesDialog(frame, config, host);
		Class partypes[] = new Class[3];
		partypes[0] = JFrame.class;
		partypes[1] = OTFVisConfig.class;
		partypes[2] = OTFHostControlBar.class;
		try {
			Constructor ct = preDialogClass.getConstructor(partypes);
			Object arglist[] = new Object[3];
			arglist[0] = frame;
			arglist[1] = config;
			arglist[2] = host;
			preferencesDialog = (PreferencesDialog) ct.newInstance(arglist);

		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		preferencesDialog.buildMenu(frame, preferencesDialog, save);
		return preferencesDialog;
	}

	public void buildMenu(final JFrame frame, final PreferencesDialog preferencesDialog, final OTFSettingsSaver save) {
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		Action prefAction = new AbstractAction() {
			{
				putValue(Action.NAME, "Preferences...");
				putValue(Action.MNEMONIC_KEY, 0);
			}

			public void actionPerformed(final ActionEvent e) {
				preferencesDialog.setVisible(true);
			}
		};
		fileMenu.add(prefAction);
		if (!preferencesDialog.host.isLiveHost()) {
			Action saveAction = new AbstractAction() {
				{
					putValue(Action.NAME, "Save Settings to mvi");
					putValue(Action.MNEMONIC_KEY, 1);
				}

				public void actionPerformed(final ActionEvent e) {
					save.saveSettings();
				}
			};
			fileMenu.add(saveAction);
		}
		Action saveAsAction = new AbstractAction() {
			{
				putValue(Action.NAME, "Save Settings as...");
				putValue(Action.MNEMONIC_KEY, 1);
			}

			public void actionPerformed(final ActionEvent e) {
				save.saveSettingsAs();
			}
		};
		fileMenu.add(saveAsAction);

		Action openAction = new AbstractAction() {
			{
				putValue(Action.NAME, "Open Settings...");
				putValue(Action.MNEMONIC_KEY, 1);
			}

			public void actionPerformed(final ActionEvent e) {
				save.readSettings();
				if (frame != null) {
					frame.getContentPane().invalidate();
					OTFVisConfig conf = (OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);
					PreferencesDialog.buildMenu(frame, conf, host, save);
				}
			}
		};
		fileMenu.add(openAction);

		Action exitAction = new AbstractAction("Quit") {
			public void actionPerformed(ActionEvent e) {
				OnTheFlyClientFileQuad.endProgram(0);
			}
		};
		fileMenu.add(exitAction);

		frame.setJMenuBar(menuBar);
		SwingUtilities.updateComponentTreeUI(frame);
	}

	public void actionPerformed(final ActionEvent e) {
		if (e.getSource() instanceof JComboBox) {
			JComboBox cb = (JComboBox) e.getSource();
			String newFunc = (String) cb.getSelectedItem();

			if (cb == this.leftMFunc) {
				this.cfg.setLeftMouseFunc(newFunc);
			} else if (cb == this.middleMFunc) {
				this.cfg.setMiddleMouseFunc(newFunc);
			} else if (cb == this.rightMFunc) {
				this.cfg.setRightMouseFunc(newFunc);
			}
		} else if (e.getSource() instanceof JButton) {
			if (e.getActionCommand().equals("backgroundColor")) {
				JPanel frame = new JPanel();
				Color c = JColorChooser.showDialog(frame, "Choose the background color", this.cfg.getBackgroundColor());
				if (c != null) {
					this.cfg.setBackgroundColor(c);
					if (this.host != null) {
						this.host.invalidateHandlers();
					}
				}
			}
			if (e.getActionCommand() == "networkColor") {
				JPanel frame = new JPanel();
				Color c = JColorChooser.showDialog(frame, "Choose the network color", this.cfg.getNetworkColor());
				if (c != null) {
					this.cfg.setNetworkColor(c);
					if (this.host != null) {
						this.host.invalidateHandlers();
					}
				}
			}
		}
	}

}
