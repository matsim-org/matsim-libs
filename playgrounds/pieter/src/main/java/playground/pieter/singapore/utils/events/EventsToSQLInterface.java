package playground.pieter.singapore.utils.events;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JButton;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JCheckBox;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

public class EventsToSQLInterface extends JFrame {

	private JPanel contentPane;
	private JTextField txtMcalibration;
	private JTextField txtDatasinginputtransittransitschedulexmlgz;
	private JTextField txtPostgresProperties;
	private JTextField txttest;
	private JTextField txtConfig;
	private JTextField txtDataeventstosqlproperties;
	private JTextField txtNetworkxml;
	private JTextField txtEventsxml;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					EventsToSQLInterface frame = new EventsToSQLInterface();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public EventsToSQLInterface() {
		setTitle("Events to PostgreSQL tables");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[]{144, 262, 0};
		gbl_contentPane.rowHeights = new int[]{20, 20, 20, 20, 20, 20, 23, 20, 20, 23, 0};
		gbl_contentPane.columnWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		gbl_contentPane.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		contentPane.setLayout(gbl_contentPane);
		
		JLabel lblFillFromProperties = new JLabel("Fill from properties file");
		GridBagConstraints gbc_lblFillFromProperties = new GridBagConstraints();
		gbc_lblFillFromProperties.anchor = GridBagConstraints.WEST;
		gbc_lblFillFromProperties.insets = new Insets(0, 0, 5, 5);
		gbc_lblFillFromProperties.gridx = 0;
		gbc_lblFillFromProperties.gridy = 0;
		contentPane.add(lblFillFromProperties, gbc_lblFillFromProperties);
		
		txtDataeventstosqlproperties = new JTextField();
		txtDataeventstosqlproperties.setText("data/eventsToSQL.properties");
		GridBagConstraints gbc_txtDataeventstosqlproperties = new GridBagConstraints();
		gbc_txtDataeventstosqlproperties.anchor = GridBagConstraints.NORTH;
		gbc_txtDataeventstosqlproperties.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtDataeventstosqlproperties.insets = new Insets(0, 0, 5, 0);
		gbc_txtDataeventstosqlproperties.gridx = 1;
		gbc_txtDataeventstosqlproperties.gridy = 0;
		contentPane.add(txtDataeventstosqlproperties, gbc_txtDataeventstosqlproperties);
		txtDataeventstosqlproperties.setColumns(10);
		
		JLabel lblSchemaName = new JLabel("Schema Name for output");
		lblSchemaName.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_lblSchemaName = new GridBagConstraints();
		gbc_lblSchemaName.anchor = GridBagConstraints.WEST;
		gbc_lblSchemaName.insets = new Insets(0, 0, 5, 5);
		gbc_lblSchemaName.gridx = 0;
		gbc_lblSchemaName.gridy = 1;
		contentPane.add(lblSchemaName, gbc_lblSchemaName);
		
		txtMcalibration = new JTextField();
		txtMcalibration.setText("m_calibration");
		GridBagConstraints gbc_txtMcalibration = new GridBagConstraints();
		gbc_txtMcalibration.anchor = GridBagConstraints.NORTH;
		gbc_txtMcalibration.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtMcalibration.insets = new Insets(0, 0, 5, 0);
		gbc_txtMcalibration.gridx = 1;
		gbc_txtMcalibration.gridy = 1;
		contentPane.add(txtMcalibration, gbc_txtMcalibration);
		txtMcalibration.setColumns(10);
		
		JLabel lblTransitSchedule = new JLabel("Transit schedule");
		lblTransitSchedule.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_lblTransitSchedule = new GridBagConstraints();
		gbc_lblTransitSchedule.anchor = GridBagConstraints.WEST;
		gbc_lblTransitSchedule.insets = new Insets(0, 0, 5, 5);
		gbc_lblTransitSchedule.gridx = 0;
		gbc_lblTransitSchedule.gridy = 2;
		contentPane.add(lblTransitSchedule, gbc_lblTransitSchedule);
		
		txtDatasinginputtransittransitschedulexmlgz = new JTextField();
		txtDatasinginputtransittransitschedulexmlgz.setText("data/sing2.2/input/transit/transitSchedule.xml.gz");
		GridBagConstraints gbc_txtDatasinginputtransittransitschedulexmlgz = new GridBagConstraints();
		gbc_txtDatasinginputtransittransitschedulexmlgz.anchor = GridBagConstraints.NORTH;
		gbc_txtDatasinginputtransittransitschedulexmlgz.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtDatasinginputtransittransitschedulexmlgz.insets = new Insets(0, 0, 5, 0);
		gbc_txtDatasinginputtransittransitschedulexmlgz.gridx = 1;
		gbc_txtDatasinginputtransittransitschedulexmlgz.gridy = 2;
		contentPane.add(txtDatasinginputtransittransitschedulexmlgz, gbc_txtDatasinginputtransittransitschedulexmlgz);
		txtDatasinginputtransittransitschedulexmlgz.setColumns(10);
		
		JLabel lblNetwork = new JLabel("Network");
		GridBagConstraints gbc_lblNetwork = new GridBagConstraints();
		gbc_lblNetwork.anchor = GridBagConstraints.WEST;
		gbc_lblNetwork.insets = new Insets(0, 0, 5, 5);
		gbc_lblNetwork.gridx = 0;
		gbc_lblNetwork.gridy = 3;
		contentPane.add(lblNetwork, gbc_lblNetwork);
		
		txtNetworkxml = new JTextField();
		txtNetworkxml.setText("network.xml");
		GridBagConstraints gbc_txtNetworkxml = new GridBagConstraints();
		gbc_txtNetworkxml.anchor = GridBagConstraints.NORTH;
		gbc_txtNetworkxml.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtNetworkxml.insets = new Insets(0, 0, 5, 0);
		gbc_txtNetworkxml.gridx = 1;
		gbc_txtNetworkxml.gridy = 3;
		contentPane.add(txtNetworkxml, gbc_txtNetworkxml);
		txtNetworkxml.setColumns(10);
		
		JLabel lblConfig = new JLabel("Config");
		GridBagConstraints gbc_lblConfig = new GridBagConstraints();
		gbc_lblConfig.anchor = GridBagConstraints.WEST;
		gbc_lblConfig.insets = new Insets(0, 0, 5, 5);
		gbc_lblConfig.gridx = 0;
		gbc_lblConfig.gridy = 4;
		contentPane.add(lblConfig, gbc_lblConfig);
		
		txtConfig = new JTextField();
		txtConfig.setText("config.xml");
		GridBagConstraints gbc_txtConfig = new GridBagConstraints();
		gbc_txtConfig.anchor = GridBagConstraints.NORTH;
		gbc_txtConfig.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtConfig.insets = new Insets(0, 0, 5, 0);
		gbc_txtConfig.gridx = 1;
		gbc_txtConfig.gridy = 4;
		contentPane.add(txtConfig, gbc_txtConfig);
		txtConfig.setColumns(10);
		
		JLabel lblEvents = new JLabel("Events file?");
		GridBagConstraints gbc_lblEvents = new GridBagConstraints();
		gbc_lblEvents.anchor = GridBagConstraints.WEST;
		gbc_lblEvents.insets = new Insets(0, 0, 5, 5);
		gbc_lblEvents.gridx = 0;
		gbc_lblEvents.gridy = 5;
		contentPane.add(lblEvents, gbc_lblEvents);
		
		txtEventsxml = new JTextField();
		txtEventsxml.setText("events.xml");
		GridBagConstraints gbc_txtEventsxml = new GridBagConstraints();
		gbc_txtEventsxml.anchor = GridBagConstraints.NORTH;
		gbc_txtEventsxml.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtEventsxml.insets = new Insets(0, 0, 5, 0);
		gbc_txtEventsxml.gridx = 1;
		gbc_txtEventsxml.gridy = 5;
		contentPane.add(txtEventsxml, gbc_txtEventsxml);
		txtEventsxml.setColumns(10);
		
		JCheckBox chckbxLinkTraffic = new JCheckBox("Produce link traffic table? (huge)");
		GridBagConstraints gbc_chckbxLinkTraffic = new GridBagConstraints();
		gbc_chckbxLinkTraffic.anchor = GridBagConstraints.NORTHWEST;
		gbc_chckbxLinkTraffic.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxLinkTraffic.gridx = 1;
		gbc_chckbxLinkTraffic.gridy = 6;
		contentPane.add(chckbxLinkTraffic, gbc_chckbxLinkTraffic);
		
		JLabel lblpropertiesForPostgresql = new JLabel(".properties for postgresql");
		GridBagConstraints gbc_lblpropertiesForPostgresql = new GridBagConstraints();
		gbc_lblpropertiesForPostgresql.anchor = GridBagConstraints.WEST;
		gbc_lblpropertiesForPostgresql.insets = new Insets(0, 0, 5, 5);
		gbc_lblpropertiesForPostgresql.gridx = 0;
		gbc_lblpropertiesForPostgresql.gridy = 7;
		contentPane.add(lblpropertiesForPostgresql, gbc_lblpropertiesForPostgresql);
		
		txtPostgresProperties = new JTextField();
		txtPostgresProperties.setText("data/matsim2postgres.properties");
		GridBagConstraints gbc_txtPostgresProperties = new GridBagConstraints();
		gbc_txtPostgresProperties.anchor = GridBagConstraints.NORTH;
		gbc_txtPostgresProperties.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtPostgresProperties.insets = new Insets(0, 0, 5, 0);
		gbc_txtPostgresProperties.gridx = 1;
		gbc_txtPostgresProperties.gridy = 7;
		contentPane.add(txtPostgresProperties, gbc_txtPostgresProperties);
		txtPostgresProperties.setColumns(10);
		
		JLabel lblTableNameSuffix = new JLabel("Table name suffix");
		GridBagConstraints gbc_lblTableNameSuffix = new GridBagConstraints();
		gbc_lblTableNameSuffix.anchor = GridBagConstraints.WEST;
		gbc_lblTableNameSuffix.insets = new Insets(0, 0, 5, 5);
		gbc_lblTableNameSuffix.gridx = 0;
		gbc_lblTableNameSuffix.gridy = 8;
		contentPane.add(lblTableNameSuffix, gbc_lblTableNameSuffix);
		
		txttest = new JTextField();
		txttest.setText("_test");
		GridBagConstraints gbc_txttest = new GridBagConstraints();
		gbc_txttest.anchor = GridBagConstraints.NORTH;
		gbc_txttest.fill = GridBagConstraints.HORIZONTAL;
		gbc_txttest.insets = new Insets(0, 0, 5, 0);
		gbc_txttest.gridx = 1;
		gbc_txttest.gridy = 8;
		contentPane.add(txttest, gbc_txttest);
		txttest.setColumns(10);
		
		JButton btnSaveAsDefault = new JButton("Save as default setup");
		GridBagConstraints gbc_btnSaveAsDefault = new GridBagConstraints();
		gbc_btnSaveAsDefault.fill = GridBagConstraints.BOTH;
		gbc_btnSaveAsDefault.insets = new Insets(0, 0, 0, 5);
		gbc_btnSaveAsDefault.gridx = 0;
		gbc_btnSaveAsDefault.gridy = 9;
		contentPane.add(btnSaveAsDefault, gbc_btnSaveAsDefault);
		
		JButton btnStartEventsProcessing = new JButton("START EVENTS PROCESSING");
		GridBagConstraints gbc_btnStartEventsProcessing = new GridBagConstraints();
		gbc_btnStartEventsProcessing.anchor = GridBagConstraints.NORTH;
		gbc_btnStartEventsProcessing.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnStartEventsProcessing.gridx = 1;
		gbc_btnStartEventsProcessing.gridy = 9;
		contentPane.add(btnStartEventsProcessing, gbc_btnStartEventsProcessing);
	}

}
