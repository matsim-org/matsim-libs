
/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationSampler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.run.gui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.UnicodeInputStream;

import com.github.luben.zstd.ZstdInputStream;

/**
 * @author mrieser / Senozon AG
 */
final class PopulationSampler extends JDialog {

	private final static Logger log = LogManager.getLogger(PopulationSampler.class);

	private static final long serialVersionUID = 1L;

	private JTextField txtPath;
	private JSpinner pctSpinner;
	private JButton btnChoose;
	private JButton btnCreateSample;

	PopulationSampler(JFrame parent) {
		super(parent);
		setTitle("Create Population Sample");

		this.btnChoose = new JButton("Choose…");

		JLabel lblinput = new JLabel("Input Population:");
		this.txtPath = new JTextField("");
		JLabel lblpercentage = new JLabel("Sample Size:");
		this.pctSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
		JLabel lblPercentage = new JLabel("%");
		btnCreateSample = new JButton("Create Sample…");

		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
						.addContainerGap()
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addGroup(groupLayout.createSequentialGroup()
										.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
												.addComponent(lblinput)
												.addComponent(lblpercentage))
										.addPreferredGap(ComponentPlacement.RELATED)
										.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
												.addGroup(groupLayout.createSequentialGroup()
														.addComponent(pctSpinner, GroupLayout.PREFERRED_SIZE,
																GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
														.addPreferredGap(ComponentPlacement.RELATED)
														.addComponent(lblPercentage, GroupLayout.DEFAULT_SIZE,
																GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE)
														.addGap(0, 20, Short.MAX_VALUE))
												.addGroup(groupLayout.createSequentialGroup()
														.addComponent(txtPath, GroupLayout.DEFAULT_SIZE, 170,
																Short.MAX_VALUE)
														.addPreferredGap(ComponentPlacement.RELATED)
														.addComponent(btnChoose))))
								.addComponent(btnCreateSample, Alignment.TRAILING))
						.addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
						.addContainerGap()
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(btnChoose)
								.addComponent(lblinput)
								.addComponent(txtPath))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblpercentage)
								.addComponent(lblPercentage)
								.addComponent(pctSpinner))
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(btnCreateSample)
						.addContainerGap()));
		getContentPane().setLayout(groupLayout);

		setupComponents();
	}

	private void setupComponents() {
		this.btnChoose.addActionListener(e -> {
			JFileChooser chooser = new JFileChooser();
			int result = chooser.showOpenDialog(null);
			if (result == JFileChooser.APPROVE_OPTION) {
				File f = chooser.getSelectedFile();
				String filename = f.getAbsolutePath();
				PopulationSampler.this.txtPath.setText(filename);
			}
		});

		this.btnCreateSample.addActionListener(e -> {
			createSample();
			PopulationSampler.this.setVisible(false);
		});
	}

	private void createSample() {
		final String srcFilename = PopulationSampler.this.txtPath.getText();
		File srcFile = new File(srcFilename);
		if (!srcFile.exists()) {
			JOptionPane.showMessageDialog(null, "The specified file could not be found: " + srcFilename,
					"File not found!", JOptionPane.ERROR_MESSAGE);
			return;
		}

		final String namePart = srcFilename.substring(0, srcFilename.toLowerCase(Locale.ROOT).lastIndexOf(".xml"));
		final int percentage = (Integer)PopulationSampler.this.pctSpinner.getValue();
		final double samplesize = percentage / 100.0;

		JFileChooser chooser = new SaveFileSaver();
		chooser.setCurrentDirectory(srcFile.getParentFile());
		chooser.setSelectedFile(new File(srcFile.getParentFile(), namePart + "." + percentage + "pct.xml.gz"));
		int saveResult = chooser.showSaveDialog(PopulationSampler.this);
		if (saveResult == JFileChooser.APPROVE_OPTION) {
			File destFile = chooser.getSelectedFile();
			doCreateSample(srcFile, null, samplesize, destFile);
		}
	}

	private void doCreateSample(File inputPopulationFile, File networkFile, double samplesize,
			File outputPopulationFile) {
		AsyncFileInputProgressDialog gui = new AsyncFileInputProgressDialog();

		new Thread(() -> {
			MutableScenario sc = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());

			if (networkFile != null) {
				SwingUtilities.invokeLater(() -> gui.setTitle("Loading Network…"));
				try (FileInputStream fis = new FileInputStream(networkFile);
						BufferedInputStream is = getBufferedInputStream(networkFile.getName(), fis)) {
					new MatsimNetworkReader(sc.getNetwork()).parse(is);
				} catch (IOException | RuntimeException e) {
					log.error(e.getMessage(), e);
					SwingUtilities.invokeLater(gui::dispose);
					SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
							"Error while reading the network file: " + e.getMessage(),
							"Cannot create population sample", JOptionPane.ERROR_MESSAGE));
					return;
				}
			}

			SwingUtilities.invokeLater(() -> gui.setTitle("Creating Population Sample…"));
			StreamingPopulationReader reader = new StreamingPopulationReader(sc);
			StreamingPopulationWriter writer = null;

			try (FileInputStream fis = new FileInputStream(inputPopulationFile);
					BufferedInputStream is = getBufferedInputStream(inputPopulationFile.getName(), fis)) {
				writer = new StreamingPopulationWriter(new IdentityTransformation(), samplesize);
				writer.startStreaming(outputPopulationFile.getAbsolutePath());
				reader.addAlgorithm(writer);
				reader.parse(is);
				SwingUtilities.invokeLater(gui::dispose);
				writer.closeStreaming();
			} catch (RuntimeException | IOException e) {
				log.error(e.getMessage(), e);
				SwingUtilities.invokeLater(gui::dispose);

				if (writer != null) {
					writer.closeStreaming();
					outputPopulationFile.delete();
				}

				if (e instanceof RuntimeException && networkFile == null) {
					//just making a guess that providing the corresponding network will solve the issue
					SwingUtilities.invokeLater(() -> {
						JOptionPane.showMessageDialog(null,
								"<html>It looks like the population file cannot be parsed without a network file.<br />Please select a matching network file in the next dialog.</html>",
								"Problems creating population sample", JOptionPane.WARNING_MESSAGE);
						JFileChooser netChooser = new JFileChooser();
						netChooser.setCurrentDirectory(inputPopulationFile.getParentFile());
						int result = netChooser.showOpenDialog(this);
						if (result == JFileChooser.APPROVE_OPTION) {
							doCreateSample(inputPopulationFile, netChooser.getSelectedFile(), samplesize,
									outputPopulationFile);
						}
					});
				} else {
					SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
							"The population sample cannot be created, as not all necessary data is available.",
							"Cannot create population sample", JOptionPane.ERROR_MESSAGE));
				}
			}
		}, "sampler").start();
	}

	private static BufferedInputStream getBufferedInputStream(String filename, FileInputStream fis) throws IOException {
		String lcFilename = filename.toLowerCase(Locale.ROOT);
		if (lcFilename.endsWith(".gz")) {
			return new BufferedInputStream(new UnicodeInputStream(new GZIPInputStream(fis)));
		}
		if (lcFilename.endsWith(".zst")) {
			return new BufferedInputStream(new UnicodeInputStream(new ZstdInputStream(fis)));
		}
		return new BufferedInputStream(new UnicodeInputStream(fis));
	}
}
