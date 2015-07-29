/* *********************************************************************** *
 * project: org.matsim.*
 * MyMapViewer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.evacuation.scenariomanager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.io.DepartureTimeDistribution;
import org.matsim.contrib.evacuation.model.AbstractModule;
import org.matsim.contrib.evacuation.model.Constants.ModuleType;
import org.matsim.contrib.evacuation.model.config.EvacuationConfigModule;
import org.matsim.contrib.evacuation.view.DefaultOpenDialog;
import org.matsim.contrib.evacuation.view.DefaultSaveDialog;

/**
 * @author wdoering
 * 
 */
class ScenarioXMLMask extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;
	// Fields shall be moved to ScenarioXMLToolBox later

	// elements are defined in the order they appear in the mask

	private JLabel labelCurrentFile;

	// osm file
	private JLabel labelOSMFilePath;
	private JButton btOSMBrowse;

	// main traffic type
	private JComboBox boxTrafficType;

	// evac file
	private JLabel labelEvacFilePath;
	private JButton btEvacBrowse;

	// population file
	private JLabel labelPopFilePath;
	private JButton btPopBrowse;

	// output directory
	private JLabel labelOutDirPath;
	private JButton btOutDirBrowse;

	// sample size
	private JTextField textFieldSampleSize;
	public JLabel labelSampleSize;
	private JSlider sliderSampleSize;

	// dep time
	private JComboBox boxDepTime;

	// sigma
	private JLabel labelSigma;
	private JTextField textFieldSigma;

	// mu
	private JLabel labelMu;
	private JTextField textFieldMu;

	// earliest
	private JLabel labelEarliest;
	private JTextField textFieldEarliest;

	// latest
	private JLabel labelLatest;
	private JTextField textFieldLatest;

	private final Controller controller;

	private JButton btNew;
	private JButton btOpen;
	private JButton btSave;
	private EvacuationConfigModule gcm;
	private boolean configOpened;
	private String fileLocation;
	private String[] trafficTypeStrings;

	private String[] distTypeStrings;

	private void initComponents() {

		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		Border border = BorderFactory.createLineBorder(Color.BLACK, 1);
		Border emptyBorder = BorderFactory.createEmptyBorder(15, 15, 15, 15);

		Dimension inputSize = new Dimension(400, 30);
		Dimension varInputSize = new Dimension(100, 20);

		JPanel panelCurrentFile = new JPanel();
		this.labelCurrentFile = new JLabel(" / ");
		this.labelCurrentFile.setForeground(Color.GRAY);
		final String lcf = this.controller.getLocale().labelCurrentFile();
		panelCurrentFile.setBorder(BorderFactory
				.createTitledBorder(border, lcf));
		panelCurrentFile.add(this.labelCurrentFile);

		// OSM panel
		JPanel panelOSM = new JPanel();
		this.labelOSMFilePath = new JLabel(" / ");
		this.labelOSMFilePath.setForeground(Color.GRAY);
		this.labelOSMFilePath.setPreferredSize(inputSize);
		this.btOSMBrowse = new JButton(this.controller.getLocale().btSet());
		this.btOSMBrowse.addActionListener(this);
		this.btOSMBrowse.setActionCommand(this.controller.getLocale()
				.labelNetworkFile());
		panelOSM.setBorder(BorderFactory.createTitledBorder(border,
				this.controller.getLocale().labelNetworkFile()));
		panelOSM.add(this.labelOSMFilePath);
		panelOSM.add(this.btOSMBrowse);

		// // prepare main traffic type elements
		// MainTrafficTypeType[] trafficTypeElements =
		// MainTrafficTypeType.values();
		// this.trafficTypeStrings = new String[trafficTypeElements.length];
		// for (int i = 0; i < trafficTypeElements.length; i++)
		// this.trafficTypeStrings[i] = trafficTypeElements[i].toString();
		// // TODO: englishElements convert english to native language elements

		// TODO this is already defined below -- needs to be cleand up!! [GL Sep
		// '14]
		this.trafficTypeStrings = new String[] { "vehicular", "pedestrian" };

		// main traffic type
		JPanel panelTrafficType = new JPanel();
		this.boxTrafficType = new JComboBox(this.trafficTypeStrings);
		this.boxTrafficType.setPreferredSize(inputSize);
		this.boxTrafficType.setActionCommand(this.controller.getLocale()
				.labelTrafficType());
		this.boxTrafficType.addActionListener(this);

		panelTrafficType.setBorder(BorderFactory.createTitledBorder(border,
				this.controller.getLocale().labelTrafficType()));
		panelTrafficType.add(this.boxTrafficType);

		JPanel panelEvac = new JPanel();
		this.labelEvacFilePath = new JLabel(this.controller.getLocale()
				.getLeaveEmptyToCreateNew());
		this.labelEvacFilePath.setForeground(Color.GRAY);
		this.labelEvacFilePath.setPreferredSize(inputSize);
		this.btEvacBrowse = new JButton(this.controller.getLocale().btSet());
		this.btEvacBrowse.addActionListener(this);
		this.btEvacBrowse.setActionCommand(this.controller.getLocale()
				.labelEvacFile());

		// evacuation area and population
		panelEvac.setBorder(BorderFactory.createTitledBorder(border,
				this.controller.getLocale().labelEvacFile()));
		panelEvac.add(this.labelEvacFilePath);
		panelEvac.add(this.btEvacBrowse);

		JPanel panelPop = new JPanel();
		this.labelPopFilePath = new JLabel(this.controller.getLocale()
				.getLeaveEmptyToCreateNew());
		this.labelPopFilePath.setForeground(Color.GRAY);
		this.labelPopFilePath.setPreferredSize(inputSize);
		this.btPopBrowse = new JButton(this.controller.getLocale().btSet());
		this.btPopBrowse.addActionListener(this);
		this.btPopBrowse.setActionCommand(this.controller.getLocale()
				.labelPopFile());
		panelPop.setBorder(BorderFactory.createTitledBorder(border,
				this.controller.getLocale().labelPopFile()));
		panelPop.add(this.labelPopFilePath);
		panelPop.add(this.btPopBrowse);

		// output directory
		JPanel panelOutDir = new JPanel();
		this.labelOutDirPath = new JLabel(" / ");
		this.labelOutDirPath.setPreferredSize(inputSize);
		this.labelOutDirPath.setForeground(Color.GRAY);
		this.btOutDirBrowse = new JButton(this.controller.getLocale().btSet());
		this.btOutDirBrowse.addActionListener(this);
		this.btOutDirBrowse.setActionCommand(this.controller.getLocale()
				.labelOutDir());

		panelOutDir.setBorder(BorderFactory.createTitledBorder(border,
				this.controller.getLocale().labelOutDir()));
		panelOutDir.add(this.labelOutDirPath);
		panelOutDir.add(this.btOutDirBrowse);

		// sample size
		JPanel panelSampleSize = new JPanel();
		this.labelSampleSize = new JLabel("0.787");
		this.labelSampleSize.setPreferredSize(varInputSize);
		this.sliderSampleSize = new JSlider(1, 1000, 787);
		this.sliderSampleSize.setOrientation(JSlider.HORIZONTAL);
		this.sliderSampleSize.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				ScenarioXMLMask.this.labelSampleSize.setText(""
						+ (ScenarioXMLMask.this.sliderSampleSize.getValue() / 1000d));
				checkSaveConditions();
			}
		});
		panelSampleSize.setBorder(BorderFactory.createTitledBorder(border,
				this.controller.getLocale().labelSampleSize()));
		panelSampleSize.add(this.labelSampleSize);
		panelSampleSize.add(this.sliderSampleSize);

		// prepare main traffic type elements
		// DistributionType[] distTypeElements = DistributionType.values();
		// this.distTypeStrings = new String[distTypeElements.length];
		// for (int i = 0; i < distTypeElements.length; i++)
		// this.distTypeStrings[i] = distTypeElements[i].toString();
		// TODO: think about this [GL Sep '14]
		this.distTypeStrings = new String[] { "normal", "log-normal",
				"dirac-delta" };

		// TODO: englishElements convert english to native language elements

		// departure time distribution
		JPanel panelDepTime = new JPanel();
		panelDepTime
				.setLayout(new BoxLayout(panelDepTime, BoxLayout.PAGE_AXIS));
		this.boxDepTime = new JComboBox(this.distTypeStrings);
		this.boxDepTime.setSelectedIndex(1);
		this.boxDepTime.setBorder(emptyBorder);
		this.boxDepTime.setActionCommand(this.controller.getLocale()
				.labelDepTime());
		this.boxDepTime.addActionListener(this);
		// boxDepTime.setPreferredSize(inputSize);
		// boxDepTime.setMinimumSize(inputSize);

		JPanel panelParams = new JPanel();
		panelParams.setLayout(new GridLayout(2, 4));
		this.labelSigma = new JLabel(" "
				+ this.controller.getLocale().labelSigma());
		this.textFieldSigma = new JTextField("0.25");
		this.textFieldSigma.setPreferredSize(varInputSize);
		// textFieldSigma.setBorder(emptyBorder);

		this.labelMu = new JLabel(" " + this.controller.getLocale().labelMu());
		this.textFieldMu = new JTextField("0.1");
		this.textFieldMu.setPreferredSize(varInputSize);
		// textFieldMu.setBorder(emptyBorder);

		this.labelEarliest = new JLabel(" "
				+ this.controller.getLocale().labelEarliest());
		this.textFieldEarliest = new JTextField("0.04315872");
		this.textFieldEarliest.setPreferredSize(varInputSize);
		// textFieldEarliest.setBorder(emptyBorder);

		this.labelLatest = new JLabel(" "
				+ this.controller.getLocale().labelLatest());
		this.textFieldLatest = new JTextField("1.3783154");
		this.textFieldLatest.setPreferredSize(varInputSize);
		// textFieldLatest.setBorder(emptyBorder);

		JPanel panelIO = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panelIO.setBackground(new Color(190, 190, 190));
		this.btNew = new JButton(this.controller.getLocale().btNew());
		this.btOpen = new JButton(this.controller.getLocale().btOpen());
		this.btSave = new JButton(this.controller.getLocale().btSave());
		this.btNew.addActionListener(this);
		this.btOpen.addActionListener(this);
		this.btSave.addActionListener(this);
		this.btSave.setEnabled(false);

		panelIO.add(this.btNew);
		panelIO.add(this.btOpen);
		panelIO.add(this.btSave);

		panelParams.add(this.labelSigma);
		panelParams.add(this.textFieldSigma);
		panelParams.add(this.labelMu);
		panelParams.add(this.textFieldMu);
		panelParams.add(this.labelEarliest);
		panelParams.add(this.textFieldEarliest);
		panelParams.add(this.labelLatest);
		panelParams.add(this.textFieldLatest);

		panelDepTime.setBorder(BorderFactory.createTitledBorder(border,
				this.controller.getLocale().labelDepTime()));
		panelDepTime.add(this.boxDepTime);
		panelDepTime.add(panelParams);

		this.add(panelCurrentFile);
		this.add(panelOSM);
		this.add(panelTrafficType);
		this.add(panelEvac);
		this.add(panelPop);
		this.add(panelOutDir);
		this.add(panelSampleSize);
		this.add(panelDepTime);
		this.add(panelIO);

	}

	ScenarioXMLMask(AbstractModule module, Controller controller) {
		this.controller = controller;
		this.setLayout(new BorderLayout());
		initComponents();
	}

	public void readConfig() {
		EvacuationConfigModule gcm = this.controller.getEvacuationConfigModule();
		// String nfn = gcm.getNetworkFileName();
		// this.textFieldNetworkFile.setText(nfn);
		// String mtt = gcm.getMainTrafficType();
		// this.textFieldTrafficType.setText(mtt);

		// this.btRun.setEnabled(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getActionCommand().equals(
				this.controller.getLocale().labelNetworkFile())) { // osm
			DefaultOpenDialog openDialog = new DefaultOpenDialog(
					this.controller, "osm", "osm file (*.osm)", false);
			openDialog.showDialog(this.controller.getParentComponent(), null);
			if (openDialog.getSelectedFile() != null) {
				this.labelOSMFilePath.setText(openDialog.getSelectedFile()
						.getAbsolutePath());
			}
		} else if (e.getActionCommand().equals( // evacuation area
				this.controller.getLocale().labelEvacFile())) {
			DefaultSaveDialog saveDialog = new DefaultSaveDialog(
					this.controller, "shp", "area file (*.shp)", false);
			saveDialog.showDialog(this.controller.getParentComponent(), null);
			if (saveDialog.getSelectedFile() != null) {
				if (saveDialog.getSelectedFile().getAbsolutePath()
						.equals(this.labelPopFilePath.getText()))
					JOptionPane.showMessageDialog(this, this.controller
							.getLocale().msgSameFiles(), "",
							JOptionPane.ERROR_MESSAGE);
				else
					this.labelEvacFilePath.setText(saveDialog.getSelectedFile()
							.getAbsolutePath());
			}

		} else if (e.getActionCommand().equals( // population
				this.controller.getLocale().labelPopFile())) {

			DefaultSaveDialog saveDialog = new DefaultSaveDialog(
					this.controller, "shp", "population file (*.shp)", false);
			saveDialog.showDialog(this.controller.getParentComponent(), null);
			if (saveDialog.getSelectedFile() != null) {
				if (saveDialog.getSelectedFile().getAbsolutePath()
						.equals(this.labelEvacFilePath.getText()))
					JOptionPane.showMessageDialog(this, this.controller
							.getLocale().msgSameFiles(), "",
							JOptionPane.ERROR_MESSAGE);
				else
					this.labelPopFilePath.setText(saveDialog.getSelectedFile()
							.getAbsolutePath());
			}
		} else if (e.getActionCommand().equals( // output
				this.controller.getLocale().labelOutDir())) {
			DefaultOpenDialog openDialog = new DefaultOpenDialog(
					this.controller, "", "directory", true);
			openDialog.showDialog(this.controller.getParentComponent(),
					"select output directory");
			if (openDialog.getSelectedFile() != null) {
				this.labelOutDirPath.setText(openDialog.getSelectedFile()
						.getAbsolutePath());
				this.gcm = null;
				this.configOpened = true;
			}

		} else if (e.getActionCommand().equals(
				this.controller.getLocale().btNew())) { // new
			DefaultSaveDialog save = new DefaultSaveDialog(this.controller,
					"xml", "Evacuation config file", true);

			save.showDialog(this.controller.getParentComponent(),
					"Save Evacuation config file");
			if (save.getSelectedFile() != null) {
				this.labelCurrentFile.setText(save.getSelectedFile()
						.getAbsolutePath());
				this.fileLocation = save.getSelectedFile().getAbsolutePath();
				this.configOpened = true;

				this.controller.setGoalAchieved(false);

			}

		} else if (e.getActionCommand().equals(
				this.controller.getLocale().btOpen())) // Open
		{
			this.setEnabled(false);
			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			setMaskEnabled(false);

			SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

				@Override
				protected String doInBackground() {
					final boolean open = ScenarioXMLMask.this.controller
							.openEvacuationConfig();

					if (open) // open Evacuation Config
					{
						ScenarioXMLMask.this.fileLocation = ScenarioXMLMask.this.controller
								.getEvacuationFile();
						ScenarioXMLMask.this.gcm = ScenarioXMLMask.this.controller
								.getEvacuationConfigModule();
						ScenarioXMLMask.this.configOpened = true;
						ScenarioXMLMask.this.controller.setGoalAchieved(true);
						ScenarioXMLMask.this.btSave.setEnabled(false);

						// enable other modules if shape files exist
						if (!ScenarioXMLMask.this.controller.isStandAlone()) {
							File evacFile = new File(
									ScenarioXMLMask.this.gcm
											.getEvacuationAreaFileName());
							if (evacFile.exists())
								ScenarioXMLMask.this.controller
										.enableModule(ModuleType.POPULATION);
							File popFile = new File(
									ScenarioXMLMask.this.gcm
											.getEvacuationAreaFileName());
							if (popFile.exists()) {
								ScenarioXMLMask.this.controller
										.enableModule(ModuleType.EVACUATIONSCENARIO);
								ScenarioXMLMask.this.controller
										.setPopulationFileOpened(true);
							}

							ScenarioXMLMask.this.controller.updateParentUI();
						}

					} else {
					}

					return "";
				}

				@Override
				protected void done() {
					setEnabled(true);
					setCursor(Cursor.getDefaultCursor());
					updateMask();
					setMaskEnabled(true);

				}
			};

			worker.execute();

		} else if (e.getActionCommand().equals(
				this.controller.getLocale().btSave())) // Save
		{
			if (this.configOpened) {
				if (this.gcm == null)
					this.gcm = new EvacuationConfigModule("evacuation");// ,
																// fileLocation);

				File f = new File(this.labelOSMFilePath.getText());
				String osmPath = f.getParent();
				final String matsimOutputDir = this.labelOutDirPath.getText();
				this.gcm.setOutputDir(matsimOutputDir + "/");

				if (this.labelPopFilePath.getText().equals(
						this.controller.getLocale().getLeaveEmptyToCreateNew())) {
					this.gcm.setPopulationFileName(osmPath + "/population.shp");
				} else {
					this.gcm.setPopulationFileName(this.labelPopFilePath
							.getText());
				}

				if (this.labelEvacFilePath.getText().equals(
						this.controller.getLocale().getLeaveEmptyToCreateNew())) {
					this.gcm.setEvacuationAreaFileName(osmPath + "/area.shp");

				} else {
					this.gcm.setEvacuationAreaFileName(this.labelEvacFilePath
							.getText());
				}

				String osmfile = this.controller.getCurrentOSMFile();
				osmfile = this.labelOSMFilePath.getText();
				this.gcm.setNetworkFileName(osmfile);
				this.gcm.setMainTrafficType(this.boxTrafficType
						.getSelectedItem().toString().toLowerCase());
				DepartureTimeDistribution dtdt = new DepartureTimeDistribution();
				dtdt.setDistribution(this.boxDepTime.getSelectedItem()
						.toString());
				dtdt.setSigma(Double.valueOf(this.textFieldSigma.getText()));
				dtdt.setMu(Double.valueOf(this.textFieldMu.getText()));
				dtdt.setEarliest(Double.valueOf(this.textFieldEarliest
						.getText()));
				dtdt.setLatest(Double.valueOf(this.textFieldLatest.getText()));
				this.gcm.setDepartureTimeDistribution(dtdt);
				this.gcm.setSampleSize(this.labelSampleSize.getText());

				this.controller.setEvacuationConfigModule(this.gcm);
				this.controller.setCurrentOSMFile(osmfile);

				boolean writeConfig = this.controller.writeEvacuationConfig(
						this.gcm, this.fileLocation);
				this.controller.evacuationEvacuationConfig(new File(this.fileLocation));

				if (writeConfig) {
					this.controller.setGoalAchieved(true);
					this.btSave.setEnabled(false);
				}
			}

		}

		checkSaveConditions();

	}

	private void checkSaveConditions() {
		if ((this.configOpened)
				&& (!this.labelCurrentFile.getText().equals(" / "))
				&& (!this.labelOSMFilePath.getText().equals(" / "))
				&& (!this.labelOutDirPath.getText().equals(" / ")))
			this.btSave.setEnabled(true);
		else
			this.btSave.setEnabled(false);
	}

	public void updateMask() {
		if ((this.gcm == null) || (this.gcm.getNetworkFileName() == null))
			return;
		this.labelCurrentFile.setText(this.fileLocation);
		this.labelOSMFilePath.setText(this.gcm.getNetworkFileName());
		this.labelEvacFilePath.setText(this.gcm.getEvacuationAreaFileName());
		this.labelPopFilePath.setText(this.gcm.getPopulationFileName());
		this.labelOutDirPath.setText(this.gcm.getOutputDir());

		String gcmMTT = this.gcm.getMainTrafficType().toLowerCase();
		// System.out.println(gcmMTT + " | " + trafficTypeStrings[0]);
		for (int i = 0; i < this.trafficTypeStrings.length; i++) {
			if (this.trafficTypeStrings[i].toLowerCase().equals(gcmMTT))
				this.boxTrafficType.setSelectedIndex(i);
		}

		DepartureTimeDistribution gcmDep = this.gcm
				.getDepartureTimeDistribution();
		String gcmDepType = gcmDep.getDistribution();// .toString().toLowerCase();
		// System.out.println(gcmDepType + " | " + distTypeStrings[0]);
		for (int i = 0; i < this.distTypeStrings.length; i++) {
			if (this.distTypeStrings[i].toLowerCase().equals(gcmDepType))
				this.boxDepTime.setSelectedIndex(i);
		}

		this.textFieldSigma.setText(gcmDep.getSigma() + "");
		this.textFieldMu.setText(gcmDep.getMu() + "");
		this.textFieldEarliest.setText(gcmDep.getEarliest() + "");
		this.textFieldLatest.setText(gcmDep.getLatest() + "");

		this.sliderSampleSize.setValue((int) (this.gcm.getSampleSize() * 1000));

	}

	public void setMaskEnabled(boolean b) {
		this.btEvacBrowse.setEnabled(b);
		this.btPopBrowse.setEnabled(b);
		this.btOutDirBrowse.setEnabled(b);
		this.btOSMBrowse.setEnabled(b);
		this.sliderSampleSize.setEnabled(b);
		this.textFieldEarliest.setEnabled(b);
		this.textFieldLatest.setEnabled(b);
		this.textFieldMu.setEnabled(b);
		this.textFieldSigma.setEnabled(b);

		this.boxDepTime.setEnabled(b);
		this.boxTrafficType.setEnabled(b);
		this.btOpen.setEnabled(b);
		this.btNew.setEnabled(b);
		this.btSave.setEnabled(b);

	}

}