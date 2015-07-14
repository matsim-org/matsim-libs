package playground.pieter.singapore.utils.events;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JButton;
import javax.swing.JTextField;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import others.sergioo.util.dataBase.NoConnectionException;

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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class EventsToSQLInterfaceSingapore extends JFrame {

    private final JTextField schemaNameComponent;
	private final JTextField transitScheduleFileComponent;
	private final JTextField postgresPropertiesComponent;
	private final JTextField tableSuffixComponent;
	private final JTextField configFileComponent;
	private final JTextField eventsToSQLPropertiesFileComponent;
	private final JTextField networkFileComponent;
	private final JTextField eventsFileComponent;
	private Properties defaultProperties;
    private final String configFile = "examples/pt-tutorial/0.config.xml";
	private final String tableSuffix = "_ezlinksim";
    private final EventsToSQLInterfaceSingapore self = this;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					EventsToSQLInterfaceSingapore frame = new EventsToSQLInterfaceSingapore();
					frame.setVisible(true);
					frame.loadDefaultProperties(new File(
							"data/eventsToSQL.properties"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
    private EventsToSQLInterfaceSingapore() {
		setTitle("Events to PostgreSQL tables");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 764, 300);
        JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] { 144, 509, 0 };
		gbl_contentPane.rowHeights = new int[] { 20, 20, 20, 20, 20, 20, 23,
				20, 20, 23, 0 };
		gbl_contentPane.columnWeights = new double[] { 0.0, 1.0,
				Double.MIN_VALUE };
		gbl_contentPane.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0,
				0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		contentPane.setLayout(gbl_contentPane);

		JLabel lblFillFromProperties = new JLabel("Fill from properties file");
		GridBagConstraints gbc_lblFillFromProperties = new GridBagConstraints();
		gbc_lblFillFromProperties.anchor = GridBagConstraints.WEST;
		gbc_lblFillFromProperties.insets = new Insets(0, 0, 5, 5);
		gbc_lblFillFromProperties.gridx = 0;
		gbc_lblFillFromProperties.gridy = 0;
		contentPane.add(lblFillFromProperties, gbc_lblFillFromProperties);

		eventsToSQLPropertiesFileComponent = new JTextField();
		eventsToSQLPropertiesFileComponent.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyChar()=='\n'){
					File defaultPropertiesFile = new File(
							eventsToSQLPropertiesFileComponent.getText());
					eventsToSQLPropertiesFileComponent
							.setText(defaultPropertiesFile.getPath());
					loadDefaultProperties(defaultPropertiesFile);
				}
			}
		});
		eventsToSQLPropertiesFileComponent.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				File defaultPropertiesFile = fileSelect(
						eventsToSQLPropertiesFileComponent.getText(),
						"Select properties file");
				eventsToSQLPropertiesFileComponent
						.setText(defaultPropertiesFile.getPath());
				loadDefaultProperties(defaultPropertiesFile);
			}
		});
		eventsToSQLPropertiesFileComponent
				.setText("data/eventsToSQL.properties");
		GridBagConstraints gbc_txtDataeventstosqlproperties = new GridBagConstraints();
		gbc_txtDataeventstosqlproperties.anchor = GridBagConstraints.NORTH;
		gbc_txtDataeventstosqlproperties.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtDataeventstosqlproperties.insets = new Insets(0, 0, 5, 0);
		gbc_txtDataeventstosqlproperties.gridx = 1;
		gbc_txtDataeventstosqlproperties.gridy = 0;
		contentPane.add(eventsToSQLPropertiesFileComponent,
                gbc_txtDataeventstosqlproperties);
		eventsToSQLPropertiesFileComponent.setColumns(10);

		JLabel lblSchemaName = new JLabel("Schema Name for output");
		lblSchemaName.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_lblSchemaName = new GridBagConstraints();
		gbc_lblSchemaName.anchor = GridBagConstraints.WEST;
		gbc_lblSchemaName.insets = new Insets(0, 0, 5, 5);
		gbc_lblSchemaName.gridx = 0;
		gbc_lblSchemaName.gridy = 1;
		contentPane.add(lblSchemaName, gbc_lblSchemaName);

		schemaNameComponent = new JTextField();
		schemaNameComponent.setText("m_calibration");
		GridBagConstraints gbc_schemaName = new GridBagConstraints();
		gbc_schemaName.anchor = GridBagConstraints.NORTH;
		gbc_schemaName.fill = GridBagConstraints.HORIZONTAL;
		gbc_schemaName.insets = new Insets(0, 0, 5, 0);
		gbc_schemaName.gridx = 1;
		gbc_schemaName.gridy = 1;
		contentPane.add(schemaNameComponent, gbc_schemaName);
		schemaNameComponent.setColumns(10);

		JLabel lblTransitSchedule = new JLabel("Transit schedule");
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
						"select transit schedule file").getPath());
			}
		});
		transitScheduleFileComponent
				.setText("data/sing2.2/input/transit/transitSchedule.xml.gz");
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
						networkFileComponent.getText(),"select network file").getPath());
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
						configFileComponent.getText(),"select config file").getPath());
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

		JLabel lblpropertiesForPostgresql = new JLabel(
				".properties for postgresql");
		GridBagConstraints gbc_lblpropertiesForPostgresql = new GridBagConstraints();
		gbc_lblpropertiesForPostgresql.anchor = GridBagConstraints.WEST;
		gbc_lblpropertiesForPostgresql.insets = new Insets(0, 0, 5, 5);
		gbc_lblpropertiesForPostgresql.gridx = 0;
		gbc_lblpropertiesForPostgresql.gridy = 5;
		contentPane.add(lblpropertiesForPostgresql,
                gbc_lblpropertiesForPostgresql);

		postgresPropertiesComponent = new JTextField();
		postgresPropertiesComponent.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				postgresPropertiesComponent.setText(fileSelect(
						postgresPropertiesComponent.getText(),"select PostgreSQL connection properties file").getPath());

			}
		});
		postgresPropertiesComponent.setText("data/matsim2postgres.properties");
		GridBagConstraints gbc_postgresProperties = new GridBagConstraints();
		gbc_postgresProperties.anchor = GridBagConstraints.NORTH;
		gbc_postgresProperties.fill = GridBagConstraints.HORIZONTAL;
		gbc_postgresProperties.insets = new Insets(0, 0, 5, 0);
		gbc_postgresProperties.gridx = 1;
		gbc_postgresProperties.gridy = 5;
		contentPane.add(postgresPropertiesComponent, gbc_postgresProperties);
		postgresPropertiesComponent.setColumns(10);

		JLabel lblEvents = new JLabel("Events file?");
		GridBagConstraints gbc_lblEvents = new GridBagConstraints();
		gbc_lblEvents.anchor = GridBagConstraints.WEST;
		gbc_lblEvents.insets = new Insets(0, 0, 5, 5);
		gbc_lblEvents.gridx = 0;
		gbc_lblEvents.gridy = 7;
		contentPane.add(lblEvents, gbc_lblEvents);

		eventsFileComponent = new JTextField();
		eventsFileComponent.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				eventsFileComponent.setText(fileSelect(
						eventsFileComponent.getText(),"select events file").getPath());
			}
		});
		eventsFileComponent.setText("events.xml");
		GridBagConstraints gbc_eventsFile = new GridBagConstraints();
		gbc_eventsFile.anchor = GridBagConstraints.NORTH;
		gbc_eventsFile.fill = GridBagConstraints.HORIZONTAL;
		gbc_eventsFile.insets = new Insets(0, 0, 5, 0);
		gbc_eventsFile.gridx = 1;
		gbc_eventsFile.gridy = 7;
		contentPane.add(eventsFileComponent, gbc_eventsFile);
		eventsFileComponent.setColumns(10);

		JLabel lblTableNameSuffix = new JLabel("Table name suffix");
		GridBagConstraints gbc_lblTableNameSuffix = new GridBagConstraints();
		gbc_lblTableNameSuffix.anchor = GridBagConstraints.WEST;
		gbc_lblTableNameSuffix.insets = new Insets(0, 0, 5, 5);
		gbc_lblTableNameSuffix.gridx = 0;
		gbc_lblTableNameSuffix.gridy = 8;
		contentPane.add(lblTableNameSuffix, gbc_lblTableNameSuffix);

		tableSuffixComponent = new JTextField();
		tableSuffixComponent.setText("_test");
		GridBagConstraints gbc_tableSuffix = new GridBagConstraints();
		gbc_tableSuffix.anchor = GridBagConstraints.NORTH;
		gbc_tableSuffix.fill = GridBagConstraints.HORIZONTAL;
		gbc_tableSuffix.insets = new Insets(0, 0, 5, 0);
		gbc_tableSuffix.gridx = 1;
		gbc_tableSuffix.gridy = 8;
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
		gbc_btnSaveAsDefault.gridy = 9;
		contentPane.add(btnSaveAsDefault, gbc_btnSaveAsDefault);

		JButton btnStartEventsProcessing = new JButton(
				"START EVENTS PROCESSING");
		btnStartEventsProcessing.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				self.setVisible(false);
				runEventsProcessing();
				System.exit(0);
			}
		});
		GridBagConstraints gbc_btnStartEventsProcessing = new GridBagConstraints();
		gbc_btnStartEventsProcessing.anchor = GridBagConstraints.NORTH;
		gbc_btnStartEventsProcessing.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnStartEventsProcessing.gridx = 1;
		gbc_btnStartEventsProcessing.gridy = 9;
		contentPane.add(btnStartEventsProcessing, gbc_btnStartEventsProcessing);
	}

	void saveDefaultProperties() {
		this.defaultProperties = new Properties();

		this.defaultProperties.setProperty("schemaName",
				schemaNameComponent.getText());
		this.defaultProperties.setProperty("transitScheduleFile",
				transitScheduleFileComponent.getText());
		this.defaultProperties.setProperty("postgresProperties",
				postgresPropertiesComponent.getText());
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

			fileSelect(eventsToSQLPropertiesFileComponent.getText(),"Path not found. Enter proerties filename.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void runEventsProcessing() {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils
				.createScenario(ConfigUtils.loadConfig(configFile));
		scenario.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(scenario)
				.readFile(transitScheduleFileComponent.getText());
		new MatsimNetworkReader(scenario).readFile(networkFileComponent
				.getText());

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsToPlanElementsSingapore test;
		// if(linkTrafficComponent.isSelected()){
		// test = new EventsToPlanElements(
		// scenario.getTransitSchedule(), scenario.getNetwork(),
		// scenario.getConfig(),new File(postgresPropertiesComponent.getText())
		// ,tableSuffixComponent.getText());
		// }else{
		test = new EventsToPlanElementsSingapore(scenario.getTransitSchedule(),
				scenario.getNetwork(), scenario.getConfig(), tableSuffix, schemaNameComponent.getText());
		// }
		eventsManager.addHandler(test);
		new MatsimEventsReader(eventsManager).readFile(eventsFileComponent
				.getText());
		if (test.isWriteIdsForLinks())
			test.getLinkWriter().finish();

		try {
			test.writeSimulationResultsToSQL(new File(
					postgresPropertiesComponent.getText()), eventsFileComponent
					.getText(), tableSuffixComponent.getText());
		} catch (InstantiationException | NoConnectionException | SQLException | IOException | ClassNotFoundException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        System.out.println(test.getStuck());
		if (test.isWriteIdsForLinks())
			test.indexLinkRecords(
					new File(postgresPropertiesComponent.getText()),
					tableSuffixComponent.getText());
	}

	void loadDefaultProperties(File defaultPropertiesFile) {
		this.defaultProperties = new Properties();
		try {
			this.defaultProperties.load(new FileInputStream(
					defaultPropertiesFile));
			String[] properties = { "transitScheduleFile", "networkFile",
					"eventsFile", "configFile",  "tableSuffix",
					"schemaName", "postgresProperties" };
			for (String property : properties) {
				try {
					String propertyValue = this.defaultProperties
							.getProperty(property);
					// reflection
					Field aField = getClass().getDeclaredField(property);
					aField.set(this, propertyValue);
					setComponentValues();
				} catch (NullPointerException | SecurityException | NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
					e.printStackTrace();
				}
            }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setComponentValues() {
        String schemaName = "u_fouriep";
        schemaNameComponent.setText(schemaName);
        String transitScheduleFile = "examples/pt-tutorial/transitschedule.xml";
        transitScheduleFileComponent.setText(transitScheduleFile);
        String postgresProperties = "data/matsim2postgres.properties";
        postgresPropertiesComponent.setText(postgresProperties);
		tableSuffixComponent.setText(tableSuffix);
		configFileComponent.setText(configFile);
        String networkFile = "examples/pt-tutorial/multimodalnetwork.xml";
        networkFileComponent.setText(networkFile);
        String eventsFile = "output/pt-tutorial/ITERS/it.0/0.events.xml.gz";
        eventsFileComponent.setText(eventsFile);
		// linkTrafficComponent.setSelected(Boolean.parseBoolean(linkTraffic));
	}

	File fileSelect(String defaultPath, String title) {
		JFileChooser chooser = new JFileChooser(defaultPath);
		chooser.setToolTipText(title);
		chooser.setDialogTitle(title);
		chooser.showOpenDialog(new JPanel());
		return chooser.getSelectedFile();
	}
}
