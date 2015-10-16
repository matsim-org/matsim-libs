package playground.pieter.events;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

import others.sergioo.util.dataBase.DataBaseAdmin;
import playground.pieter.singapore.utils.postgresql.CSVWriter;
import playground.pieter.singapore.utils.postgresql.PostgresType;
import playground.pieter.singapore.utils.postgresql.PostgresqlCSVWriter;
import playground.pieter.singapore.utils.postgresql.PostgresqlColumnDefinition;
import playground.pieter.singapore.utils.postgresql.TableWriter;
import playground.pieter.wrashid.nan.MainDensityAnalysisWithPt;
import playground.pieter.wrashid.nan.NetworkReadExample;

public class EventsToLinkFlowAndDensityToSQLgui extends JFrame {

    private final JTextField schemaNameComponent;
	private final JTextField postgresPropertiesComponent;
	private final JTextField tableSuffixComponent;
	private final JTextField eventsToLinkFlowAndDensityToSQLPropertiesFileComponent;
	private final JTextField networkFileComponent;
	private final JTextField eventsFileComponent;
	private Properties defaultProperties;
	private final EventsToLinkFlowAndDensityToSQLgui self = this;
	private String defaultpath = "";
	private final JTextField centreYCoordComponent;
	private final JTextField radiusComponent;
	private final JTextField centreXCoordComponent;
	private final JTextField binSizeComponent;
	private String schemaName;
	private String postgresProperties;
	private String tableSuffix;
	private String networkFile;
	private String eventsFile;
	private String centreYCoord;
	private String centreXCoord;
	private String radius;
	private final JTextPane commentComponent;
	private String comment;
	private String binSize;
	private final HashMap<MultiModalFlowAndDensityCollector.FlowType, HashMap<Id<Link>, int[]>> linkOutFlowsByType = new HashMap<>();
	private final HashMap<MultiModalFlowAndDensityCollector.FlowType, HashMap<Id, int[]>> instantaneousLinkOccupancyByType = new HashMap<>();
	private final HashMap<MultiModalFlowAndDensityCollector.FlowType, HashMap<Id, double[]>> averageLinkOccupancyByType = new HashMap<>();
	private Map<Id<Link>, ? extends Link> links;
	private int numberOfTimeBins;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {

				EventsToLinkFlowAndDensityToSQLgui frame = new EventsToLinkFlowAndDensityToSQLgui();
				frame.setVisible(true);
				frame.loadDefaultProperties(new File(frame.eventsToLinkFlowAndDensityToSQLPropertiesFileComponent
						.getText()));

			}
		});
	}

	/**
	 * Create the frame.
	 */
    private EventsToLinkFlowAndDensityToSQLgui() {
		setTitle("Events to Link flows and densities written to PostgreSQL / CSV tables");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 764, 403);
        JPanel contentPane = new JPanel();
		contentPane.setBackground(UIManager.getColor("Panel.background"));
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] { 268, 500 };
		gbl_contentPane.rowHeights = new int[] { 20, 20, 20, 20, 20, 40, 0, 30, 20, 23, 0 };
		gbl_contentPane.columnWeights = new double[] { 1.0, 1.0 };
		gbl_contentPane.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		contentPane.setLayout(gbl_contentPane);

		JLabel lblFillFromProperties = new JLabel("Fill from properties file");
		GridBagConstraints gbc_lblFillFromProperties = new GridBagConstraints();
		gbc_lblFillFromProperties.anchor = GridBagConstraints.WEST;
		gbc_lblFillFromProperties.insets = new Insets(0, 0, 5, 5);
		gbc_lblFillFromProperties.gridx = 0;
		gbc_lblFillFromProperties.gridy = 0;
		contentPane.add(lblFillFromProperties, gbc_lblFillFromProperties);

		eventsToLinkFlowAndDensityToSQLPropertiesFileComponent = new JTextField();
		eventsToLinkFlowAndDensityToSQLPropertiesFileComponent.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				File defaultPropertiesFile = fileSelect(
						eventsToLinkFlowAndDensityToSQLPropertiesFileComponent.getText(), "Select properties file");
				eventsToLinkFlowAndDensityToSQLPropertiesFileComponent.setText(defaultPropertiesFile.getPath());
				loadDefaultProperties(defaultPropertiesFile);
			}
		});
		eventsToLinkFlowAndDensityToSQLPropertiesFileComponent.setText("./eventsToLinkFlowAndDensityToSQL.properties");
		GridBagConstraints gbc_txtDataeventstosqlproperties = new GridBagConstraints();
		gbc_txtDataeventstosqlproperties.anchor = GridBagConstraints.NORTH;
		gbc_txtDataeventstosqlproperties.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtDataeventstosqlproperties.insets = new Insets(0, 0, 5, 0);
		gbc_txtDataeventstosqlproperties.gridx = 1;
		gbc_txtDataeventstosqlproperties.gridy = 0;
		contentPane.add(eventsToLinkFlowAndDensityToSQLPropertiesFileComponent, gbc_txtDataeventstosqlproperties);
		eventsToLinkFlowAndDensityToSQLPropertiesFileComponent.setColumns(10);

		JLabel lblSchemaName = new JLabel("Schema name for SQL/output path for CSVs");
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

		JLabel lblNetwork = new JLabel("Network");
		GridBagConstraints gbc_lblNetwork = new GridBagConstraints();
		gbc_lblNetwork.anchor = GridBagConstraints.WEST;
		gbc_lblNetwork.insets = new Insets(0, 0, 5, 5);
		gbc_lblNetwork.gridx = 0;
		gbc_lblNetwork.gridy = 2;
		contentPane.add(lblNetwork, gbc_lblNetwork);

		networkFileComponent = new JTextField();
		networkFileComponent.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				networkFileComponent.setText(fileSelect(networkFileComponent.getText(), "select network file")
						.getPath());
			}
		});
		networkFileComponent.setText("./");
		GridBagConstraints gbc_networkFile = new GridBagConstraints();
		gbc_networkFile.anchor = GridBagConstraints.NORTH;
		gbc_networkFile.fill = GridBagConstraints.HORIZONTAL;
		gbc_networkFile.insets = new Insets(0, 0, 5, 0);
		gbc_networkFile.gridx = 1;
		gbc_networkFile.gridy = 2;
		contentPane.add(networkFileComponent, gbc_networkFile);
		networkFileComponent.setColumns(10);

		JLabel lblpropertiesForPostgresql = new JLabel(".properties for postgresql (leave empty for CSV)");
		GridBagConstraints gbc_lblpropertiesForPostgresql = new GridBagConstraints();
		gbc_lblpropertiesForPostgresql.anchor = GridBagConstraints.WEST;
		gbc_lblpropertiesForPostgresql.insets = new Insets(0, 0, 5, 5);
		gbc_lblpropertiesForPostgresql.gridx = 0;
		gbc_lblpropertiesForPostgresql.gridy = 3;
		contentPane.add(lblpropertiesForPostgresql, gbc_lblpropertiesForPostgresql);

		postgresPropertiesComponent = new JTextField();
		postgresPropertiesComponent.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				postgresPropertiesComponent.setText(fileSelect(postgresPropertiesComponent.getText(),
						"select PostgreSQL connection properties file").getPath());

			}
		});
		postgresPropertiesComponent.setText("data/matsim2postgres.properties");
		GridBagConstraints gbc_postgresProperties = new GridBagConstraints();
		gbc_postgresProperties.anchor = GridBagConstraints.NORTH;
		gbc_postgresProperties.fill = GridBagConstraints.HORIZONTAL;
		gbc_postgresProperties.insets = new Insets(0, 0, 5, 0);
		gbc_postgresProperties.gridx = 1;
		gbc_postgresProperties.gridy = 3;
		contentPane.add(postgresPropertiesComponent, gbc_postgresProperties);
		postgresPropertiesComponent.setColumns(10);

		JLabel lblEvents = new JLabel("Events file?");
		GridBagConstraints gbc_lblEvents = new GridBagConstraints();
		gbc_lblEvents.anchor = GridBagConstraints.WEST;
		gbc_lblEvents.insets = new Insets(0, 0, 5, 5);
		gbc_lblEvents.gridx = 0;
		gbc_lblEvents.gridy = 4;
		contentPane.add(lblEvents, gbc_lblEvents);

		eventsFileComponent = new JTextField();
		eventsFileComponent.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				eventsFileComponent.setText(fileSelect(eventsFileComponent.getText(), "select events file").getPath());
			}
		});
		eventsFileComponent.setText("./");
		GridBagConstraints gbc_eventsFile = new GridBagConstraints();
		gbc_eventsFile.anchor = GridBagConstraints.NORTH;
		gbc_eventsFile.fill = GridBagConstraints.HORIZONTAL;
		gbc_eventsFile.insets = new Insets(0, 0, 5, 0);
		gbc_eventsFile.gridx = 1;
		gbc_eventsFile.gridy = 4;
		contentPane.add(eventsFileComponent, gbc_eventsFile);
		eventsFileComponent.setColumns(10);

		JTextPane commentLabel = new JTextPane();
		commentLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
		commentLabel.setEditable(false);
		commentLabel.setBackground(UIManager.getColor("Panel.background"));
		commentLabel
				.setText("Table comment (leaving this field empty will identify table by date, events and network file names)");
		GridBagConstraints gbc_commentLabel = new GridBagConstraints();
		gbc_commentLabel.insets = new Insets(0, 0, 5, 5);
		gbc_commentLabel.fill = GridBagConstraints.BOTH;
		gbc_commentLabel.gridx = 0;
		gbc_commentLabel.gridy = 5;
		contentPane.add(commentLabel, gbc_commentLabel);

		commentComponent = new JTextPane();
		GridBagConstraints gbc_commentComponent = new GridBagConstraints();
		gbc_commentComponent.insets = new Insets(0, 0, 5, 0);
		gbc_commentComponent.fill = GridBagConstraints.BOTH;
		gbc_commentComponent.gridx = 1;
		gbc_commentComponent.gridy = 5;
		contentPane.add(commentComponent, gbc_commentComponent);

		JTextPane txtpnCentreCoordinateIf = new JTextPane();
		txtpnCentreCoordinateIf.setEditable(false);
		txtpnCentreCoordinateIf.setBackground(UIManager.getColor("Panel.background"));
		txtpnCentreCoordinateIf
				.setText("Centre coordinate if you want to only process a subset of links within a certain radius from this coord. "
						+ "Leaving these fields empty will process the entire network.");
		GridBagConstraints gbc_txtpnCentreCoordinateIf = new GridBagConstraints();
		gbc_txtpnCentreCoordinateIf.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnCentreCoordinateIf.fill = GridBagConstraints.BOTH;
		gbc_txtpnCentreCoordinateIf.gridx = 0;
		gbc_txtpnCentreCoordinateIf.gridy = 6;
		contentPane.add(txtpnCentreCoordinateIf, gbc_txtpnCentreCoordinateIf);

		JPanel panel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.insets = new Insets(0, 0, 5, 0);
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 1;
		gbc_panel.gridy = 6;
		contentPane.add(panel, gbc_panel);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[] { 200, 200, 100 };
		gbl_panel.rowHeights = new int[] { 20, 20 };
		gbl_panel.columnWeights = new double[] { 1.0, 1.0, 1.0 };
		gbl_panel.rowWeights = new double[] { 0.0, 0.0 };
		panel.setLayout(gbl_panel);

		JLabel lblNewLabel = new JLabel("Centre: x ");
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		panel.add(lblNewLabel, gbc_lblNewLabel);

		JLabel lblCentreY = new JLabel("Centre: y");
		GridBagConstraints gbc_lblCentreY = new GridBagConstraints();
		gbc_lblCentreY.insets = new Insets(0, 0, 5, 5);
		gbc_lblCentreY.gridx = 1;
		gbc_lblCentreY.gridy = 0;
		panel.add(lblCentreY, gbc_lblCentreY);

		JLabel lblRadiusmeters = new JLabel("Radius");
		GridBagConstraints gbc_lblRadiusmeters = new GridBagConstraints();
		gbc_lblRadiusmeters.insets = new Insets(0, 0, 5, 0);
		gbc_lblRadiusmeters.gridx = 2;
		gbc_lblRadiusmeters.gridy = 0;
		panel.add(lblRadiusmeters, gbc_lblRadiusmeters);

		centreXCoordComponent = new JTextField();
		GridBagConstraints gbc_xCoordComponent = new GridBagConstraints();
		gbc_xCoordComponent.insets = new Insets(0, 0, 0, 5);
		gbc_xCoordComponent.fill = GridBagConstraints.HORIZONTAL;
		gbc_xCoordComponent.gridx = 0;
		gbc_xCoordComponent.gridy = 1;
		panel.add(centreXCoordComponent, gbc_xCoordComponent);
		centreXCoordComponent.setColumns(10);

		centreYCoordComponent = new JTextField();
		GridBagConstraints gbc_yCoordComponent = new GridBagConstraints();
		gbc_yCoordComponent.insets = new Insets(0, 0, 0, 5);
		gbc_yCoordComponent.fill = GridBagConstraints.HORIZONTAL;
		gbc_yCoordComponent.gridx = 1;
		gbc_yCoordComponent.gridy = 1;
		panel.add(centreYCoordComponent, gbc_yCoordComponent);
		centreYCoordComponent.setColumns(10);

		radiusComponent = new JTextField();
		radiusComponent.setText("5000");
		GridBagConstraints gbc_radiusComponent = new GridBagConstraints();
		gbc_radiusComponent.fill = GridBagConstraints.HORIZONTAL;
		gbc_radiusComponent.gridx = 2;
		gbc_radiusComponent.gridy = 1;
		panel.add(radiusComponent, gbc_radiusComponent);
		radiusComponent.setColumns(10);

		JLabel lblBinSizeFor = new JLabel("Bin size for analysis (seconds) - CAREFUL!");
		GridBagConstraints gbc_lblBinSizeFor = new GridBagConstraints();
		gbc_lblBinSizeFor.anchor = GridBagConstraints.WEST;
		gbc_lblBinSizeFor.insets = new Insets(0, 0, 5, 5);
		gbc_lblBinSizeFor.gridx = 0;
		gbc_lblBinSizeFor.gridy = 7;
		contentPane.add(lblBinSizeFor, gbc_lblBinSizeFor);

		binSizeComponent = new JTextField();
		binSizeComponent.setText("300");
		GridBagConstraints gbc_binSizeComponent = new GridBagConstraints();
		gbc_binSizeComponent.insets = new Insets(0, 0, 5, 0);
		gbc_binSizeComponent.fill = GridBagConstraints.HORIZONTAL;
		gbc_binSizeComponent.gridx = 1;
		gbc_binSizeComponent.gridy = 7;
		contentPane.add(binSizeComponent, gbc_binSizeComponent);
		binSizeComponent.setColumns(10);

		JLabel lblTableNameSuffix = new JLabel("Table name suffix (to distinguish between runs)");
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

		JButton btnStartEventsProcessing = new JButton("START EVENTS PROCESSING");
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
		gbc_btnStartEventsProcessing.gridy = 9;
		contentPane.add(btnStartEventsProcessing, gbc_btnStartEventsProcessing);
	}

	void saveDefaultProperties() {
		this.defaultProperties = new Properties();

		this.defaultProperties.setProperty("schemaName", schemaNameComponent.getText());
		this.defaultProperties.setProperty("postgresProperties", postgresPropertiesComponent.getText());
		this.defaultProperties.setProperty("tableSuffix", tableSuffixComponent.getText());
		this.defaultProperties.setProperty("networkFile", networkFileComponent.getText());
		this.defaultProperties.setProperty("eventsFile", eventsFileComponent.getText());
		this.defaultProperties.setProperty("comment", commentComponent.getText());
		this.defaultProperties.setProperty("radius", radiusComponent.getText());
		this.defaultProperties.setProperty("centreXCoord", centreXCoordComponent.getText());
		this.defaultProperties.setProperty("centreYCoord", centreYCoordComponent.getText());
		this.defaultProperties.setProperty("binSize", binSizeComponent.getText());
		try {
			this.defaultProperties.store(new FileOutputStream(new File(
					eventsToLinkFlowAndDensityToSQLPropertiesFileComponent.getText())), "");
		} catch (FileNotFoundException e) {

			fileSelect(eventsToLinkFlowAndDensityToSQLPropertiesFileComponent.getText(),
					"Path not found. Enter proerties filename.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void runEventsProcessing() throws InstantiationException, IllegalAccessException, ClassNotFoundException,
			IOException, SQLException {
		String networkFile = networkFileComponent.getText();
		String eventsFile = eventsFileComponent.getText();
		String x = centreXCoordComponent.getText();
		String y = centreYCoordComponent.getText();
		// if any one of these is empty, or invalid, process the entire network
		Coord center = null;
		double radiusInMeters = 5000;
		try {
			center = new Coord(Double.parseDouble(x), Double.parseDouble(y)); // center=null means use all links
			radiusInMeters = Double.parseDouble(radiusComponent.getText());
		} catch (Exception e) {

		}
		boolean isOldEventFile = false;
		int binSizeInSeconds = Integer.parseInt(binSizeComponent.getText());

		// String
		// networkFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/examples/equil/network.xml";
		// String
		// eventsFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/output/equil/ITERS/it.5/5.events.txt.gz";
		// Coord center=new CoordImpl(0,0);
		// boolean isOldEventFile=false;

		links = NetworkReadExample.getNetworkLinks(networkFile, center, radiusInMeters);// input/set
																						// center
																						// and
																						// radius
																						// InFlowInfoCollectorWithPt
																						// inflowHandler
																						// =
																						// new
																						// InFlowInfoCollectorWithPt(
		// links, isOldEventFile, binSizeInSeconds);
		// OutFlowInfoCollectorWithPt outflowHandler = new
		// OutFlowInfoCollectorWithPt(
		// links, isOldEventFile, binSizeInSeconds);// "links" makes run
		// // faster
		//
		// inflowHandler.reset(0);
		// outflowHandler.reset(0);
		MultiModalFlowAndDensityCollector flowAndDensityCollector = new MultiModalFlowAndDensityCollector(links,
				binSizeInSeconds);
		flowAndDensityCollector.reset(0);
		this.numberOfTimeBins = flowAndDensityCollector.getNumberOfTimeBins();

		EventsManager events = EventsUtils.createEventsManager(); // create new
																	// object of
																	// events-manager
																	// class

		// events.addHandler(inflowHandler); // add handler
		// events.addHandler(outflowHandler);
		events.addHandler(flowAndDensityCollector);

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);

		reader.parse(eventsFile); // where we find events data
		for (MultiModalFlowAndDensityCollector.FlowType flowType : MultiModalFlowAndDensityCollector.FlowType.values()) {
			HashMap<Id<Link>, int[]> linkInFlow = flowAndDensityCollector.getLinkInFlow(flowType);
			HashMap<Id<Link>, int[]> linkOutFlow = flowAndDensityCollector.getLinkOutFlow(flowType);

			HashMap<Id<Link>, int[]> deltaFlow = MainDensityAnalysisWithPt.deltaFlow(linkInFlow, linkOutFlow);
			HashMap<Id, int[]> instantaneousLinkOccupancy = flowAndDensityCollector.calculateOccupancy(deltaFlow,
					links);
			HashMap<Id, double[]> averageLinkDensities = flowAndDensityCollector.getAvgDeltaFlow(flowType);
			// add to the map of flows
			this.linkOutFlowsByType.put(flowType, linkOutFlow);
			this.instantaneousLinkOccupancyByType.put(flowType, instantaneousLinkOccupancy);
			this.averageLinkOccupancyByType.put(flowType, averageLinkDensities);
		}
		// if there arent any postgres properties, attempt to write to csv, else
		// write to postgres
		writeResults(!postgresPropertiesComponent.getText().equals(""));
		System.out.println("entering:exiting = " + flowAndDensityCollector.getEnterLinkCount()+":"+flowAndDensityCollector.getLeavLinkCount());
	}

	private void writeResults(boolean toSQL) throws InstantiationException, IllegalAccessException,
			ClassNotFoundException, IOException, SQLException {
		DateFormat df = new SimpleDateFormat("yyyy_MM_dd");
		String formattedDate = df.format(new Date());
		// start with activities
		String densityTableName = "matsim_link_flow_and_density" + tableSuffixComponent.getText();
		List<PostgresqlColumnDefinition> columns = new ArrayList<>();
		columns.add(new PostgresqlColumnDefinition("link_id", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("length", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("lanes", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("bin_start_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("allowed_modes", PostgresType.TEXT));
		for (MultiModalFlowAndDensityCollector.FlowType flowType : MultiModalFlowAndDensityCollector.FlowType.values()) {
			columns.add(new PostgresqlColumnDefinition("outflow_" + flowType.toString().toLowerCase(), PostgresType.INT));
			columns.add(new PostgresqlColumnDefinition("vehs_on_link_instant_" + flowType.toString().toLowerCase(),
					PostgresType.INT));
			columns.add(new PostgresqlColumnDefinition("avg_vehs_on_link_" + flowType.toString().toLowerCase(),
					PostgresType.FLOAT8));
		}
		TableWriter densityWriter = null;
		if (toSQL) {
			String tabname = schemaNameComponent.getText() + "." + densityTableName;
			String fileName = postgresPropertiesComponent.getText();
			File file = new File(fileName);
			DataBaseAdmin dba = new DataBaseAdmin(file);
			densityWriter = new PostgresqlCSVWriter("DENSITYWRITER", tabname, dba, 100, columns);
		} else {
			densityWriter = new CSVWriter("DENSITYWRITER", densityTableName, schemaNameComponent.getText(), 1000,
					columns);
		}
		if (!commentComponent.getText().equals("")) {
			densityWriter.addComment(commentComponent.getText().replaceAll("[^a-zA-Z0-9-.]", " "));
		} else {
			String eventsFileName = eventsFileComponent.getText();
			eventsFileName = eventsFileName.replaceAll("\\\\", "/");
			eventsFileName = eventsFileName.replaceAll(":", "");
			String networkFileName = networkFileComponent.getText();
			networkFileName = networkFileName.replaceAll("\\\\", "/");
			networkFileName = networkFileName.replaceAll(":", "");
			densityWriter
					.addComment(String
							.format("Link outflow and occupancy for network %s from events file %s, created on %s. Bin size is %s",
									networkFileName, eventsFileName, formattedDate, binSizeComponent.getText()));

		}
		for (Id id : links.keySet()) {
			for (int i = 0; i < this.numberOfTimeBins; i++) {
				Object[] args = new Object[columns.size()];
				args[0] = id.toString();
				args[1] = links.get(id).getLength();
				args[2] = links.get(id).getNumberOfLanes();
				args[3] = i * Integer.parseInt(this.binSizeComponent.getText());
				String modeString="";
				for(String mode:links.get(id).getAllowedModes()){
					modeString += mode + " ";
				}
				args[4] = modeString;
				int argsIndex = 5;
				double flowSum=0.0;
				for (MultiModalFlowAndDensityCollector.FlowType flowType : MultiModalFlowAndDensityCollector.FlowType
						.values()) {
					HashMap<Id<Link>, int[]> linkOutFlow = this.linkOutFlowsByType.get(flowType);
					HashMap<Id, int[]> instantaneousLinkOccupancy = this.instantaneousLinkOccupancyByType
							.get(flowType);
					HashMap<Id, double[]> averageLinkDensities = this.averageLinkOccupancyByType.get(flowType);
					int[] flows = linkOutFlow.get(id);
					int[] occupancy = instantaneousLinkOccupancy.get(id);
					double[] avgOccup = averageLinkDensities.get(id);
					args[argsIndex] = flows == null ? 0 : flows[i];
					flowSum += (Integer)args[argsIndex++];
					args[argsIndex] = occupancy == null ? 0 : occupancy[i];
					flowSum += (Integer)args[argsIndex++];
					args[argsIndex] = avgOccup == null ? 0 : avgOccup[i];
					flowSum += (Double)args[argsIndex++];
				}
				if(flowSum>0){
					densityWriter.addLine(args);					
				}
			}

		}
		densityWriter.finish();
	}

	void loadDefaultProperties(File defaultPropertiesFile) {
		this.defaultProperties = new Properties();
		try {
			this.defaultProperties.load(new FileInputStream(defaultPropertiesFile));
			String[] properties = { "comment", "networkFile", "eventsFile", "centreXCoord", "centreYCoord",
					"tableSuffix", "schemaName", "postgresProperties", "radius", "binSize" };
			for (String property : properties) {
				try {
					String propertyValue = this.defaultProperties.getProperty(property);
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
		schemaNameComponent.setText(schemaName);
		postgresPropertiesComponent.setText(postgresProperties);
		tableSuffixComponent.setText(tableSuffix);
		networkFileComponent.setText(networkFile);
		eventsFileComponent.setText(eventsFile);
		centreXCoordComponent.setText(centreXCoord);
		centreYCoordComponent.setText(centreYCoord);
		radiusComponent.setText(radius);
		commentComponent.setText(comment);
		binSizeComponent.setText(binSize);
	}

	File fileSelect(String path, String title) {
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
		chooser.setToolTipText(title);
		chooser.setDialogTitle(title);
		chooser.showOpenDialog(new JPanel());
		defaultpath = chooser.getSelectedFile().getPath();
		return chooser.getSelectedFile();
	}
}
