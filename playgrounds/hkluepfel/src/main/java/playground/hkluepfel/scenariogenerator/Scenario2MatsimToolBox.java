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

package playground.hkluepfel.scenariogenerator;

import java.awt.BorderLayout;
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
import org.matsim.contrib.grips.control.Controller;
import org.matsim.contrib.grips.model.Constants;

public class Scenario2MatsimToolBox extends JPanel
{
	private static final long serialVersionUID = 1L;
	private Controller controller;
	private JButton btRun;
	private JTextArea textOutput;
	private Scenario2Matsim scenario2matsim;
	private LogAppender logAppender;
	private Logger root;

	@SuppressWarnings("resource")
	public Scenario2MatsimToolBox(Scenario2Matsim s2m, Controller controller)
	{
		new Interceptor(this, System.out);
		this.scenario2matsim = s2m;

		this.controller = controller;
		int width = this.controller.getParentComponent().getWidth(); 
		int height = this.controller.getParentComponent().getHeight(); 
		
		this.setLayout(new BorderLayout());
		this.textOutput = new JTextArea();
		this.textOutput.setPreferredSize(new Dimension(width - 20,(int)(height/1.5)));
		this.btRun = new JButton(this.controller.getLocale().btRun());
		this.btRun.setEnabled(false);

		JPanel buttonPanel = new JPanel();
		JPanel infoPanel = new JPanel();

		infoPanel.add(new JScrollPane(this.textOutput));
		this.textOutput.setEnabled(false);
		buttonPanel.add(btRun);
		this.add(infoPanel, BorderLayout.NORTH);
		this.add(buttonPanel, BorderLayout.CENTER);

		root = Logger.getRootLogger();
		logAppender = new LogAppender(this);
		root.addAppender(logAppender);
		
		this.btRun.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					Scenario2MatsimToolBox.this.btRun.setEnabled(false);
					Scenario2MatsimToolBox.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

					SwingWorker<String, Void> worker = new SwingWorker<String, Void>()
					{

						@Override
						protected String doInBackground()
						{
							String gripsFile = Scenario2MatsimToolBox.this.controller.getGripsFile();
							Scenario2MatsimConverter s2m = new Scenario2MatsimConverter(gripsFile);
							s2m.run();
							return "";
						}

						@Override
						protected void done()
						{
							Scenario2MatsimToolBox.this.setCursor(Cursor.getDefaultCursor());
							Scenario2MatsimToolBox.this.btRun.setEnabled(true);
							Scenario2MatsimToolBox.this.scenario2matsim.setMainGoalAchieved(true);
							Scenario2MatsimToolBox.this.controller.setGoalAchieved(Scenario2MatsimToolBox.this.scenario2matsim.isMainGoalAchieved());
							
							Scenario2MatsimToolBox.this.root.removeAppender(Scenario2MatsimToolBox.this.logAppender);

							if (!Scenario2MatsimToolBox.this.controller.isStandAlone())
								Scenario2MatsimToolBox.this.controller.openMastimConfig(new File(Scenario2MatsimToolBox.this.controller.getGripsConfigModule().getOutputDir() + Constants.DEFAULT_MATSIM_CONFIG_FILE));
						}
					};
					
					worker.execute();

					
				} catch (Exception e2)
				{
					e2.printStackTrace();
				} finally
				{
				}

			}
		});

	}

	private class Interceptor extends PrintStream
	{
		Scenario2MatsimToolBox s2mToolBox;

		public Interceptor(Scenario2MatsimToolBox s2mToolBox, OutputStream out)
		{
			super(out, true);
			this.s2mToolBox = s2mToolBox;
		}

		@Override
		public void print(String s)
		{
			s2mToolBox.textOutput.append(s + "\r\n");
		}

		@Override
		public void println(String x)
		{
			s2mToolBox.textOutput.append(x + "\r\n");
		}

	}
	
	public class LogAppender extends AppenderSkeleton
	{
		private Scenario2MatsimToolBox s2mToolBox;
		private long n = 0;

		public LogAppender(Scenario2MatsimToolBox s2mToolBox)
		{
			super();
			this.s2mToolBox = s2mToolBox;
		}

		@Override
		protected void append(LoggingEvent loggingEvent)
		{
			
			this.s2mToolBox.textOutput.append(loggingEvent.getMessage() + "\r\n");
			this.s2mToolBox.textOutput.selectAll();
			
			if (++n>20)
			{
				Element root = this.s2mToolBox.textOutput.getDocument().getDefaultRootElement();
				Element first = root.getElement(0);
				try
				{
					this.s2mToolBox.textOutput.getDocument().remove(first.getStartOffset(), first.getEndOffset());
				} catch (BadLocationException e)
				{
					e.printStackTrace();
				}
			}
		}

		@Override
		public void close()
		{

		}

		@Override
		public boolean requiresLayout()
		{
			return false;
		}

	}

	public void enableRunButton(boolean toggle)
	{
		this.btRun.setEnabled(toggle);
		
	}

}
