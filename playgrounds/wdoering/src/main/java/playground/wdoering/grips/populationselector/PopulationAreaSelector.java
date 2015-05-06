/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationSelector.java
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

package playground.wdoering.grips.populationselector;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;

import org.apache.log4j.Logger;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.evacuation.io.GripsConfigReader;
import org.matsim.contrib.evacuation.jxmapviewerhelper.TileFactoryBuilder;
import org.matsim.contrib.evacuation.model.config.GripsConfigModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

public class PopulationAreaSelector implements ActionListener{

	private static final Logger log = Logger.getLogger(PopulationAreaSelector.class);
	
	private JFrame frame;
	private JPanel compositePanel;
	private MyMapViewer jMapViewer;
	private JButton saveButton;
	private JButton openBtn;


	private final String wms;

	private final String layer;

	private String configFile;
	private String scPath;
	private Scenario sc;

	private GeoPosition networkCenter;
	
	private JTable areaTable;
	private Object[][] areaTableData;
	
	private int selectedAreaID = -1;
	private String selectedAreaPop = "100";

	private JTextField popInput;
	private JButton popDeleteBt;
	private JLabel popLabel;

	protected boolean editing = false;

	private Polygon areaPolygon;

	private String targetSystem;

	private MathTransform transform;

	private String popshp;

	
	public void setSelectedAreaID(int selectedAreaID)
	{
		this.selectedAreaID = selectedAreaID;
		if (selectedAreaID>-1)
		{
			this.popLabel.setEnabled(true);
			this.popInput.setEnabled(true);
			this.popDeleteBt.setEnabled(true);
		}
	}
	
	public void setSelectedAreaPop(String selectedAreaPop) {
		this.selectedAreaPop = selectedAreaPop;
		this.popInput.setText(selectedAreaPop);
	}
	
	public int getSelectedAreaID() {
		return this.selectedAreaID;
	}
	
	public String getSelectedAreaPop() {
		return this.selectedAreaPop;
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
		System.out.println(PopulationAreaSelector.class.getSimpleName());
		System.out.println("Starts the GRIPS evacuation area editor.");
		System.out.println();
		System.out.println("usage 1: " + PopulationAreaSelector.class.getSimpleName() +"\n" +
						   "         starts the editor and uses openstreetmap as backround layer\n" +
						   "         requires a working internet connection");
		System.out.println("usage 2: " + PopulationAreaSelector.class.getSimpleName() + " -wms <url> -layer <layer name>\n" +
				           "         starts the editor and uses the given wms server to load a backgorund layer");
		
	}
	
	/**
	 * Create the application.
	 * @param layer 
	 * @param wms 
	 * @param config 
	 */
	public PopulationAreaSelector(String wms, String layer){
		this.wms = wms;
		this.layer = layer;
		initialize();
//		loadMapView(osmFile);

	}

//	private void loadMapView(String osm) {
//		
//		if (this.wms == null) {
//			addMapViewer(TileFactoryBuilder.getOsmTileFactory());
//		} else {
//			addMapViewer(TileFactoryBuilder.getWMSTileFactory(this.wms, this.layer));
//		}
////		this.snapper = new ShapeToStreetSnapperThreadWrapper(osm,this,);
//		this.jMapViewer.setSnapper(this.snapper);
//		this.jMapViewer.setCenterPosition(this.snapper.getNetworkCenter());
//		this.jMapViewer.setZoom(2);
//		this.compositePanel.repaint();
//	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		this.frame = new JFrame();
		this.frame.setSize(960, 640);
		this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.frame.getContentPane().setLayout(new BorderLayout(0, 0));
		this.frame.setResizable(true);
		
		JPanel panel = new JPanel();
		
		JPanel tools = new JPanel(new BorderLayout());
		this.frame.getContentPane().add(tools, BorderLayout.EAST);
		this.frame.getContentPane().add(panel, BorderLayout.SOUTH);
		
		this.openBtn = new JButton("Open");
		panel.add(this.openBtn);
		
		
		
		Object[] columnNames = {"area id", "population"};
		DefaultTableModel tableModel = new DefaultTableModel()
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		
		this.areaTableData = new Object[][]{};
//		areaTableData = new Object[][]{{0,1},{1,2}};
		
