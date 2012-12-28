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

package org.matsim.contrib.grips.roadclosureseditor;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.grips.jxmapviewerhelper.TileFactoryBuilder;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactory;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;



public class RoadClosuresEditor implements ActionListener{

	private static final Logger log = Logger.getLogger(RoadClosuresEditor.class);

	private JFrame frame;
	private JPanel compositePanel;
	private MyMapViewer jMapViewer;
	private JButton saveButton;
	private JButton openBtn;
	private JTextField blockFieldLink1hh;
	private JTextField blockFieldLink1mm;
	private JTextField blockFieldLink2hh;
	private JTextField blockFieldLink2mm;

	private final HashMap<Id, String> roadClosures;
	private Id currentLinkId1 = null;
	private Id currentLinkId2 = null;


	private JPanel blockPanel;


	private JButton blockButtonOK;

	private JCheckBox cbLink1;

	private JCheckBox cbLink2;


	private JPanel panelLink1;

	private JPanel panelLink2;

	private boolean saveLink1 = false;
	private boolean saveLink2 = false;

	private JPanel panelDescriptions;

	private Scenario sc;

	private GeoPosition networkCenter;

	private String configFile;

	private String scPath;

	private final String wms;

	private final String layer;

	private Polygon areaPolygon;

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
		System.out.println(RoadClosuresEditor.class.getSimpleName());
		System.out.println("Starts the GRIPS road closures editor.");
		System.out.println();
		System.out.println("usage 1: " + RoadClosuresEditor.class.getSimpleName() +"\n" +
				"         starts the editor and uses openstreetmap as backround layer\n" +
				"         requires a working internet connection");
		System.out.println("usage 2: " + RoadClosuresEditor.class.getSimpleName() + " -wms <url> -layer <layer name>\n" +
				"         starts the editor and uses the given wms server to load a backgorund layer");

	}


	/**
	 * Create the application.
	 * @param layer 
	 * @param wms 
	 * @param config 
	 */
	public RoadClosuresEditor(String wms, String layer){
		this.wms = wms;
		this.layer = layer;
		this.roadClosures = new HashMap<Id, String>();

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
		this.frame.setSize(960, 768);
		this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.frame.getContentPane().setLayout(new BorderLayout(0, 0));
		this.frame.setResizable(true);

		JPanel panel = new JPanel();
		this.frame.getContentPane().add(panel, BorderLayout.SOUTH);

		this.blockPanel = new JPanel(new GridLayout(18, 2));



		this.blockFieldLink1hh = new JTextField("--");
		this.blockFieldLink1mm = new JTextField("--");
		this.blockFieldLink2hh = new JTextField("--");
		this.blockFieldLink2mm = new JTextField("--");
		this.blockButtonOK = new JButton("ok");

		this.blockPanel.setSize(new Dimension(200, 200));

		//add hour / minute input check listeners
		this.blockFieldLink1hh.addKeyListener(new TypeHour());
		this.blockFieldLink1mm.addKeyListener(new TypeMinute());
		this.blockFieldLink2hh.addKeyListener(new TypeHour());
		this.blockFieldLink2mm.addKeyListener(new TypeMinute());
		this.blockFieldLink1hh.addFocusListener(new CheckHour());
		this.blockFieldLink1mm.addFocusListener(new CheckMinute());
		this.blockFieldLink2hh.addFocusListener(new CheckHour());
		this.blockFieldLink2mm.addFocusListener(new CheckMinute());

		this.cbLink1 = new JCheckBox("link 1");
		this.cbLink2 = new JCheckBox("link 2");

		this.blockButtonOK.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				updateRoadClosure();

			}
		});

		this.cbLink1.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				RoadClosuresEditor.this.saveLink1 = !RoadClosuresEditor.this.saveLink1;

				if (RoadClosuresEditor.this.saveLink1)
				{
					RoadClosuresEditor.this.blockFieldLink1hh.setEnabled(true);
					RoadClosuresEditor.this.blockFieldLink1mm.setEnabled(true);

				}
				else
				{
					RoadClosuresEditor.this.blockFieldLink1hh.setEnabled(false);
					RoadClosuresEditor.this.blockFieldLink1mm.setEnabled(false);
				}
			}
		});

		this.cbLink2.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				RoadClosuresEditor.this.saveLink2 = !RoadClosuresEditor.this.saveLink2;

				if (RoadClosuresEditor.this.saveLink2)
				{
					RoadClosuresEditor.this.blockFieldLink2hh.setEnabled(true);
					RoadClosuresEditor.this.blockFieldLink2mm.setEnabled(true);
				}
				else
				{
					RoadClosuresEditor.this.blockFieldLink2hh.setEnabled(false);
					RoadClosuresEditor.this.blockFieldLink2mm.setEnabled(false);
				}



			}
		});

		this.cbLink1.setBackground(Color.red);
		this.cbLink2.setBackground(Color.green);

		this.cbLink1.setSelected(false);
		this.cbLink2.setSelected(false);


		this.panelDescriptions = new JPanel(new GridLayout(1, 3));
		this.panelLink1 = new JPanel(new GridLayout(1, 3));
		this.panelLink2 = new JPanel(new GridLayout(1, 3));

		this.panelDescriptions.add(new JLabel("Road ID"));
		this.panelDescriptions.add(new JLabel("HH"));
		this.panelDescriptions.add(new JLabel("MM"));

		this.panelLink1.add(this.cbLink1);
		this.panelLink1.add(this.blockFieldLink1hh);
		this.panelLink1.add(this.blockFieldLink1mm);
		this.panelLink1.setBackground(Color.red);

		this.panelLink2.add(this.cbLink2);
		this.panelLink2.add(this.blockFieldLink2hh);
		this.panelLink2.add(this.blockFieldLink2mm);
		this.panelLink2.setBackground(Color.green);

		this.blockPanel.add(this.panelDescriptions);
		this.blockPanel.add(this.panelLink1);
		this.blockPanel.add(this.panelLink2);
		this.blockPanel.add(this.blockButtonOK);		

		this.blockPanel.setPreferredSize(new Dimension(300,300));

		this.blockPanel.setBorder(BorderFactory.createLineBorder(Color.black));


		this.cbLink1.setEnabled(false); this.cbLink2.setEnabled(false);

		this.blockFieldLink1hh.setEnabled(false); this.blockFieldLink1mm.setEnabled(false);
		this.blockFieldLink2hh.setEnabled(false); this.blockFieldLink2mm.setEnabled(false);

		this.blockButtonOK.setEnabled(false);

		//		this.blockFieldLink1hh.setSelectedTextColor(Color.red);
		//		this.blockFieldLink1mm.setSelectedTextColor(Color.green);

		this.frame.getContentPane().add(this.blockPanel, BorderLayout.EAST);

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
	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand() == "Save") {
			this.sc.getConfig().network().setTimeVariantNetwork(true);
			String changeEventsFile = this.scPath + "/networkChangeEvents.xml";
			this.sc.getConfig().network().setChangeEventInputFile(changeEventsFile);
			new ConfigWriter(this.sc.getConfig()).write(this.configFile);
			saveRoadClosures(changeEventsFile, this.roadClosures);
		}

		if (e.getActionCommand() == "Open") {
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
				String shp = this.sc.getConfig().getModule("grips").getValue("evacuationAreaFile");			
				readShapeFile(shp);
				loadMapView();
			} else {
				log.info("Open command cancelled by user.");
			}
		}

	}


	private synchronized void saveRoadClosures(String fileName, HashMap<Id, String> roadClosures)
	{
		if (roadClosures.size()>0)
		{


			//create change event
			Collection<NetworkChangeEvent> evs = new ArrayList<NetworkChangeEvent>();
			NetworkChangeEventFactory fac = new NetworkChangeEventFactoryImpl();

			Iterator it = roadClosures.entrySet().iterator();
			while (it.hasNext())
			{
				Map.Entry pairs = (Map.Entry)it.next();

				Id currentId = (Id)pairs.getKey();
				String timeString = (String)pairs.getValue();

				try {
					double time = Time.parseTime(timeString);
					NetworkChangeEvent ev = fac.createNetworkChangeEvent(time);
//					ev.setFreespeedChange(new ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 0));
					ev.setFlowCapacityChange(new ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 0));
					
					ev.addLink(this.sc.getNetwork().getLinks().get(currentId));
					evs.add(ev);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}


			NetworkChangeEventsWriter writer = new NetworkChangeEventsWriter();
			if (fileName.endsWith(".xml")) {
				writer.write(fileName, evs);
			} else {
				writer.write(fileName + ".xml", evs);		
			}
			
			System.out.println("saved road closures");
		}


	}


	public void setSaveButtonEnabled(boolean enabled) {
		this.saveButton.setEnabled(enabled);
	}

	/**
	 * set edit. basically dis-/enabling text fields
	 * and setting labels. 
	 * @param b
	 */
	public void setEditMode(boolean b)
	{
		if (b)
		{
			this.cbLink1.setEnabled(true);
			this.cbLink2.setEnabled(true);
			this.blockButtonOK.setEnabled(true);
		}
		else
		{
			this.cbLink1.setEnabled(false);
			this.cbLink2.setEnabled(false);
			this.blockButtonOK.setEnabled(false);

			this.blockFieldLink1hh.setText("--");
			this.blockFieldLink1mm.setText("--");
			this.blockFieldLink1hh.setEnabled(false);
			this.blockFieldLink1mm.setEnabled(false);

			this.blockFieldLink2hh.setText("--");
			this.blockFieldLink2mm.setText("--");
			this.blockFieldLink2hh.setEnabled(false);
			this.blockFieldLink2mm.setEnabled(false);

			this.cbLink1.setSelected(false);
			this.cbLink2.setSelected(false);
			this.cbLink1.setText("-");
			this.cbLink2.setText("-");
			this.saveLink1 = false;
			this.saveLink2 = false;


		}


	}

	/**
	 * set id #1 of the first selected link (in gui and object data)
	 * checks if there is any data for the link prior to this selection 
	 * 
	 * @param id
	 */	
	public void setLink1Id(Id id)
	{
		if (id!=null)
		{
			this.cbLink1.setText(id.toString());
			this.currentLinkId1 = id;

			if(this.roadClosures.containsKey(id))	
			{
				this.cbLink1.setSelected(true);
				this.blockFieldLink1hh.setEnabled(true); this.blockFieldLink1mm.setEnabled(true);

				this.blockFieldLink1hh.setText(this.roadClosures.get(id).substring(0,2));
				this.blockFieldLink1mm.setText(this.roadClosures.get(id).substring(3,5));

				this.saveLink1 = true;
			}
		}
		else
		{
			this.blockFieldLink1hh.setText("--"); this.blockFieldLink1mm.setText("--");
			this.blockFieldLink1hh.setEnabled(false); this.blockFieldLink2hh.setEnabled(false);
			this.cbLink1.setEnabled(false);

		}

	}

	/**
	 * set id #2 of the second selected link (in gui and object data)
	 * checks if there is any data for the link prior to this selection 
	 * 
	 * @param id
	 */
	public void setLink2Id(Id id)
	{
		if (id!=null)
		{
			this.cbLink2.setText(id.toString());
			this.currentLinkId2 = id;

			if(this.roadClosures.containsKey(id))	
			{
				this.cbLink2.setSelected(true);
				this.blockFieldLink2hh.setEnabled(true); this.blockFieldLink2mm.setEnabled(true);

				this.blockFieldLink2hh.setText(this.roadClosures.get(id).substring(0,2));
				this.blockFieldLink2mm.setText(this.roadClosures.get(id).substring(3,5));

				this.saveLink2 = true;
			}
		}
		else
		{
			this.blockFieldLink2hh.setText("--"); this.blockFieldLink2mm.setText("--");
			this.blockFieldLink2hh.setEnabled(false); this.blockFieldLink2mm.setEnabled(false);
			this.cbLink2.setEnabled(false);
		}

	}

	public void updateRoadClosure()
	{
		if ((this.currentLinkId1!=null))
		{
			if (this.cbLink1.isSelected())
				this.roadClosures.put(this.currentLinkId1, this.blockFieldLink1hh.getText() + ":" + this.blockFieldLink1mm.getText());
			else
				this.roadClosures.remove(this.currentLinkId1);

		}

		if ((this.currentLinkId2!=null))
		{
			if (this.cbLink2.isSelected())
				this.roadClosures.put(this.currentLinkId2, this.blockFieldLink2hh.getText() + ":" + this.blockFieldLink2mm.getText());
			else
				this.roadClosures.remove(this.currentLinkId2);
		}

		this.saveButton.setEnabled(this.roadClosures.size() > 0);

	}

	public synchronized HashMap<Id, String> getRoadClosures()
	{
		return this.roadClosures;
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


	public boolean hasLink(Id id)
	{


		if (this.roadClosures.containsKey(id))
			return true;
		else
			return false;
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
				RoadClosuresEditor window = new RoadClosuresEditor(this.wms,this.layer);
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
