/* *********************************************************************** *
 * project: org.matsim.*
 * RoadClosuresEditor.java
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

package playground.wdoering.grips.evacuationanalysis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.grips.jxmapviewerhelper.TileFactoryBuilder;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactory;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.Time;

import playground.gregor.sim2d_v3.events.XYVxVyEventsFileReader;
import playground.wdoering.debugvisualization.controller.XYVxVyEventThread;
import playground.wdoering.grips.evacuationanalysis.EvacuationAnalysis.Mode;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;



public class EvacuationAnalysis implements ActionListener{

	public static enum Mode { EVACUATION, UTILIZATION, CLEARING };
	
	private static final Logger log = Logger.getLogger(EvacuationAnalysis.class);
	private JFrame frame;
	private JPanel compositePanel;
	private MyMapViewer jMapViewer;
	private JButton saveButton;
	private JButton openBtn;
	private JPanel blockPanel;
	private JPanel panelDescriptions;
	private Scenario sc;
	private GeoPosition networkCenter;
	private String configFile;
	private String scPath;
	private final String wms;
	private final String layer;
	private Polygon areaPolygon;
	private File currentEventFile;
	private Thread readerThread;
	private double cellSize = 100;
	private QuadTree<Cell> cellTree;
	private EventHandler eventHandler;
	private AbstractGraphPanel graphPanel;
	private JPanel controlPanel;
	private JButton calcButton;
	private ArrayList<File> eventFiles;
	private JComboBox iterationsList;
	private JTextField gridSizeField;
	private JComboBox modeList;
	private JSlider transparencySlider;
	private float cellTransparency;
	private String itersOutputDir;
	private boolean firstLoad;
	private Mode mode = Mode.EVACUATION;
	
	

	/**
	 * Launch the application.
	 * 
	 * Check for parameters such as wms / layer
	 * 
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

		} else if (args.length != 0)
		{
			printUsage();
			System.exit(-1);
		}

		EventQueue.invokeLater(new Runner(wms,layer));
	}

	private static void printUsage()
	{
		System.out.println();
		System.out.println(EvacuationAnalysis.class.getSimpleName());
		System.out.println("Starts the GRIPS evacuation analysis.");
		System.out.println();
		System.out.println("usage 1: " + EvacuationAnalysis.class.getSimpleName() +"\n" +
				"         starts the editor and uses openstreetmap as backround layer\n" +
				"         requires a working internet connection");
		System.out.println("usage 2: " + EvacuationAnalysis.class.getSimpleName() + " -wms <url> -layer <layer name>\n" +
				"         starts the editor and uses the given wms server to load a backgorund layer");

	}


	/**
	 * Create the application.
	 * @param layer 
	 * @param wms 
	 * @param config 
	 */
	public EvacuationAnalysis(String wms, String layer)
	{
		this.wms = wms;
		this.layer = layer;

		initialize();

	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize()
	{
		this.firstLoad = true;
		this.cellTransparency = .5f;
		
		//////////////////////////////////////////////////////////////////////////////
		//basic frame settings
		//////////////////////////////////////////////////////////////////////////////
		
		this.frame = new JFrame();
		this.frame.setSize(960, 768);
		this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.frame.getContentPane().setLayout(new BorderLayout(0, 0));
		this.frame.setResizable(true);

		//the panel on the right hand side, to display graphs etc.
		JPanel panel = new JPanel();
		this.frame.getContentPane().add(panel, BorderLayout.SOUTH);
		this.blockPanel = new JPanel();
		this.blockPanel.setLayout(new BoxLayout(this.blockPanel, BoxLayout.Y_AXIS));
		this.blockPanel.setSize(new Dimension(400, 368));


		//////////////////////////////////////////////////////////////////////////////
		// DESCRIPTIONS
		//////////////////////////////////////////////////////////////////////////////
		
//		this.panelDescriptions = new JPanel(new GridLayout(1, 3));
//		this.panelDescriptions.add(new JLabel("graph"));

		
		//////////////////////////////////////////////////////////////////////////////
		// PANELS
		//////////////////////////////////////////////////////////////////////////////
		
		this.graphPanel = new EvacuationTimeGraphPanel(360,280);
		this.controlPanel = new JPanel(new GridLayout(7, 3));
		this.controlPanel.setPreferredSize(new Dimension(360,30));
		this.controlPanel.setSize(new Dimension(360,30));

		//FIXME: this part is meant to display the graph in another window
//		JFrame graphFrame = new JFrame();
//		graphFrame.setPreferredSize(new Dimension(1000, 1000));
//		graphFrame.setSize(1000, 1000);
//		graphFrame.setLocationRelativeTo(null);
//		graphFrame.add(graphPanel);
//		graphFrame.setVisible(true);
//		graphFrame.validate();
		
//		this.blockPanel.add(this.panelDescriptions);
		this.blockPanel.add(graphPanel);
		this.blockPanel.add(controlPanel);
		this.blockPanel.setPreferredSize(new Dimension(360,700));
		this.blockPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		
		this.compositePanel = new JPanel();
		this.compositePanel.setBounds(new Rectangle(0, 0, 800, 800));
		this.compositePanel.setLayout(new BorderLayout(0, 0));
		
		//////////////////////////////////////////////////////////////////////////////
		// CONTROL
		//////////////////////////////////////////////////////////////////////////////

		
		this.frame.getContentPane().add(this.blockPanel, BorderLayout.EAST);
		this.frame.getContentPane().add(this.compositePanel, BorderLayout.CENTER);

		this.openBtn = new JButton("Open");
		this.saveButton = new JButton("Save");
		this.saveButton.setEnabled(false);
		this.saveButton.setHorizontalAlignment(SwingConstants.RIGHT);
		this.calcButton = new JButton("calculate");
		this.calcButton.setEnabled(false);
		this.calcButton.addActionListener(this);
		this.calcButton.setPreferredSize(new Dimension(100,20));
		this.calcButton.setSize(new Dimension(100,24));
		
		JPanel iterationSelectionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		iterationSelectionPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		this.iterationsList = new JComboBox();
		this.iterationsList.addActionListener(this);
		this.iterationsList.setPreferredSize(new Dimension(220,24));
		iterationSelectionPanel.add(new JLabel(" event file: ", SwingConstants.RIGHT));
		iterationSelectionPanel.add(this.iterationsList);
		
		JPanel gridSizeSelectionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		gridSizeSelectionPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		this.gridSizeField = new JTextField(""+cellSize);
		this.gridSizeField.setActionCommand("changeIteration");
		this.gridSizeField.addActionListener(this);
		this.gridSizeField.addKeyListener(new TypeNumber());
		this.gridSizeField.setPreferredSize(new Dimension(220,24));
		gridSizeSelectionPanel.add(new JLabel(" grid size: ", SwingConstants.RIGHT));
		gridSizeSelectionPanel.add(this.gridSizeField);
		
		JPanel modeSelectionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		modeSelectionPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		this.modeList = new JComboBox(new String[]{"evacuation time", "clearing time", "link utilization"});
		this.modeList.setActionCommand("changeMode");
		this.modeList.addActionListener(this);
		this.modeList.setPreferredSize(new Dimension(220,24));
		modeSelectionPanel.add(new JLabel(" mode: ", SwingConstants.RIGHT));
		modeSelectionPanel.add(this.modeList);
		
		JPanel calculateButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); 
		calculateButtonPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 7));
		calculateButtonPanel.add(new JLabel(""));
		calculateButtonPanel.add(calcButton);
		calculateButtonPanel.setPreferredSize(new Dimension(220,40));
		
		JPanel transparencySliderPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		transparencySliderPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		transparencySlider = new JSlider(JSlider.HORIZONTAL, 1, 100, 50);
		transparencySlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				updateTransparency((float)(((JSlider)e.getSource()).getValue())/100f);
			}
		});
		transparencySlider.setPreferredSize(new Dimension(220,24));
		transparencySliderPanel.add(new JLabel(" cell transparency: ", SwingConstants.RIGHT));
		transparencySliderPanel.add(transparencySlider);
		
		this.controlPanel.add(new JLabel(""));
		this.controlPanel.add(iterationSelectionPanel);
		this.controlPanel.add(gridSizeSelectionPanel);
		this.controlPanel.add(modeSelectionPanel);
		this.controlPanel.add(calculateButtonPanel);
		this.controlPanel.add(new JSeparator());
		this.controlPanel.add(transparencySliderPanel);
		panel.add(this.openBtn);
		panel.add(this.saveButton);
		
		this.openBtn.addActionListener(this);
		this.saveButton.addActionListener(this);

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
		
		this.frame.setLocationRelativeTo(null);

	}
	
	public void updateTransparency(float transparency)
	{
		this.cellTransparency = transparency;
		if (this.jMapViewer!=null)
			this.jMapViewer.setCellTransparency(this.cellTransparency);
	}

	/**
	 * save, open and (re)calculate events
	 * 
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		/**
		 * Save heatmap shape and graphs
		 * 
		 */
		if (e.getActionCommand() == "Save")
		{
			//TODO: save Shape file and Graphs
			
//			this.sc.getConfig().network().setTimeVariantNetwork(true);
//			String changeEventsFile = this.scPath + "/networkChangeEvents.xml";
//			this.sc.getConfig().network().setChangeEventInputFile(changeEventsFile);
//			new ConfigWriter(this.sc.getConfig()).write(this.configFile);
		}

		/**
		 * Open MATSim config file.
		 * 
		 */
		if (e.getActionCommand() == "Open")
		{
			final JFileChooser fc = new JFileChooser();
			fc.setCurrentDirectory(new File("C:/temp/!matsimfiles/fostercityca/output/"));
//			fc.setCurrentDirectory(new File("C:/temp/!matsimfiles/hh/demo/output/"));
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
			
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				//open file
				File file = fc.getSelectedFile();
				log.info("Opening: " + file.getAbsolutePath() + ".");
				this.configFile = file.getAbsolutePath();
				this.scPath = file.getParent();
				Config c = ConfigUtils.loadConfig(this.configFile);
				this.sc = ScenarioUtils.loadScenario(c);
				String shp = this.sc.getConfig().getModule("grips").getValue("evacuationAreaFile");
				
				//read the shape file
				readShapeFile(shp);
				
				//get events file, check if there is at least the very first iteration 
				this.itersOutputDir = this.sc.getConfig().getModule("controler").getValue("outputDirectory");
				
				//get all available events 
				eventFiles = getAvailableEventFiles(this.itersOutputDir);
				
				
				//check if empty
				if (eventFiles.isEmpty())
				{
					JOptionPane.showMessageDialog(this.frame, "Could not find any event files", "Event files unavailable", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				currentEventFile = eventFiles.get(0);
				
				iterationsList.removeAllItems();
				for (File eventFile : eventFiles)
				{
					String shortenedFileName = eventFile.getName();
					iterationsList.addItem(shortenedFileName);
				}
				
				//read events
				readEvents();
				
				//update buttons
				this.openBtn.setEnabled(false);
				this.saveButton.setEnabled(true);
				this.calcButton.setEnabled(true);
				
				this.firstLoad = false;
				
				
			} else {
				log.info("Open command cancelled by user.");
			}
		}
		
		if (e.getActionCommand() == "calculate")
		{
			readEvents();
			if (this.jMapViewer != null)
				this.jMapViewer.repaint();
		}

		if ((e.getActionCommand() == "changeIteration") && (!firstLoad)) 
		{
			File newFile = getEventPathFromName(""+iterationsList.getSelectedItem());
			if (newFile!=null)
				currentEventFile = newFile;
		}
		
		if (e.getActionCommand() == "changeMode")
		{
			if (modeList.getSelectedItem().toString().contains("evacuation"))
			{
				setMode(Mode.EVACUATION);
			}
			else if (modeList.getSelectedItem().toString().contains("utilization"))
			{
				setMode(Mode.UTILIZATION);
			}
			else if (modeList.getSelectedItem().toString().contains("clearing"))
			{
				setMode(Mode.CLEARING);
			}
				
		}
		
	}

	private void setMode(Mode mode)
	{
		this.mode = mode;
		
		if (jMapViewer!=null)
			jMapViewer.setMode(mode);
		
	}
	
	public Mode getMode() {
		return mode;
	}

	private File getEventPathFromName(String selectedItem)
	{
		for (File eventFile : eventFiles)
		{
			if (eventFile.getName().contains(selectedItem))
				return eventFile;
		}
		return null;
	}

	private void readEvents() {
		
		
		//run event reader
		runEventReader(currentEventFile);
		
		//initialize map viewer
		if (this.jMapViewer == null)
			loadMapView();
		
		//get data from eventhandler (if not null)
		if (eventHandler!=null)
		{
			//get data
			EventData data = eventHandler.getData();
			
			//update data in both the map viewer and the graphs
			jMapViewer.updateEventData(data);
			graphPanel.updateData(data);
		}
	}
	
	private ArrayList<File> getAvailableEventFiles(String dirString)
	{
		File dir = new File(dirString);
		Stack<File> directoriesToScan = new Stack<File>();
		ArrayList<File> files = new ArrayList<File>();
		
		directoriesToScan.add(dir);
		
		while (!directoriesToScan.isEmpty())
		{
			File currentDir = directoriesToScan.pop();
			File[] filesToCheck = currentDir.listFiles(new EventFileFilter());
			
			for (File currentFile : filesToCheck)
			{
				if (currentFile.isDirectory())
					directoriesToScan.push(currentFile);
				else
					files.add(currentFile);
			}
			
		}
		
		return files;
	}

	/**
	 * Initialize map viewer.
	 */
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
			
			//ignore end nodes
			if (node.getId().toString().contains("en")) continue;
			
			e.expandToInclude(MGC.coord2Coordinate(node.getCoord()));
		}
		Coord centerC = new CoordImpl((e.getMaxX()+e.getMinX())/2, (e.getMaxY()+e.getMinY())/2);
		CoordinateTransformation ct2 =  new GeotoolsTransformation(this.sc.getConfig().global().getCoordinateSystem(),"EPSG:4326");
		centerC = ct2.transform(centerC);
		this.networkCenter = new GeoPosition(centerC.getY(),centerC.getX());

		return this.networkCenter;
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


	private void runEventReader(File eventFile)
	{
		this.eventHandler = null;
		EventsManager e = EventsUtils.createEventsManager();
		XYVxVyEventsFileReader reader = new XYVxVyEventsFileReader(e);
		this.readerThread = new Thread(new EventReaderThread(reader,eventFile.toString()), "readerthread");
		this.eventHandler = new EventHandler(eventFile.getName(), this.sc, this.getGridSize(), this.readerThread);
		e.addHandler(this.eventHandler);
		this.readerThread.run();
	}

	/**
	 * check if file is availabe & readable
	 * 
	 * @param file
	 * @return true if available and readable
	 */
	private boolean exists(String file)
	{
		File f = new File(file);
		if(f.exists() && f.canRead())
			return true;
		return false;
	}

	public void setSaveButtonEnabled(boolean enabled)
	{
		this.saveButton.setEnabled(enabled);
	}
	
	public synchronized Link getRoadClosure(Id linkId)
	{
		return this.sc.getNetwork().getLinks().get(linkId);
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
	
	class EventFileFilter implements java.io.FileFilter
	{
		
		@Override
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			}
			if (f.getName().endsWith(".events.xml.gz")){
				return true;
			}
			return false;
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
	
	class TypeNumber implements KeyListener 
	{
		@Override
		public void keyTyped(KeyEvent e)
		{
			if (!Character.toString(e.getKeyChar()).matches("[.0-9]"))
				e.consume();
		}
		@Override
		public void keyReleased(KeyEvent e) {
			JTextField textField = ((JTextField)e.getSource());
			if (!(textField.getText().matches("^[0-9]{0,4}\\.?[0-9]{0,4}$")))
				textField.setText(""+getGridSize());
			else
			{
				if (textField.getText().length()>0)
					setGridSize(Double.parseDouble(textField.getText()));
			}
			
		}
		@Override
		public void keyPressed(KeyEvent e) {}
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
				EvacuationAnalysis window = new EvacuationAnalysis(this.wms,this.layer);
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
			for (Feature ft : shapeFileReader.getFeatureSet())
			{
				Geometry geo = ft.getDefaultGeometry();
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
	
	public void setGridSize(double gridSize)
	{
		this.cellSize = gridSize;
	}

	public double getGridSize()
	{
		return this.cellSize;
	}

	public float getCellTransparency()
	{
		return this.cellTransparency;
	}	
}