		tableModel.setDataVector(this.areaTableData, columnNames);
		this.areaTable = new JTable();
		this.areaTable.setModel(tableModel);
		
		
		this.areaTable.getSelectionModel().addListSelectionListener(new SelectionListener(this));
		this.areaTable.setSelectionBackground(Color.blue);
		this.areaTable.setSelectionForeground(Color.white);		
		this.areaTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		
		JPanel editAreaPanel = new JPanel(new GridLayout(0,3));
		
		this.popLabel = new JLabel("population:");
		
		this.popInput = new JTextField();
		this.popInput.addKeyListener(new KeyListener()
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
				
				int i = -1;
				while (i<PopulationAreaSelector.this.areaTable.getRowCount())
				{
					i++;
					int id = (Integer)(PopulationAreaSelector.this.areaTable.getModel()).getValueAt(i, 0);
					if (id==PopulationAreaSelector.this.selectedAreaID)
						break;
				}
				((DefaultTableModel)PopulationAreaSelector.this.areaTable.getModel()).setValueAt(PopulationAreaSelector.this.popInput.getText(), i, 1);
				
			}
			
			@Override
			public void keyPressed(KeyEvent e) {}
		});
		
		
		
		this.popDeleteBt = new JButton("delete area");
		this.popDeleteBt.addActionListener(new ActionListener()
		{
			

			@Override
			public void actionPerformed(ActionEvent e)
			{
				int sel = PopulationAreaSelector.this.areaTable.getSelectedRow();
				
				if (sel>-1)
				{
					PopulationAreaSelector.this.editing =true;
					DefaultTableModel defModel = (DefaultTableModel)PopulationAreaSelector.this.areaTable.getModel();
					
					if (PopulationAreaSelector.this.areaTable.getSelectedRow() <= defModel.getRowCount())
					{
						int id = (Integer)(PopulationAreaSelector.this.areaTable.getModel()).getValueAt(sel, 0);
						PopulationAreaSelector.this.jMapViewer.removeArea(id);

						//delete action/weight (row) in table
						((DefaultTableModel)PopulationAreaSelector.this.areaTable.getModel()).removeRow(PopulationAreaSelector.this.areaTable.getSelectedRow());
						
						PopulationAreaSelector.this.jMapViewer.repaint();
						
					}
					
					if (defModel.getRowCount()==0)
					{
						PopulationAreaSelector.this.popDeleteBt.setEnabled(false);
						PopulationAreaSelector.this.popInput.setEnabled(false);
						PopulationAreaSelector.this.popLabel.setEnabled(false);
					}
					
					PopulationAreaSelector.this.editing=false;
				}	
			}
		});
		
		
		this.popLabel.setEnabled(false);
		this.popInput.setEnabled(false);
		this.popDeleteBt.setEnabled(false);
		
		editAreaPanel.add(this.popLabel);
		editAreaPanel.add(this.popInput);
		editAreaPanel.add(this.popDeleteBt);
		
		tools.add(new JScrollPane(this.areaTable), BorderLayout.CENTER);
		tools.add(editAreaPanel, BorderLayout.SOUTH);
		
		tools.setPreferredSize(new Dimension(320, 640));
		tools.setBorder(BorderFactory.createTitledBorder("population areas"));
		
		
		
		this.saveButton = new JButton("Save");
		this.saveButton.setEnabled(false);
		this.saveButton.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(this.saveButton);
		this.compositePanel = new JPanel();
