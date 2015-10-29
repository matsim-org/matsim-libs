package playground.singapore.travelsummary;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JButton;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JCheckBox;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Properties;

public class EventsToTravelSummaryTablesGUI extends JFrame {

	private JPanel contentPane;
	private JTextField outputPathComponent;
	private JTextField transitScheduleFileComponent;
	private JTextField tableSuffixComponent;
	private JTextField configFileComponent;
	private JTextField eventsToSQLPropertiesFileComponent;
	private JTextField networkFileComponent;
	private JTextField eventsFileComponent;
	private Properties defaultProperties;
	private String transitScheduleFile = "transitschedule.xml";
	private String networkFile = "multimodalnetwork.xml";
	private String eventsFile = "events.xml.gz";
	private String configFile = "config.xml";
	private String tableSuffix = "_test";
	private String outputPath = "./";
	private EventsToTravelSummaryTablesGUI self = this;
	private String defaultpath = "";

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					EventsToTravelSummaryTablesGUI frame = new EventsToTravelSummaryTablesGUI();
					frame.setVisible(true);
					frame.loadDefaultProperties(new File(
							"eventsToSQL.properties"));
				} catch (Exception e) {
					System.err.println("No default properties file.");
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public EventsToTravelSummaryTablesGUI() {
		setTitle("Events to PostgreSQL tables");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 764, 246);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] { 144, 509, 0 };
		gbl_contentPane.rowHeights = new int[] { 20, 20, 20, 20, 20, 20, 20,
				23, 0 };
		gbl_contentPane.columnWeights = new double[] { 0.0, 1.0,
				Double.MIN_VALUE };
		gbl_contentPane.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0,
				0.0, 0.0, 0.0, Double.MIN_VALUE };
		contentPane.setLayout(gbl_contentPane);

		JLabel lblFillFromProperties = new JLabel(
				"Fill this dialog from properties file:");
		GridBagConstraints gbc_lblFillFromProperties = new GridBagConstraints();
		gbc_lblFillFromProperties.anchor = GridBagConstraints.WEST;
		gbc_lblFillFromProperties.insets = new Insets(0, 0, 5, 5);
		gbc_lblFillFromProperties.gridx = 0;
		gbc_lblFillFromProperties.gridy = 0;
		contentPane.add(lblFillFromProperties, gbc_lblFillFromProperties);

		eventsToSQLPropertiesFileComponent = new JTextField();
		eventsToSQLPropertiesFileComponent.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				File defaultPropertiesFile = fileSelect(
						eventsToSQLPropertiesFileComponent.getText(),
						"Select properties file", false);
				try {
					eventsToSQLPropertiesFileComponent
							.setText(defaultPropertiesFile.getPath());
					loadDefaultProperties(defaultPropertiesFile);
				} catch (NullPointerException ne) {
					// do nothing
				}
			}
		});
		eventsToSQLPropertiesFileComponent.setText("eventsToSQL.properties");
		GridBagConstraints gbc_txtDataeventstosqlproperties = new GridBagConstraints();
		gbc_txtDataeventstosqlproperties.anchor = GridBagConstraints.NORTH;
		gbc_txtDataeventstosqlproperties.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtDataeventstosqlproperties.insets = new Insets(0, 0, 5, 0);
		gbc_txtDataeventstosqlproperties.gridx = 1;
		gbc_txtDataeventstosqlproperties.gridy = 0;
		contentPane.add(eventsToSQLPropertiesFileComponent,
				gbc_txtDataeventstosqlproperties);
		eventsToSQLPropertiesFileComponent.setColumns(10);

		JLabel lbloutputPath = new JLabel("Output path for CSVs");
		lbloutputPath.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_lbloutputPath = new GridBagConstraints();
		gbc_lbloutputPath.anchor = GridBagConstraints.WEST;
		gbc_lbloutputPath.insets = new Insets(0, 0, 5, 5);
		gbc_lbloutputPath.gridx = 0;
		gbc_lbloutputPath.gridy = 1;
		contentPane.add(lbloutputPath, gbc_lbloutputPath);

		outputPathComponent = new JTextField();
		outputPathComponent.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				outputPathComponent.setText(fileSelect(
						outputPathComponent.getText(), "Select output path",
						true).getPath());
			}
		});
		outputPathComponent.setText("./");
		GridBagConstraints gbc_outputPath = new GridBagConstraints();
		gbc_outputPath.anchor = GridBagConstraints.NORTH;
		gbc_outputPath.fill = GridBagConstraints.HORIZONTAL;
		gbc_outputPath.insets = new Insets(0, 0, 5, 0);
		gbc_outputPath.gridx = 1;
		gbc_outputPath.gridy = 1;
		contentPane.add(outputPathComponent, gbc_outputPath);
		outputPathComponent.setColumns(10);

		JLabel lblTransitSchedule = new JLabel(
				"Transit schedule (empty for none)");
		lblTransitSchedule.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_lblTransitSchedule = new GridBagConstraints();
		gbc_lblTransitSchedule.anchor = GridBagConstraints.WEST;
		gbc_lblTransitSchedule.insets = new Insets(0, 0, 5, 5);
		gbc_lblTransitSchedule.gridx = 0;
		gbc_lblTransitSchedule.gridy = 2;
		contentPane.add(lblTransitSchedule, gbc_lblTransitSchedule);

		transitScheduleFileComponent = new JTextField();
		transitScheduleFileComponent.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				transitScheduleFileComponent.setText(fileSelect(
						transitScheduleFileComponent.getText(),
						"select transit schedule file", false).getPath());
			}
		});
		transitScheduleFileComponent.setText("transitSchedule.xml.gz");
		GridBagConstraints gbc_transitScheduleFile = new GridBagConstraints();
		gbc_transitScheduleFile.anchor = GridBagConstraints.NORTH;
		gbc_transitScheduleFile.fill = GridBagConstraints.HORIZONTAL;
		gbc_transitScheduleFile.insets = new Insets(0, 0, 5, 0);
		gbc_transitScheduleFile.gridx = 1;
		gbc_transitScheduleFile.gridy = 2;
		contentPane.add(transitScheduleFileComponent, gbc_transitScheduleFile);
		transitScheduleFileComponent.setColumns(10);

		JLabel lblNetwork = new JLabel("Network");
		GridBagConstraints gbc_lblNetwork = new GridBagConstraints();
		gbc_lblNetwork.anchor = GridBagConstraints.WEST;
		gbc_lblNetwork.insets = new Insets(0, 0, 5, 5);
		gbc_lblNetwork.gridx = 0;
		gbc_lblNetwork.gridy = 3;
		contentPane.add(lblNetwork, gbc_lblNetwork);

		networkFileComponent = new JTextField();
		networkFileComponent.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				networkFileComponent.setText(fileSelect(
						networkFileComponent.getText(), "select network file",
						false).getPath());
			}
		});
		networkFileComponent.setText("network.xml");
		GridBagConstraints gbc_networkFile = new GridBagConstraints();
		gbc_networkFile.anchor = GridBagConstraints.NORTH;
		gbc_networkFile.fill = GridBagConstraints.HORIZONTAL;
		gbc_networkFile.insets = new Insets(0, 0, 5, 0);
		gbc_networkFile.gridx = 1;
		gbc_networkFile.gridy = 3;
		contentPane.add(networkFileComponent, gbc_networkFile);
		networkFileComponent.setColumns(10);

		JLabel lblConfig = new JLabel("Config");
		GridBagConstraints gbc_lblConfig = new GridBagConstraints();
		gbc_lblConfig.anchor = GridBagConstraints.WEST;
		gbc_lblConfig.insets = new Insets(0, 0, 5, 5);
		gbc_lblConfig.gridx = 0;
		gbc_lblConfig.gridy = 4;
		contentPane.add(lblConfig, gbc_lblConfig);

		configFileComponent = new JTextField();
		configFileComponent.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				configFileComponent.setText(fileSelect(
						configFileComponent.getText(), "select config file",
						false).getPath());
			}
		});
		configFileComponent.setText("config.xml");
		GridBagConstraints gbc_configFile = new GridBagConstraints();
		gbc_configFile.anchor = GridBagConstraints.NORTH;
		gbc_configFile.fill = GridBagConstraints.HORIZONTAL;
		gbc_configFile.insets = new Insets(0, 0, 5, 0);
		gbc_configFile.gridx = 1;
		gbc_configFile.gridy = 4;
		contentPane.add(configFileComponent, gbc_configFile);
		configFileComponent.setColumns(10);

		JLabel lblEvents = new JLabel("Events file");
		GridBagConstraints gbc_lblEvents = new GridBagConstraints();
		gbc_lblEvents.anchor = GridBagConstraints.WEST;
		gbc_lblEvents.insets = new Insets(0, 0, 5, 5);
		gbc_lblEvents.gridx = 0;
		gbc_lblEvents.gridy = 5;
		contentPane.add(lblEvents, gbc_lblEvents);

		eventsFileComponent = new JTextField();
		eventsFileComponent.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				eventsFileComponent.setText(fileSelect(
						eventsFileComponent.getText(), "select events file",
						false).getPath());
			}
		});
		eventsFileComponent.setText("events.xml");
		GridBagConstraints gbc_eventsFile = new GridBagConstraints();
		gbc_eventsFile.anchor = GridBagConstraints.NORTH;
		gbc_eventsFile.fill = GridBagConstraints.HORIZONTAL;
		gbc_eventsFile.insets = new Insets(0, 0, 5, 0);
		gbc_eventsFile.gridx = 1;
		gbc_eventsFile.gridy = 5;
		contentPane.add(eventsFileComponent, gbc_eventsFile);
		eventsFileComponent.setColumns(10);

		JLabel lblTableNameSuffix = new JLabel("Table name suffix");
		GridBagConstraints gbc_lblTableNameSuffix = new GridBagConstraints();
		gbc_lblTableNameSuffix.anchor = GridBagConstraints.WEST;
		gbc_lblTableNameSuffix.insets = new Insets(0, 0, 5, 5);
		gbc_lblTableNameSuffix.gridx = 0;
		gbc_lblTableNameSuffix.gridy = 6;
		contentPane.add(lblTableNameSuffix, gbc_lblTableNameSuffix);

		tableSuffixComponent = new JTextField();
		tableSuffixComponent.setText("_test");
		GridBagConstraints gbc_tableSuffix = new GridBagConstraints();
		gbc_tableSuffix.anchor = GridBagConstraints.NORTH;
		gbc_tableSuffix.fill = GridBagConstraints.HORIZONTAL;
		gbc_tableSuffix.insets = new Insets(0, 0, 5, 0);
		gbc_tableSuffix.gridx = 1;
		gbc_tableSuffix.gridy = 6;
		contentPane.add(tableSuffixComponent, gbc_tableSuffix);
		tableSuffixComponent.setColumns(10);

		JButton btnSaveAsDefault = new JButton("Save as default setup");
		btnSaveAsDefault.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				saveDefaultProperties();
			}

		});
		GridBagConstraints gbc_btnSaveAsDefault = new GridBagConstraints();
		gbc_btnSaveAsDefault.fill = GridBagConstraints.BOTH;
		gbc_btnSaveAsDefault.insets = new Insets(0, 0, 0, 5);
		gbc_btnSaveAsDefault.gridx = 0;
		gbc_btnSaveAsDefault.gridy = 7;
		contentPane.add(btnSaveAsDefault, gbc_btnSaveAsDefault);

		JButton btnStartEventsProcessing = new JButton(
				"START EVENTS PROCESSING");
		btnStartEventsProcessing.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				self.setVisible(false);
				try {
					runEventsProcessing();
				} catch (Exception ex) {
					ex.printStackTrace();
					System.exit(ABORT);
				}
				System.exit(0);
			}
		});
		GridBagConstraints gbc_btnStartEventsProcessing = new GridBagConstraints();
		gbc_btnStartEventsProcessing.anchor = GridBagConstraints.NORTH;
		gbc_btnStartEventsProcessing.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnStartEventsProcessing.gridx = 1;
		gbc_btnStartEventsProcessing.gridy = 7;
		contentPane.add(btnStartEventsProcessing, gbc_btnStartEventsProcessing);
	}

	public void saveDefaultProperties() {
		this.defaultProperties = new Properties();

		this.defaultProperties.setProperty("outputPath",
				outputPathComponent.getText());
		this.defaultProperties.setProperty("transitScheduleFile",
				transitScheduleFileComponent.getText());
		this.defaultProperties.setProperty("tableSuffix",
				tableSuffixComponent.getText());
		this.defaultProperties.setProperty("configFile",
				configFileComponent.getText());
		this.defaultProperties.setProperty("networkFile",
				networkFileComponent.getText());
		this.defaultProperties.setProperty("eventsFile",
				eventsFileComponent.getText());
		try {
			this.defaultProperties.store(new FileOutputStream(new File(
					eventsToSQLPropertiesFileComponent.getText())), "");
		} catch (FileNotFoundException e) {

			fileSelect(eventsToSQLPropertiesFileComponent.getText(),
					"Path not found. Enter properties filename.", false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void runEventsProcessing() {
		boolean isTransit = false;
		MutableScenario scenario = (MutableScenario) ScenarioUtils
				.createScenario(ConfigUtils.loadConfig(configFileComponent
						.getText()));
		scenario.getConfig().transit().setUseTransit(true);
		if (!transitScheduleFileComponent.getText().equals("NA")
				&& !transitScheduleFileComponent.getText().equals("")) {
			new TransitScheduleReader(scenario)
					.readFile(transitScheduleFileComponent.getText());
			isTransit = true;
		}
		new MatsimNetworkReader(scenario).readFile(networkFileComponent
				.getText());

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsToTravelSummaryTables test;
		// if(linkTrafficComponent.isSelected()){
		// test = new EventsToPlanElements(
		// scenario.getTransitSchedule(), scenario.getNetwork(),
		// scenario.getConfig(),new File(postgresPropertiesComponent.getText())
		// ,tableSuffixComponent.getText());
		// }else{
		if (isTransit) {
			test = new EventsToTravelSummaryTables(
					scenario.getTransitSchedule(), scenario.getNetwork(),
					scenario.getConfig());

		} else {
			test = new EventsToTravelSummaryTables(scenario.getNetwork(),
					scenario.getConfig());
		}
		// }
		eventsManager.addHandler(test);
		new MatsimEventsReader(eventsManager).readFile(eventsFileComponent
				.getText());

		try {

			test.writeSimulationResultsToCSV(outputPathComponent.getText(),
					tableSuffixComponent.getText());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Number of stuck vehicles/passengers: "
				+ test.getStuck());

	}

	public void loadDefaultProperties(File defaultPropertiesFile) {
		this.defaultProperties = new Properties();
		try {
			this.defaultProperties.load(new FileInputStream(
					defaultPropertiesFile));
			String[] properties = { "transitScheduleFile", "networkFile",
					"eventsFile", "configFile", "tableSuffix", "outputPath" };
			for (String property : properties) {
				try {
					String propertyValue = this.defaultProperties
							.getProperty(property);
					// reflection
					Field aField = getClass().getDeclaredField(property);
					aField.set(this, propertyValue);
				} catch (NullPointerException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {

					e.printStackTrace();
				}
			}
					setComponentValues();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setComponentValues() {
		outputPathComponent.setText(outputPath);
		transitScheduleFileComponent.setText(transitScheduleFile);
		tableSuffixComponent.setText(tableSuffix);
		configFileComponent.setText(configFile);
		networkFileComponent.setText(networkFile);
		eventsFileComponent.setText(eventsFile);
	}

	public File fileSelect(String path, String title, boolean dirsOnly) {
		boolean validPath = false;
		File file = null;
		try {
			file = new File(path);
			validPath = file.isFile();
			if (validPath)
				this.defaultpath = file.getPath();
		} catch (Exception e) {

		}
		JFileChooser chooser = new JFileChooser(defaultpath);
		if (dirsOnly)
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setToolTipText(title);
		chooser.setDialogTitle(title);
		chooser.showOpenDialog(new JPanel());
		try {
			defaultpath = chooser.getSelectedFile().getPath();
		} catch (NullPointerException ne) {
			// do nothing
		}
		return chooser.getSelectedFile();
	}

}
