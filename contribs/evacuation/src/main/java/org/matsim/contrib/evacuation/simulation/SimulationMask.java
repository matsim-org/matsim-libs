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

package org.matsim.contrib.evacuation.simulation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.model.locale.Locale;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

public class SimulationMask extends JPanel {
	private static final long serialVersionUID = 1L;
	private Controller controller;
	private JButton btRun;
	private JTextArea textOutput;

	private JTextField textFirstIteration;
	private JTextField textLastIteration;
	private Locale locale;
	private JLabel labelConfigName;
	protected String configFile;

	public SimulationMask(Controller controller) {

		this.labelConfigName = new JLabel("");

		this.controller = controller;
		this.locale = this.controller.getLocale();
		this.setLayout(new BorderLayout());
		//
		this.textOutput = new JTextArea(20, 20);
		this.textOutput.setEnabled(false);
		this.textOutput.setDisabledTextColor(Color.BLACK);
		JLabel labelFirstIteration = new JLabel(" first iteration: ");
		JLabel labelLastIteration = new JLabel(" last iteration: ");
		JPanel itPanel = new JPanel(new GridLayout(5, 2));
		itPanel.setPreferredSize(new Dimension(550, 150));
		itPanel.setSize(new Dimension(550, 100));
		itPanel.setMaximumSize(new Dimension(550, 150));

		JPanel centerPanel = new JPanel();
		centerPanel.setBorder(new LineBorder(Color.darkGray, 4));

		centerPanel.add(itPanel);

		this.textFirstIteration = new JTextField();
		this.textLastIteration = new JTextField();
		this.textFirstIteration.setEnabled(false);
		this.textLastIteration.setEnabled(false);

		itPanel.add(new JLabel(" destination:"));
		itPanel.add(labelConfigName);

		itPanel.add(labelFirstIteration);
		itPanel.add(textFirstIteration);
		itPanel.add(labelLastIteration);
		itPanel.add(textLastIteration);
		itPanel.add(new JLabel(""));

		this.btRun = new JButton(locale.btRun());
		this.btRun.setEnabled(false);

		JPanel infoPanel = new JPanel();
		infoPanel.setSize(600, 200);

		// infoPanel.add(new
		// JLabel(this.controller.getLocale().moduleMatsimScenarioGenerator()));
		itPanel.add(btRun);
		itPanel.add(infoPanel);

		this.add(new JScrollPane(textOutput), BorderLayout.NORTH);
		this.add(centerPanel, BorderLayout.CENTER);
		Logger root = Logger.getRootLogger();
		root.addAppender(new LogAppender(this));

		this.btRun.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					// System.setOut(Scenario2MatsimMask.this.outputRedirect);
					SimulationMask.this.btRun.setEnabled(false);
					// check if outputdir exists
					Scenario scenario = SimulationMask.this.controller
							.getScenario();
					Config config = scenario.getConfig();
					String outdir = config.getParam("evacuation", "outputDir")
							+ "/output";
					;
					outdir = config.getParam("controler", "outputDirectory");
					File file = new File(outdir);
					int a = 0;
					if (file.exists()) {
						a = JOptionPane.showConfirmDialog(SimulationMask.this,
								locale.infoMatsimOverwriteOutputDir(),
								locale.infoMatsimTime(),
								JOptionPane.WARNING_MESSAGE);
						String newdir = outdir + "_old";
						int i = 1;
						while (new File(newdir + i).exists())
							i++;
						newdir += i;
						file.renameTo(new File(newdir));
					} else {
						a = JOptionPane.showConfirmDialog(SimulationMask.this,
								locale.infoMatsimTime(), "",
								JOptionPane.WARNING_MESSAGE);
					}
					SimulationMask.this.setCursor(Cursor
							.getPredefinedCursor(Cursor.WAIT_CURSOR));

					if (a == JOptionPane.OK_OPTION) {

						SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

							@Override
							protected String doInBackground() {
								Config config = SimulationMask.this.controller
										.getScenario().getConfig();

								config.setParam("controler", "firstIteration",
										textFirstIteration.getText());
								config.setParam("controler", "lastIteration",
										textLastIteration.getText());
								new ConfigWriter(config)
										.write(SimulationMask.this.configFile);

								Controler matsimController = new Controler(
										config);
								matsimController.getConfig().controler().setOverwriteFileSetting(
										true ?
												OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
												OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
								matsimController.run();

								SimulationMask.this.controller
										.setGoalAchieved(true);

								return "";
							}

							@Override
							protected void done() {
								SimulationMask.this.setCursor(Cursor
										.getDefaultCursor());
								SimulationMask.this.btRun.setEnabled(true);

							}
						};
						worker.execute();
					}

				} catch (Exception e2) {
					e2.printStackTrace();
				} finally {
					SimulationMask.this.btRun.setEnabled(true);
					SimulationMask.this.setCursor(Cursor.getDefaultCursor());
				}

			}
		});

		this.textFirstIteration.addKeyListener(new NumberKeyListener());
		this.textLastIteration.addKeyListener(new NumberKeyListener());

		this.setVisible(true);

	}

	public void readConfig() {
		Config config = this.controller.getScenario().getConfig();
		String matsimSP = this.controller.getScenarioPath();
		this.labelConfigName.setText(matsimSP);
		this.configFile = this.controller.getMatsimConfigFile();
		this.textFirstIteration.setText(config.getModule("controler").getValue(
				"firstIteration"));
		this.textLastIteration.setText(config.getModule("controler").getValue(
				"lastIteration"));
		this.textFirstIteration.setEnabled(true);
		this.textLastIteration.setEnabled(true);
		this.btRun.setEnabled(true);
	}

	private class NumberKeyListener implements KeyListener {

		@Override
		public void keyTyped(KeyEvent e) {
			if (!Character.toString(e.getKeyChar()).matches("[0-9]"))
				e.consume();
		}

		@Override
		public void keyReleased(KeyEvent e) {
			if (e.getSource() instanceof JTextField) {
				String val = ((JTextField) e.getSource()).getText();
				if ((val != "") && (!isNumeric(val)))
					((JTextField) e.getSource()).setText("0");

			}

		}

		@Override
		public void keyPressed(KeyEvent e) {
		}

		public boolean isNumeric(String str) {
			return str.matches("-?\\d+(\\.\\d+)?");
			// this has been changed by Refactor|Rename from d to destination?
			// reverted it (HK, 2014-02-19)
		}

	}

	public class LogAppender extends AppenderSkeleton {
		private SimulationMask msgMask;
		private long n = 0;

		public LogAppender(SimulationMask msgMask) {
			super();
			this.msgMask = msgMask;
		}

		@Override
		protected void append(LoggingEvent loggingEvent) {

			this.msgMask.textOutput.append(loggingEvent.getMessage() + "\r\n");
			this.msgMask.textOutput.selectAll();

			if (++n > 20) {
				Element root = this.msgMask.textOutput.getDocument()
						.getDefaultRootElement();
				Element first = root.getElement(0);
				try {
					this.msgMask.textOutput.getDocument().remove(
							first.getStartOffset(), first.getEndOffset());
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void close() {

		}

		@Override
		public boolean requiresLayout() {
			return false;
		}

	}

}