//		this.compositePanel.setBounds(new Rectangle(0, 0, 640, 640));
		this.frame.getContentPane().add(this.compositePanel, BorderLayout.CENTER);
		this.compositePanel.setLayout(new BorderLayout(0, 0));

		this.openBtn.addActionListener(this);
		this.saveButton.addActionListener(this);
		
		this.frame.setTitle("Population Area Selector");
		
		this.frame.setLocationRelativeTo(null);
		
		this.frame.addComponentListener(new ComponentListener() 
		{  
		        @Override
				public void componentResized(ComponentEvent evt)
		        {
		            Component src = (Component)evt.getSource();
		            Dimension newSize = src.getSize();
		            updateMapViewerSize(newSize.width, newSize.height);
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
		this.jMapViewer.setBounds(0, 0, 1024, 800);
		this.jMapViewer.setTileFactory(tf);
		this.jMapViewer.setPanEnabled(true);
		this.jMapViewer.setZoomEnabled(true);
		this.compositePanel.add(this.jMapViewer);
	}	

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == "Save")
		{
            savePolygon(this.popshp);
		}
		
		if (e.getActionCommand() == "Open") {
			final JFileChooser fc = new JFileChooser();
			
			fc.setCurrentDirectory(new File("C:/temp/!matsimfiles/hh/demo/"));
			
			fc.setFileFilter(new FileFilter() {

				@Override
				public String getDescription() {
					return "GRIPS config file";
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
				this.openBtn.setEnabled(false);
				this.saveButton.setEnabled(true);
				File file = fc.getSelectedFile();
				log.info("Opening: " + file.getAbsolutePath() + ".");
				this.configFile = file.getAbsolutePath();
				this.scPath = file.getParent();
				
				
				
				GripsConfigModule gcm = null;
				Config c = null;
				try
				{
					c = ConfigUtils.createConfig();
					gcm = new GripsConfigModule("grips");
					c.addModule(gcm);
					c.global().setCoordinateSystem("EPSG:3395");
					
					GripsConfigReader reader = new GripsConfigReader(gcm);
					reader.parse(file.getAbsolutePath());

				}
				catch(Exception ee)
				{
					log.warn("File is not a  grips config file. Guessing it is a common MATSim config file");
					c = ConfigUtils.loadConfig(this.configFile);
				}
				
				if (gcm!=null)
				{
					this.sc = ScenarioUtils.loadScenario(c);				
					String osm = this.sc.getConfig().getModule("grips").getValue("networkFile");
					String shp = gcm.getEvacuationAreaFileName();
					this.popshp = gcm.getPopulationFileName();

					this.targetSystem = c.global().getCoordinateSystem();
					
					readShapeFile(shp);
					
	//				this.snapper = new ShapeToStreetSnapperThreadWrapper(osm, this, shp);
					loadMapView();
				}
				else
					System.err.println("could not read shape file");
				
				this.compositePanel.repaint();				
			} else {
				log.info("Open command cancelled by user.");
			}
		}

		
	}
	
	public String getTargetSystem()
	{
		return this.targetSystem;
	}
	
	private void loadMapView() {
		if (this.wms == null) {
			addMapViewer(TileFactoryBuilder.getOsmTileFactory());
		} else {
			addMapViewer(TileFactoryBuilder.getWMSTileFactory(this.wms, this.layer));
		}
		this.jMapViewer.setCenterPosition(getNetworkCenter());
		this.jMapViewer.setZoom(2);
	}	
	
	private GeoPosition getNetworkCenter()
	{
		if (this.networkCenter != null)
			return this.networkCenter;
		
//		Envelope e = new Envelope();
//		for (Node node : this.sc.getNetwork().getNodes().values()) {
//			e.expandToInclude(MGC.coord2Coordinate(node.getCoord()));
//		}
//		Envelope e = areaPolygon.getEnvelope();
//		Geometry g = areaPolygon.getBoundary();
//		System.out.println(		"C:"+g.getCoordinates().length );
//		for (int i = 0; i < g.getCoordinates().length; i++)
//			System.out.println("i:"+i+": "+g.getCoordinates()[i].x + "|" + g.getCoordinates()[i].y);
//		System.out.println("cent:" + areaPolygon.getCentroid().getX() + "|" + areaPolygon.getCentroid().getY());
//		Coord centerC = new CoordImpl(areaPolygon.getCentroid().getX(), areaPolygon.getCentroid().getY());
//		CoordinateTransformation ct2 =  new GeotoolsTransformation(this.sc.getConfig().global().getCoordinateSystem(),"EPSG:4326");
//		centerC = ct2.transform(centerC);
		
		//TODO: transformation Ã¼berflÃ¼ssig? zu klÃ¤ren
		this.networkCenter = new GeoPosition(this.areaPolygon.getCentroid().getY(),this.areaPolygon.getCentroid().getX());
//		this.networkCenter = new GeoPosition(centerC.getY(),centerC.getX());

		return this.networkCenter;
	}	
	
	public void setSaveButtonEnabled(boolean enabled) {
		this.saveButton.setEnabled(enabled);
	}
	
	public void getTransform()
	{
		CoordinateReferenceSystem sourceCRS = MGC.getCRS("EPSG:4326");
		CoordinateReferenceSystem targetCRS = MGC.getCRS(this.targetSystem);
		this.transform = null;
		try {
			this.transform = CRS.findMathTransform(sourceCRS, targetCRS,true);
		} catch (FactoryException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	private static final class Runner implements Runnable {
		
		private final String wms;
		private final String layer;
		public Runner(String wms,String layer) {
			this.wms = wms;
			this.layer = layer;
		}
		@Override
		public void run() {
			try {
				PopulationAreaSelector window = new PopulationAreaSelector(this.wms,this.layer);
				window.frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public JTable getAreaTable() {
		return this.areaTable;
	}
	
	public MyMapViewer getjMapViewer() {
		return this.jMapViewer;
	}

	public void addNewArea(int index)
	{
		this.editing = true;
		((DefaultTableModel)this.areaTable.getModel()).addRow(new Object[]{index, "100"});
		if (!this.popInput.isEnabled())
		{
			this.popInput.setEnabled(true);
			this.popLabel.setEnabled(true);
			this.popDeleteBt.setEnabled(true);
		}
		this.editing = false;
		
	}

	public Polygon getAreaPolygon()
	{
		return this.areaPolygon;
	}


	

	
	public void readShapeFile(String shapeFileString)
	{
			ArrayList<Geometry> geometries = new ArrayList<Geometry>();
			for (SimpleFeature ft : ShapeFileReader.getAllFeatures(shapeFileString))
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
			
			this.areaPolygon = geofac.createPolygon(shell, null);		
			
	}
	
	public synchronized void savePolygon(String dest)
	{
		HashMap<Integer, Polygon> polygons = this.jMapViewer.getPolygons();
		
		if ((this.popshp == "") || (polygons.size()==0))
			return;
			
		CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:4326");
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(targetCRS);
		b.setName("EvacuationArea");
		b.add("location", MultiPolygon.class);
		b.add("persons", Long.class);
		try
		{
			SimpleFeatureBuilder builder = new SimpleFeatureBuilder(b.buildFeatureType());
			Collection<SimpleFeature> fts = new ArrayList<SimpleFeature>();

			for (Map.Entry<Integer, Polygon> entry : polygons.entrySet())
			{
			    int id = entry.getKey();
			    Polygon currentPolygon = entry.getValue();

				
				DefaultTableModel defModel = (DefaultTableModel)this.areaTable.getModel();
				
				int pop = 23;
				for (int j = 0; j < defModel.getRowCount(); j++){
					if ((Integer) this.areaTable.getModel().getValueAt(j, 0) == id){
						pop = Integer.parseInt(""+this.areaTable.getModel().getValueAt(j, 1));
						break;
					}
				}
				
				MultiPolygon mp = new GeometryFactory(new PrecisionModel(2)).createMultiPolygon(new Polygon[]{currentPolygon});
				SimpleFeature f = builder.buildFeature(null, new Object[]{mp,pop});
				fts.add(f);
			}

			ShapeFileWriter.writeGeometries(fts, this.popshp);
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}
	
	
}

class SelectionListener implements ListSelectionListener
{

	PopulationAreaSelector populationAreaSelector;
	
	public SelectionListener(PopulationAreaSelector dialog)
	{
		this.populationAreaSelector = dialog;
	}
	
	@Override
	public synchronized void valueChanged(ListSelectionEvent e)
	{
		
		if (!this.populationAreaSelector.editing)
		{
			int sel = this.populationAreaSelector.getAreaTable().getSelectedRow();
			int id = (Integer)(this.populationAreaSelector.getAreaTable().getModel()).getValueAt(sel, 0);
			String pop = String.valueOf((this.populationAreaSelector.getAreaTable().getModel()).getValueAt(sel, 1));
			
			this.populationAreaSelector.setSelectedAreaID(id);
			this.populationAreaSelector.setSelectedAreaPop(pop);
			
			MyMapViewer mapViewer = this.populationAreaSelector.getjMapViewer();
			if (mapViewer!=null)
			{
				mapViewer.setSelectedArea(id);
				mapViewer.repaint();
			}

		}
		
		
	}

}


