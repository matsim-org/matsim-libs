/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationPTLinesEditor.java
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

package org.matsim.contrib.grips.evacuationptlineseditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.grips.jxmapviewerhelper.TileFactoryBuilder;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.VehicleWriterV1;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class EvacuationPTLinesEditor implements ActionListener{

	private static final Logger log = Logger.getLogger(EvacuationPTLinesEditor.class);

	private JFrame frame;
	private JPanel compositePanel;
	private MyMapViewer jMapViewer;
	private JButton saveButton;
	private JButton openBtn;
	private JTextField blockFieldLink1hh;
	private JTextField blockFieldLink1mm;



	//STRING COMMANDS
	public static final String RED = "LINK_SELECT_RED";
	public static final String GREEN = "LINK_SELECT_GREEN";

	private Id currentLinkIdRed = null;
	private Id currentLinkIdGreen = null;


	private JPanel busStopConfigPanel;


	private JButton blockButtonOK;

	private final Map<Id,BusStop> busStops = new HashMap<Id,BusStop>();


	private Scenario sc;

	private GeoPosition networkCenter;

	private String configFile;

	private String scPath;

	private JRadioButton redLinkSelct;

	private JRadioButton greenLinkSelct;

	private JSpinner numDepSpinner;

	private JSpinner capSpinner;

	private JCheckBox circCheck;

	private JSpinner numVehSpinner;


	private BusStop currentBusStop;

	private JToggleButton osmButton;

	private final String wms;

	private final String layer;

	private Polygon areaPolygon;

	private void setBusStopEditorPanelEnabled(boolean toggle) {
		if (!toggle) {
			this.greenLinkSelct.setEnabled(toggle);
			this.redLinkSelct.setEnabled(toggle);
		}
		this.blockFieldLink1hh.setEnabled(toggle);
		this.blockFieldLink1mm.setEnabled(toggle);
		this.numDepSpinner.setEnabled(toggle);
		this.capSpinner.setEnabled(toggle);
		this.circCheck.setEnabled(toggle);
		this.blockButtonOK.setEnabled(toggle);
	}



	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		
		String wms = null;
		String layer = null;
		if (args.length == 4) {
			for (int i = 0; i < 4; i += 2) {
				if (args[i].equalsIgnoreCase("-wms")) {
					wms = args[i+1];
				}
				if (args[i].equalsIgnoreCase("-layer")) {
					layer = args[i+1];
				}
			}
			
		} else if (args.length != 0) {
			printUsage();
			System.exit(-1);
		}
		EventQueue.invokeLater(new Runner(wms,layer));
	}



	private static void printUsage() {
		System.out.println();
		System.out.println(EvacuationPTLinesEditor.class.getSimpleName());
		System.out.println("Starts the GRIPS public transport lines editor.");
		System.out.println();
		System.out.println("usage 1: " + EvacuationPTLinesEditor.class.getSimpleName() +"\n" +
						   "         starts the editor and uses openstreetmap as backround layer\n" +
						   "         requires a working internet connection");
		System.out.println("usage 2: " + EvacuationPTLinesEditor.class.getSimpleName() + " -wms <url> -layer <layer name>\n" +
				           "         starts the editor and uses the given wms server to load a backgorund layer");
		
	}



	/**
	 * Create the application.
	 * @param layer 
	 * @param wms 
	 * @param config 
	 */
	public EvacuationPTLinesEditor(String wms, String layer){
		this.wms = wms;
		this.layer = layer;
		initialize();

	}

	private void loadMapView() {

		if (this.wms == null) {
			addMapViewer(TileFactoryBuilder.getOsmTileFactory());
		} else {
			addMapViewer(TileFactoryBuilder.getWMSTileFactory(this.wms, this.layer));
		}
		this.jMapViewer.setCenterPosition(getNetworkCenter());
		this.jMapViewer.setZoom(2);
		this.compositePanel.repaint();
	}

	private GeoPosition getNetworkCenter() {
		if (this.networkCenter != null) {
			return this.networkCenter;
		}
		Envelope e = new Envelope();
		for (Node node : this.sc.getNetwork().getNodes().values()) {
			e.expandToInclude(MGC.coord2Coordinate(node.getCoord()));
		}
		Coord centerC = new CoordImpl((e.getMaxX()+e.getMinX())/2, (e.getMaxY()+e.getMinY())/2);
		CoordinateTransformation ct2 =  new GeotoolsTransformation(this.sc.getConfig().global().getCoordinateSystem(),"EPSG:4326");
		centerC = ct2.transform(centerC);
		this.networkCenter = new GeoPosition(centerC.getY(),centerC.getX());

		return this.networkCenter;
	}



	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize()
	{
		this.frame = new JFrame();
		this.frame.setSize(800, 640);
		this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.frame.getContentPane().setLayout(new BorderLayout(0, 0));
		this.frame.setResizable(true);

		JPanel panel = new JPanel();
		this.frame.getContentPane().add(panel, BorderLayout.SOUTH);

		this.busStopConfigPanel = new JPanel(new GridLayout(18, 2));



		this.blockFieldLink1hh = new JTextField("--");
		this.blockFieldLink1mm = new JTextField("--");
		this.blockButtonOK = new JButton("ok");

		this.busStopConfigPanel.setSize(new Dimension(200, 200));

		//add hour / minute input check listeners
		this.blockFieldLink1hh.addKeyListener(new TypeHour());
		this.blockFieldLink1mm.addKeyListener(new TypeMinute());

		this.blockFieldLink1hh.addFocusListener(new CheckHour());
		this.blockFieldLink1mm.addFocusListener(new CheckMinute());


		this.blockButtonOK.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				EvacuationPTLinesEditor.this.setBusStopEditorPanelEnabled(false);
				EvacuationPTLinesEditor.this.numVehSpinner.setEnabled(false);
				EvacuationPTLinesEditor.this.currentBusStop.hh = EvacuationPTLinesEditor.this.blockFieldLink1hh.getText();
				EvacuationPTLinesEditor.this.currentBusStop.mm = EvacuationPTLinesEditor.this.blockFieldLink1mm.getText();
				EvacuationPTLinesEditor.this.currentBusStop.numDepSpinnerValue = EvacuationPTLinesEditor.this.numDepSpinner.getValue();
				EvacuationPTLinesEditor.this.currentBusStop.capSpinnerValue = EvacuationPTLinesEditor.this.capSpinner.getValue();
				EvacuationPTLinesEditor.this.currentBusStop.circCheckSelected = EvacuationPTLinesEditor.this.circCheck.isSelected();
				EvacuationPTLinesEditor.this.currentBusStop.numVehSpinnerValue = EvacuationPTLinesEditor.this.numVehSpinner.getValue();
				EvacuationPTLinesEditor.this.currentBusStop.id = EvacuationPTLinesEditor.this.redLinkSelct.isSelected() ? EvacuationPTLinesEditor.this.currentLinkIdRed : EvacuationPTLinesEditor.this.currentLinkIdGreen;
				EvacuationPTLinesEditor.this.busStops.put(EvacuationPTLinesEditor.this.currentBusStop.id, EvacuationPTLinesEditor.this.currentBusStop);
				EvacuationPTLinesEditor.this.jMapViewer.addBusStop(EvacuationPTLinesEditor.this.currentBusStop.id);

			}
		});


		JPanel panelLink1 = new JPanel(new GridLayout(1, 3));


		this.redLinkSelct = new JRadioButton();
		this.redLinkSelct.setActionCommand(RED);
		this.redLinkSelct.setSelected(true);
		this.redLinkSelct.addActionListener(this);
		this.redLinkSelct.setEnabled(false);

		this.greenLinkSelct = new JRadioButton();
		this.greenLinkSelct.setActionCommand(GREEN);
		this.greenLinkSelct.setSelected(false);
		this.greenLinkSelct.addActionListener(this);
		this.greenLinkSelct.setEnabled(false);

		ButtonGroup group = new ButtonGroup();
		group.add(this.greenLinkSelct);
		group.add(this.redLinkSelct);


		JPanel redPanel = new JPanel();
		redPanel.setBackground(Color.RED);
		redPanel.add(this.redLinkSelct);

		JPanel greenPanel = new JPanel();
		greenPanel.setBackground(Color.GREEN);
		greenPanel.add(this.greenLinkSelct);



		panelLink1.add(new JLabel("direction"));
		panelLink1.add(greenPanel);
		panelLink1.add(redPanel);


		this.busStopConfigPanel.add(panelLink1);
		this.busStopConfigPanel.add(new JSeparator());

		this.busStopConfigPanel.add(new JLabel("first departure"));
		JPanel depPanel = new JPanel(new GridLayout(1,3));
		depPanel.add(new JLabel("hh:mm"));
		depPanel.add(this.blockFieldLink1hh);
		depPanel.add(this.blockFieldLink1mm);
		this.busStopConfigPanel.add(depPanel);
		this.busStopConfigPanel.add(new JSeparator());

		JPanel numDepPanel = new JPanel(new GridLayout(1,2));
		numDepPanel.add(new JLabel("#departures"));
		SpinnerNumberModel spm1 = new SpinnerNumberModel(0, 0, 100, 1);
		this.numDepSpinner = new JSpinner(spm1);
		numDepPanel.add(this.numDepSpinner);
		this.numDepSpinner.setEnabled(false);
		this.busStopConfigPanel.add(numDepPanel);
		this.busStopConfigPanel.add(new JSeparator());


		JPanel capPanel = new JPanel(new GridLayout(1,2));
		capPanel.add(new JLabel("capacity/vehicle"));
		SpinnerNumberModel spm2 = new SpinnerNumberModel(0, 0, 100, 1);
		this.capSpinner = new JSpinner(spm2);
		capPanel.add(this.capSpinner);
		this.capSpinner.setEnabled(false);
		this.busStopConfigPanel.add(capPanel);
		this.busStopConfigPanel.add(new JSeparator());

		JPanel circPanel = new JPanel(new GridLayout(1,2));
		circPanel.add(new JLabel("circling"));
		this.circCheck = new JCheckBox();
		circPanel.add(this.circCheck);
		this.circCheck.setEnabled(false);
		this.busStopConfigPanel.add(circPanel);

		this.circCheck.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (EvacuationPTLinesEditor.this.circCheck.isSelected()) {
					EvacuationPTLinesEditor.this.numVehSpinner.setEnabled(true);
				} else {
					EvacuationPTLinesEditor.this.numVehSpinner.setEnabled(false);
				}
			}
		});

		JPanel numVehPanel = new JPanel(new GridLayout(1,2));
		numVehPanel.add(new JLabel("#vehicles"));
		SpinnerNumberModel spm3 = new SpinnerNumberModel(0, 0, 100, 1);
		this.numVehSpinner = new JSpinner(spm3);
		numVehPanel.add(this.numVehSpinner);
		this.numVehSpinner.setEnabled(false);
		this.busStopConfigPanel.add(numVehPanel);
		
		//circling is disabled for now
		circPanel.setVisible(false);
		numVehPanel.setVisible(false);
		
		
		this.busStopConfigPanel.add(new JSeparator());


		this.busStopConfigPanel.add(this.blockButtonOK);		



		this.busStopConfigPanel.setPreferredSize(new Dimension(300,300));

		this.busStopConfigPanel.setBorder(BorderFactory.createLineBorder(Color.black));



		this.blockFieldLink1hh.setEnabled(false); this.blockFieldLink1mm.setEnabled(false);

		this.blockButtonOK.setEnabled(false);

		this.frame.getContentPane().add(this.busStopConfigPanel, BorderLayout.EAST);
		
		

		this.openBtn = new JButton("Open");
		panel.add(this.openBtn);


		this.saveButton = new JButton("Save");
		this.saveButton.setEnabled(false);
		this.saveButton.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(this.saveButton);
		
		this.compositePanel = new JPanel();
		this.compositePanel.setBounds(new Rectangle(0, 0, 800, 800));
		this.frame.getContentPane().add(this.compositePanel, BorderLayout.CENTER);
		this.compositePanel.setLayout(new BorderLayout(0, 0));

		this.openBtn.addActionListener(this);
		this.saveButton.addActionListener(this);
		
		this.frame.setLocationRelativeTo(null);

		this.frame.addComponentListener(new ComponentListener() 
		{  
			@Override
			public void componentResized(ComponentEvent evt)
			{
				Component src = (Component)evt.getSource();
				Dimension newSize = src.getSize();
				updateMapViewerSize(newSize.width-200, newSize.height);
			}
			@Override
			public void componentMoved(ComponentEvent e) {}
			@Override
			public void componentShown(ComponentEvent e) {}
			@Override
			public void componentHidden(ComponentEvent e) {}
		});

	}

	public void updateMapViewerSize(int width, int height)
	{
		if (this.jMapViewer!=null)
			this.jMapViewer.setBounds(0, 0, width, height);
	}

	public void addMapViewer(TileFactory tf) {
		this.compositePanel.setLayout(null);
		this.jMapViewer = new MyMapViewer(this);
		this.jMapViewer.setBounds(0, 0, 800, 800);
		this.jMapViewer.setTileFactory(tf);
		this.jMapViewer.setPanEnabled(true);
		this.jMapViewer.setZoomEnabled(true);
		this.compositePanel.add(this.jMapViewer);
	}	


	/**
	 * save and open events
	 * 
	 */
	@Override
	public void actionPerformed(ActionEvent e){
		if (e.getActionCommand() == "LINK_SELECT_RED") {
			BusStop bs = this.busStops.get(this.currentLinkIdRed);
			if (bs!=null) {
				this.currentBusStop = bs;
			} else {
				bs = new BusStop();
				this.busStops.put(this.currentLinkIdRed, bs);
			}
			updateControlPanel(bs);
		} else if (e.getActionCommand() == "LINK_SELECT_GREEN") {
			BusStop bs = this.busStops.get(this.currentLinkIdGreen);
			if (bs!=null) {
				this.currentBusStop = bs;
			} else {
				bs = new BusStop();
				this.busStops.put(this.currentLinkIdGreen, bs);
			}
			updateControlPanel(bs);
		}else if (e.getActionCommand() == "Save") {
			createAndSavePTLines();
		}else if (e.getActionCommand() == "Open") {
			final JFileChooser fc = new JFileChooser();
			fc.setFileFilter(new FileFilter() {

				@Override
				public String getDescription() {
					return "MATSim config file";
				}

				@Override
				public boolean accept(File f) {
					if (f.isDirectory()) {
						return true;
					}
					if (f.getName().endsWith("xml")){
						return true;
					}
					return false;
				}
			});
			int returnVal = fc.showOpenDialog(this.frame);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				this.openBtn.setEnabled(false);
				this.saveButton.setEnabled(true);
				File file = fc.getSelectedFile();
				log.info("Opening: " + file.getAbsolutePath() + ".");
				this.configFile = file.getAbsolutePath();
				this.scPath = file.getParent();
				Config c = ConfigUtils.loadConfig(this.configFile);
				
				this.sc = ScenarioUtils.loadScenario(c);
				c.scenario().setUseTransit(true);
				c.scenario().setUseVehicles(true);
				
				String shp = this.sc.getConfig().getModule("grips").getValue("evacuationAreaFile");			
				readShapeFile(shp);

				
				loadMapView();
			} else {
				log.info("Open command cancelled by user.");
			}
		}

	}

	private void createAndSavePTLines() {
		
		
		
		
		Config config = this.sc.getConfig();
		
		//settings to activate pt simulation
		config.strategy().addParam("maxAgentPlanMemorySize", "3");
		config.strategy().addParam("Module_1", "ReRoute");
		config.strategy().addParam("ModuleProbability_1", "0.1");
		config.strategy().addParam("Module_2", "ChangeExpBeta");
		config.strategy().addParam("ModuleProbability_2", "0.8");
		config.strategy().addParam("Module_3", "TransitChangeLegMode");
		config.strategy().addParam("ModuleProbability_3", "0.4");
		config.strategy().addParam("ModuleDisableAfterIteration_3", "50");
//		config.strategy().addParam("Module_4", "TransitTimeAllocationMutator");
//		config.strategy().addParam("ModuleProbability_4", "0.3");

		config.setParam("qsim", "startTime", "00:00:00");
		config.setParam("qsim", "endTime", "30:00:00");
		config.setParam("changeLegMode", "modes", "car,pt");
		config.setParam("changeLegMode", "ignoreCarAvailability", "false");
		
		config.setParam("transit", "transitScheduleFile", this.scPath+"/transitSchedule.xml");
		config.setParam("transit", "vehiclesFile", this.scPath+"/transitVehicles.xml");
		config.setParam("transit", "transitModes", "pt");

		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		
		new ConfigWriter(config).write(this.configFile);
		
		PTLinesGenerator gen = new PTLinesGenerator(this.sc,this.busStops);
		TransitSchedule schedule = gen.getTransitSchedule();
		
		new NetworkWriter(this.sc.getNetwork()).write(this.sc.getConfig().network().getInputFile());
		new TransitScheduleWriterV1(schedule).write(this.scPath+"/transitSchedule.xml");
		new VehicleWriterV1(((ScenarioImpl)this.sc).getVehicles()).writeFile(this.scPath+"/transitVehicles.xml");
		
	}



	public void setSaveButtonEnabled(boolean enabled) {
		this.saveButton.setEnabled(enabled);
	}

	/**
	 * set edit. basically dis-/enabling text fields
	 * and setting labels. 
	 * @param b
	 */
	public void setEditMode(boolean b){

		this.setBusStopEditorPanelEnabled(b);

	}

	/**
	 * set id #1 of the first selected link (in gui and object data)
	 * checks if there is any data for the link prior to this selection 
	 * 
	 * @param id
	 */	
	public void setLink1Id(Id id){
		this.currentLinkIdRed = id;
		if (id!=null){
			if(this.busStops.containsKey(id)){
				BusStop bs = this.busStops.get(id);
				this.currentBusStop = bs;
				updateControlPanel(bs);
			}
			else {
				
				this.currentBusStop = new BusStop();
				this.currentBusStop.id = id;
				this.busStops.put(id, this.currentBusStop);
				updateControlPanel(this.currentBusStop);
			}
			
			this.redLinkSelct.setSelected(true);
			this.greenLinkSelct.setSelected(false);
		}
		this.redLinkSelct.setEnabled(id != null);
	}

	private void updateControlPanel(BusStop bs) {
		this.currentBusStop = bs;
		this.blockFieldLink1hh.setText(bs.hh);
		this.blockFieldLink1mm.setText(bs.mm);
		this.numDepSpinner.setValue(bs.numDepSpinnerValue);
		this.capSpinner.setValue(bs.capSpinnerValue);
		this.numVehSpinner.setValue(bs.numVehSpinnerValue);
		this.circCheck.setSelected(bs.circCheckSelected);
		
	}



	/**
	 * set id #2 of the second selected link (in gui and object data)
	 * checks if there is any data for the link prior to this selection 
	 * 
	 * @param id
	 */
	public void setLink2Id(Id id) {
		this.currentLinkIdGreen = id;
		this.greenLinkSelct.setEnabled(id != null);
	}



	class TypeHour implements KeyListener 
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

			JTextField src = (JTextField)e.getSource();

			String text = src.getText();


			if (!text.matches("([01]?[0-9]|2[0-3])"))
				src.setText("00");

		}
		@Override
		public void keyPressed(KeyEvent e) {
			// TODO Auto-generated method stub

		}
	}


	class CheckHour implements FocusListener
	{

		@Override
		public void focusGained(FocusEvent e)
		{
			JTextField src = (JTextField)e.getSource();
			src.setSelectionStart(0);
			src.setSelectionEnd(src.getText().length());			

		}

		@Override
		public void focusLost(FocusEvent e)
		{
			JTextField src = (JTextField)e.getSource();
			String text = src.getText();

			if (!text.matches("([01]?[0-9]|2[0-3])"))
				src.setText("00");
			else if (text.matches("[0-9]"))
				src.setText("0"+text);

		}

	}


	class CheckMinute implements FocusListener
	{

		@Override
		public void focusGained(FocusEvent e)
		{
			JTextField src = (JTextField)e.getSource();
			src.setSelectionStart(0);
			src.setSelectionEnd(src.getText().length());			

		}

		@Override
		public void focusLost(FocusEvent e)
		{
			JTextField src = (JTextField)e.getSource();

			String text = src.getText();

			if ((!text.matches("[0-5][0-9]")) && (!text.matches("[0-9]")))
				src.setText("00");
			else if (text.matches("[0-9]"))
				src.setText("0" + text);



		}

	}


	class TypeMinute implements KeyListener 
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


		}
		@Override
		public void keyPressed(KeyEvent e) {
			// TODO Auto-generated method stub

		}
	}



	final static class BusStop {
		Id id;
		protected String hh = "--";
		protected String mm = "--";
		protected Object numDepSpinnerValue = new Integer(0);
		protected Object numVehSpinnerValue = new Integer(0);
		protected boolean circCheckSelected = false;
		protected Object capSpinnerValue = new Integer(0);


		@Override
		public String toString() {
			return this.id + " " + this.hh + " " + this.mm;
		}

	}

	private static final class Runner implements Runnable{


		private final String wms;
		private final String layer;

		public Runner(String wms, String layer) {
			this.wms = wms;
			this.layer = layer;
		}

		@Override
		public void run() {
			try {
				EvacuationPTLinesEditor window = new EvacuationPTLinesEditor(this.wms,this.layer);
				window.frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}


	public Scenario getScenario() {
		return this.sc;
	}



	public Polygon getAreaPolygon()
	{
		return areaPolygon;
	}
	
	public void readShapeFile(String shapeFileString)
	{
			ShapeFileReader shapeFileReader = new ShapeFileReader();
			shapeFileReader.readFileAndInitialize(shapeFileString);
	
			ArrayList<Geometry> geometries = new ArrayList<Geometry>();
			for (SimpleFeature ft : shapeFileReader.getFeatureSet())
			{
				Geometry geo = (Geometry) ft.getDefaultGeometry();
				//System.out.println(ft.getFeatureType());
				geometries.add(geo);
			}
			
			int j = 0;			
			Coordinate [] coords = geometries.get(0).getCoordinates();
			coords[coords.length-1] = coords[0];
			
			GeometryFactory geofac = new GeometryFactory();
			
			LinearRing shell = geofac.createLinearRing(coords);
			areaPolygon = geofac.createPolygon(shell, null);		
			
	}	

}