package playground.wdoering.grips.v2.scenariogenerator;

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
import org.matsim.contrib.grips.scenariogenerator.ScenarioGenerator;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.model.Constants;

public class SGMask extends JPanel
{
	private Controller controller;
	private JButton btRun;
	private JTextArea textOutput;
	private Interceptor outputRedirect;
	private PrintStream defaultOut;
	private playground.wdoering.grips.v2.scenariogenerator.ScenarioGenerator scenarioGeneratorMask;
	private LogAppender logAppender;
	private Logger root;

	public SGMask(playground.wdoering.grips.v2.scenariogenerator.ScenarioGenerator scenariogen, Controller controller)
	{
		this.defaultOut = System.out;
		this.outputRedirect = new Interceptor(this, System.out);
		this.scenarioGeneratorMask = scenariogen;

		this.controller = controller;
		int width = this.controller.getParentComponent().getWidth(); 
		int height = this.controller.getParentComponent().getHeight(); 
		
		this.setLayout(new BorderLayout());
		this.textOutput = new JTextArea();
		this.textOutput.setPreferredSize(new Dimension(width,(int)(height/1.5)));
		this.textOutput.setMinimumSize(new Dimension(height,(int)(height/1.5)));
		this.btRun = new JButton(this.controller.getLocale().btRun());
		this.btRun.setEnabled(false);

		JPanel buttonPanel = new JPanel();
		JPanel infoPanel = new JPanel();

//		infoPanel.add(new JLabel(this.controller.getLocale().moduleScenarioGenerator()));
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
//					System.setOut(SGMask.this.outputRedirect);
					SGMask.this.btRun.setEnabled(false);
					SGMask.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

					SwingWorker<String, Void> worker = new SwingWorker<String, Void>()
					{

						@Override
						protected String doInBackground()
						{
							ScenarioGenerator scengen = new org.matsim.contrib.grips.scenariogenerator.ScenarioGenerator(SGMask.this.controller.getGripsFile());
							scengen.run();
							return "";
						}

						@Override
						protected void done()
						{
							SGMask.this.setCursor(Cursor.getDefaultCursor());
							SGMask.this.btRun.setEnabled(true);
							SGMask.this.scenarioGeneratorMask.setMainGoalAchieved(true);
							SGMask.this.controller.setGoalAchieved(SGMask.this.scenarioGeneratorMask.isMainGoalAchieved());
							
							String path = SGMask.this.controller.getScenarioPath();
							
							SGMask.this.root.removeAppender(SGMask.this.logAppender);
							

							if (!SGMask.this.controller.isStandAlone())
								SGMask.this.controller.openMastimConfig(new File(SGMask.this.controller.getGripsConfigModule().getOutputDir() + Constants.DEFAULT_MATSIM_CONFIG_FILE));
							

						}
					};
					// Execute the SwingWorker; the GUI will not freeze
					worker.execute();

					
				} catch (Exception e2)
				{
					e2.printStackTrace();
				} finally
				{
//					SGMask.this.setBackground(Color.gray);
//					SGMask.this.setCursor(Cursor.getDefaultCursor());
//					System.setOut(SGMask.this.defaultOut);
				}

			}
		});

	}

	private class Interceptor extends PrintStream
	{
		SGMask mask;

		public Interceptor(SGMask mask, OutputStream out)
		{
			super(out, true);
			this.mask = mask;
		}

		@Override
		public void print(String s)
		{
			mask.textOutput.append(s + "\r\n");
		}

		@Override
		public void println(String x)
		{
			mask.textOutput.append(x + "\r\n");
		}

	}
	
	public class LogAppender extends AppenderSkeleton
	{
		private SGMask sgMask;
		private long n = 0;

		public LogAppender(SGMask sgMask)
		{
			super();
			this.sgMask = sgMask;
		}

		@Override
		protected void append(LoggingEvent loggingEvent)
		{
			
			this.sgMask.textOutput.append(loggingEvent.getMessage() + "\r\n");
			this.sgMask.textOutput.selectAll();
			
			if (++n>20)
			{
				Element root = this.sgMask.textOutput.getDocument().getDefaultRootElement();
				Element first = root.getElement(0);
				try
				{
					this.sgMask.textOutput.getDocument().remove(first.getStartOffset(), first.getEndOffset());
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
