package playground.wdoering.grips.v2.scenariogenerator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.AppenderAttachableImpl;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.grips.scenariogenerator.ScenarioGenerator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.model.locale.Locale;

public class MSGMask extends JPanel
{
	private Controller controller;
	private JButton btRun;
	private JTextArea textOutput;

	private JTextField textNumIt;

	private Interceptor outputRedirect;
	private PrintStream defaultOut;
	private JTextField textFirstIteration;
	private JTextField textLastIteration;
	private Locale locale;
	private JLabel labelConfigName;
	protected String configFile;

	public MSGMask(Controller controller)
	{

		this.labelConfigName = new JLabel("");
		// this.defaultOut = System.out;
		// this.outputRedirect = new Interceptor(this, System.out);
		//
		this.controller = controller;
		this.locale = this.controller.getLocale();
		this.setLayout(new BorderLayout());
		//
		this.textOutput = new JTextArea(20, 20);
		this.textOutput.setEnabled(false);
		JLabel labelFirstIteration = new JLabel(" first iteration: ");
		JLabel labelLastIteration = new JLabel(" last iteration: ");
		JPanel itPanel = new JPanel(new GridLayout(5, 2));
		itPanel.setPreferredSize(new Dimension(350, 150));
		itPanel.setSize(new Dimension(350, 100));
		itPanel.setMaximumSize(new Dimension(350, 150));

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

		JPanel buttonPanel = new JPanel();
		JPanel infoPanel = new JPanel();
		infoPanel.setSize(600, 200);

		infoPanel.add(new JLabel(this.controller.getLocale().moduleMatsimScenarioGenerator()));
		itPanel.add(btRun);
		itPanel.add(infoPanel);
		//
//		JScrollPane scrollPane = new JScrollPane(textOutput);
		
		this.add(new JScrollPane(textOutput), BorderLayout.NORTH);
//		this.add(infoPanel, BorderLayout.NORTH);
		this.add(centerPanel, BorderLayout.CENTER);
		// this.setBackground(Color.pink);

		// this.setMinimumSize(new Dimension(300,300));
		// this.setPreferredSize(new Dimension(500,500));

//		System.out.println(root.getName());
		Logger root = Logger.getRootLogger();
		root.addAppender(new LogAppender(this));

		this.btRun.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					// System.setOut(SGMask.this.outputRedirect);
					MSGMask.this.btRun.setEnabled(false);

					
					int a = JOptionPane.showConfirmDialog(MSGMask.this, locale.infoMatsimTime(), "", JOptionPane.WARNING_MESSAGE);

					MSGMask.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					
					if (a == JOptionPane.OK_OPTION)
					{
//						Logger logger = Logger.getLogger(Controler.class);
//						logger.addAppender(new LogAppender(MSGMask.this));
	
						SwingWorker<String, Void> worker = new SwingWorker<String, Void>()
						{
	
							@Override
							protected String doInBackground()
							{
								Config config = MSGMask.this.controller.getScenario().getConfig();
	
								config.setParam("controler", "firstIteration", textFirstIteration.getText());
								config.setParam("controler", "lastIteration", textLastIteration.getText());
								new ConfigWriter(config).write(MSGMask.this.configFile);
	
								// ScenarioGenerator scengen = new
								// org.matsim.contrib.grips.scenariogenerator.ScenarioGenerator(MSGMask.this.controller.getGripsFile());
	
								//Controler(final String configFileName, final Config config, final Scenario scenario) {
								
//								Controler matsimController = new Controler(MSGMask.this.controller.getMatsimConfigFile());
								Controler matsimController = new Controler(config);
								matsimController.run();
	
								MSGMask.this.controller.setGoalAchieved(true);
								
								return "";
							}
	
							@Override
							protected void done()
							{
								MSGMask.this.setCursor(Cursor.getDefaultCursor());
								MSGMask.this.btRun.setEnabled(true);
	
							}
						};
						worker.execute();
					}

				} catch (Exception e2)
				{
					e2.printStackTrace();
				} finally
				{
					// SGMask.this.setBackground(Color.gray);
					// SGMask.this.setCursor(Cursor.getDefaultCursor());
					// System.setOut(SGMask.this.defaultOut);
					MSGMask.this.btRun.setEnabled(true);
					MSGMask.this.setCursor(Cursor.getDefaultCursor());
				}

			}
		});

		this.textFirstIteration.addKeyListener(new NumberKeyListener());
		this.textLastIteration.addKeyListener(new NumberKeyListener());

		this.setVisible(true);

	}

	private class Interceptor extends PrintStream
	{
		MSGMask mask;

		public Interceptor(MSGMask mask, OutputStream out)
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

	public void readConfig()
	{
		Config config = this.controller.getScenario().getConfig();
		this.labelConfigName.setText(this.controller.getScenarioPath());
		// System.out.println(this.controller.getMatsimConfigFile());
		// System.out.println(this.controller.getScenarioPath());
		this.configFile = this.controller.getMatsimConfigFile();
		this.textFirstIteration.setText(config.getModule("controler").getValue("firstIteration"));
		this.textLastIteration.setText(config.getModule("controler").getValue("lastIteration"));
		this.textFirstIteration.setEnabled(true);
		this.textLastIteration.setEnabled(true);
		this.btRun.setEnabled(true);
	}

	private class NumberKeyListener implements KeyListener
	{

		@Override
		public void keyTyped(KeyEvent e)
		{
			if (!Character.toString(e.getKeyChar()).matches("[0-9]"))
				e.consume();
		}

		@Override
		public void keyReleased(KeyEvent e)
		{
			if (e.getSource() instanceof JTextField)
			{
				String val = ((JTextField) e.getSource()).getText();
				if ((val != "") && (!isNumeric(val)))
					((JTextField) e.getSource()).setText("0");

			}

		}

		@Override
		public void keyPressed(KeyEvent e)
		{
		}

		public boolean isNumeric(String str)
		{
			return str.matches("-?\\d+(\\.\\d+)?"); // match a number with
													// optional '-' and decimal.
		}

	}

	public class LogAppender extends AppenderSkeleton
	{
		private MSGMask msgMask;
		private long n = 0;

		public LogAppender(MSGMask msgMask)
		{
			super();
			this.msgMask = msgMask;
		}

		@Override
		protected void append(LoggingEvent loggingEvent)
		{
			
			this.msgMask.textOutput.append(loggingEvent.getMessage() + "\r\n");
			this.msgMask.textOutput.selectAll();
			
			if (++n>20)
			{
				Element root = this.msgMask.textOutput.getDocument().getDefaultRootElement();
				Element first = root.getElement(0);
				try
				{
					this.msgMask.textOutput.getDocument().remove(first.getStartOffset(), first.getEndOffset());
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

}
