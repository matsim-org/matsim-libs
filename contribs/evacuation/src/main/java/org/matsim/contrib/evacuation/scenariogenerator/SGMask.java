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

package org.matsim.contrib.evacuation.scenariogenerator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.model.Constants;

public class SGMask extends JPanel {
	private static final long serialVersionUID = 1L;
	private Controller controller;
	private JButton btRun;
	private JTextArea textOutput;
	private MatsimNetworkGenerator scenarioGeneratorMask;
	private LogAppender logAppender;
	private Logger root;

	@SuppressWarnings("resource")
	public SGMask(MatsimNetworkGenerator scenariogen, Controller controller) {
		new Interceptor(this, System.out);
		this.scenarioGeneratorMask = scenariogen;

		this.controller = controller;
		int width = this.controller.getParentComponent().getWidth();
		int height = this.controller.getParentComponent().getHeight();

		this.setLayout(new BorderLayout());
		this.textOutput = new JTextArea();
		this.textOutput.setPreferredSize(new Dimension(width - 20, (int) (height / 1.5)));
		this.btRun = new JButton(this.controller.getLocale().btRun());
		this.btRun.setEnabled(false);

		JPanel buttonPanel = new JPanel();
		JPanel infoPanel = new JPanel();

		infoPanel.add(new JScrollPane(this.textOutput));
		this.textOutput.setEnabled(false);
		this.textOutput.setDisabledTextColor(Color.BLACK);

		buttonPanel.add(btRun);
		this.add(infoPanel, BorderLayout.NORTH);
		this.add(buttonPanel, BorderLayout.CENTER);

		root = Logger.getRootLogger();
		logAppender = new LogAppender(this);
		root.addAppender(logAppender);

		this.btRun.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					SGMask.this.btRun.setEnabled(false);
					SGMask.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

					SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

						@Override
						protected String doInBackground() {
							ScenarioGenerator scengen = new org.matsim.contrib.evacuation.scenariogenerator.ScenarioGenerator(SGMask.this.controller.getEvacuationFile());
							scengen.run();
							return "";
						}

						@Override
						protected void done() {
							SGMask.this.setCursor(Cursor.getDefaultCursor());
							SGMask.this.btRun.setEnabled(true);
							SGMask.this.scenarioGeneratorMask.setMainGoalAchieved(true);
							SGMask.this.controller.setGoalAchieved(SGMask.this.scenarioGeneratorMask.isMainGoalAchieved());

							SGMask.this.root.removeAppender(SGMask.this.logAppender);

							if (!SGMask.this.controller.isStandAlone())
								SGMask.this.controller.openMastimConfig(new File(SGMask.this.controller.getEvacuationConfigModule().getOutputDir() + Constants.DEFAULT_MATSIM_CONFIG_FILE));

						}
					};

					worker.execute();

				} catch (Exception e2) {
					e2.printStackTrace();
				} finally {
				}

			}
		});

	}

	private class Interceptor extends PrintStream {
		SGMask mask;

		public Interceptor(SGMask mask, OutputStream out) {
			super(out, true);
			this.mask = mask;
		}

		@Override
		public void print(String s) {
			mask.textOutput.append(s + "\r\n");
		}

		@Override
		public void println(String x) {
			mask.textOutput.append(x + "\r\n");
		}

	}

	public class LogAppender extends AppenderSkeleton {
		private SGMask sgMask;
		private long n = 0;

		public LogAppender(SGMask sgMask) {
			super();
			this.sgMask = sgMask;
		}

		@Override
		protected void append(LoggingEvent loggingEvent) {

			this.sgMask.textOutput.append(loggingEvent.getMessage() + "\r\n");
			this.sgMask.textOutput.selectAll();

			if (++n > 20) {
				Element root = this.sgMask.textOutput.getDocument().getDefaultRootElement();
				Element first = root.getElement(0);
				try {
					this.sgMask.textOutput.getDocument().remove(first.getStartOffset(), first.getEndOffset());
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

	public void enableRunButton(boolean toggle) {
		this.btRun.setEnabled(toggle);

	}

}
