/* *********************************************************************** *

 * project: org.matsim.*
 * MyMapViewer.java
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

package org.matsim.contrib.evacuation.control;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.evacuation.analysis.data.EventData;
import org.matsim.contrib.evacuation.control.eventlistener.AbstractListener;
import org.matsim.contrib.evacuation.control.helper.OSMCenterCoordinateParser;
import org.matsim.contrib.evacuation.io.EvacuationConfigReader;
import org.matsim.contrib.evacuation.io.EvacuationConfigWriter;
import org.matsim.contrib.evacuation.io.ShapeIO;
import org.matsim.contrib.evacuation.model.AbstractModule;
import org.matsim.contrib.evacuation.model.AbstractToolBox;
import org.matsim.contrib.evacuation.model.Constants;
import org.matsim.contrib.evacuation.model.ModuleChain;
import org.matsim.contrib.evacuation.model.Constants.ModuleType;
import org.matsim.contrib.evacuation.model.Constants.SelectionMode;
import org.matsim.contrib.evacuation.model.config.EvacuationConfigModule;
import org.matsim.contrib.evacuation.model.imagecontainer.ImageContainerInterface;
import org.matsim.contrib.evacuation.model.locale.Locale;
import org.matsim.contrib.evacuation.model.shape.LineShape;
import org.matsim.contrib.evacuation.model.shape.PolygonShape;
import org.matsim.contrib.evacuation.model.shape.Shape;
import org.matsim.contrib.evacuation.model.shape.ShapeStyle;
import org.matsim.contrib.evacuation.populationselector.PopAreaSelector;
import org.matsim.contrib.evacuation.view.DefaultOpenDialog;
import org.matsim.contrib.evacuation.view.DefaultRenderPanel;
import org.matsim.contrib.evacuation.view.DefaultWindow;
import org.matsim.contrib.evacuation.view.Visualizer;
import org.matsim.contrib.evacuation.view.renderer.AbstractRenderLayer;
import org.matsim.contrib.evacuation.view.renderer.GridRenderer;
import org.matsim.contrib.evacuation.view.renderer.ShapeRenderer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkQuadTree;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.io.OsmNetworkReader;

import com.vividsolutions.jts.geom.Envelope;

public class Controller {

	private final ArrayList<Shape> shapes;
	private Visualizer visualizer;
	private Point mousePosition;
	private SelectionMode selectionMode;

	private ImageContainerInterface imageContainer;

	private boolean hoveringOverPoint;
	private boolean editMode;
	private Component parentComponent;

	// slippy map event listener
	private ArrayList<MouseListener> mouseListener;
	private ArrayList<MouseMotionListener> mouseMotionListener;
	private ArrayList<MouseWheelListener> mouseWheelListener;
	private ArrayList<KeyListener> keyListener;
	private boolean slippyListenersAdded;

	private AbstractListener listener;
	private String currentOSMFile;

	private EvacuationConfigModule evacuationConfigModule;
	private Config matsimConfig;

	// private String configCoordinateSystem = Constants.getEPSG();
	private final String sourceCoordinateSystem = "EPSG:4326"; // WGS 84
	private String targetCoordinateSystem = null;
	private Scenario scenario;
	private Point2D centerPosition;
	private CoordinateTransformation ctTarget2Osm;
	private CoordinateTransformation ctOsm2Target;
	private Double boundingBox;

	// these are transient variables used by most modules
	public Point2D c0;
	public Point2D c1;
	public Point p0;
	public Point p1;
	public Id<Link> linkID1;
	public Id<Link> linkID2;
	private JPanel mainPanel;
	private Rectangle mainPanelBounds;
	private boolean inSelection;

	private final ShapeUtils shapeUtils;

	private AbstractToolBox activeToolBox;
	private ArrayList<AbstractModule> modules;

	private Locale locale = Constants.getLocale();

	private LinkQuadTree links;
	private ArrayList<Link> linkList;

	// module running stand alone (by default: false)
	private boolean standAlone = false;
	private String matsimConfigFile;
	private String configFilePath;

	private String scenarioPath;
	private boolean populationFileOpened;

	private boolean mainWindowUndecorated = false;
	private String evacuationFile;

	private ModuleType activeModuleType;

	private EventData data;
	private String wms;
	private String layer;

	private ModuleChain moduleChain;

	public Controller() {
		initListeners();

		this.shapes = new ArrayList<Shape>();
		this.selectionMode = SelectionMode.CIRCLE;
		this.shapeUtils = new ShapeUtils(this);
		this.mousePosition = new Point(-1, -1);
		EventQueue.invokeLater(new Runner(this));

	}

	public Controller(String[] args) {
		this();
		this.wms = null;
		this.layer = null;
		if (args.length == 4) {
			for (int i = 0; i < 4; i += 2) {
				if (args[i].equalsIgnoreCase("-wms")) {
					this.wms = args[i + 1];
				}
				if (args[i].equalsIgnoreCase("-layer")) {
					this.layer = args[i + 1];
				}
			}
		} else if (args.length != 0) {
			printUsage();
			System.exit(-1);
		}
	}

	private void printUsage() {
		System.out.println(this.locale.getUsage());
		// TODO:
		// JOptionPane.showConfirmDialog(this, locale.infoMatsimTime(), "",
		// JOptionPane.WARNING_MESSAGE);

	}

	public String getMatsimConfigFile() {
		return this.matsimConfigFile;
	}

	private void initListeners() {
		this.mouseListener = new ArrayList<MouseListener>();
		this.mouseMotionListener = new ArrayList<MouseMotionListener>();
		this.mouseWheelListener = new ArrayList<MouseWheelListener>();
		this.keyListener = new ArrayList<KeyListener>();

		this.slippyListenersAdded = false;

	}

	private static final class Runner implements Runnable {
		private final Controller controller;

		public Runner(Controller controller) {
			this.controller = controller;
		}

		@Override
		public void run() {
			try {
				this.controller.setVisualizer(new Visualizer(this.controller));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public ArrayList<Shape> getActiveShapes() {
		return this.shapes;
	}

	public void setVisualizer(Visualizer visualizer) {
		this.visualizer = visualizer;

	}

	public void addRenderLayer(AbstractRenderLayer layer) {
		this.visualizer.addRenderLayer(layer);
	}

	public Visualizer getVisualizer() {
		return this.visualizer;
	}

	public void resetRenderer(boolean leaveMapRenderer) {
		this.visualizer.removeAllLayers(leaveMapRenderer);
	}

	public Point getMousePosition() {
		return this.mousePosition;
	}

	public void setMousePosition(Point mousePosition) {
		this.mousePosition = mousePosition;

	}

	public void setMousePosition(int x, int y) {
		this.mousePosition.x = x;
		this.mousePosition.y = y;
	}

	public Rectangle getViewportBounds() {
		return this.visualizer.getActiveMapRenderLayer().getViewportBounds();
	}

	public SelectionMode getSelectionMode() {
		return this.selectionMode;
	}

	public void setSelectionMode(SelectionMode selectionMode) {
		this.selectionMode = selectionMode;
	}

	public boolean isHoveringOverPoint() {
		return this.hoveringOverPoint;
	}

	public void setHoveringOverPoint(boolean hoveringOverPoint) {
		this.hoveringOverPoint = hoveringOverPoint;
	}

	public boolean isEditMode() {
		return this.editMode;
	}

	public void setEditMode(boolean editMode) {
		this.editMode = editMode;
	}

	public ImageContainerInterface getImageContainer() {
		return this.imageContainer;
	}

	public void setImageContainer(ImageContainerInterface imageContainer) {
		this.imageContainer = imageContainer;

		updateMapLayerImages();
	}

	public void updateMapLayerImages() {
		if (hasMapRenderer())
			this.visualizer.getActiveMapRenderLayer().updateMapImage();
	}

	public void paintLayers() {

		if (this.parentComponent != null)
			this.parentComponent.repaint();
		else
			this.visualizer.paintLayers();

	}

	public void setParentComponent(Component parentComponent) {
		this.parentComponent = parentComponent;
	}

	public Component getParentComponent() {
		return this.parentComponent;
	}

	public void repaintParent() {
		if (this.parentComponent != null)
			this.parentComponent.repaint();
	}

	public void setSlippyMapEventListeners(ArrayList<EventListener> eventListeners) {

		// put each listener to the corresponding array list
		initListeners();
		for (EventListener listener : eventListeners) {
			if (listener instanceof MouseListener) {
				this.mouseListener.add((MouseListener) listener);
				this.mouseMotionListener.add((MouseMotionListener) listener);
			} else if (listener instanceof MouseWheelListener)
				this.mouseWheelListener.add((MouseWheelListener) listener);
			else if (listener instanceof KeyListener)
				this.keyListener.add((KeyListener) listener);
		}

		if (eventListeners.size() > 0)
			this.slippyListenersAdded = true;
	}

	public ArrayList<MouseListener> getMouseListener() {
		return this.mouseListener;
	}

	public ArrayList<MouseMotionListener> getMouseMotionListener() {
		return this.mouseMotionListener;
	}

	public ArrayList<MouseWheelListener> getMouseWheelListener() {
		return this.mouseWheelListener;
	}

	public ArrayList<KeyListener> getKeyListener() {
		return this.keyListener;
	}

	public boolean slippyEventListenersAvailable() {
		return this.slippyListenersAdded;
	}

	public void setListener(AbstractListener listener) {
		this.listener = listener;
	}

	public AbstractListener getListener() {
		return this.listener;
	}

	public boolean hasMapRenderer() {
		if (this.visualizer != null)
			return this.visualizer.hasMapRenderer();
		else
			return false;
	}

	public String getCurrentOSMFile() {
		return this.currentOSMFile;
	}

	public void setCurrentOSMFile(String currentOSMFile) {
		this.currentOSMFile = currentOSMFile;
	}

	public File getCurrentWorkingDirectory() {
		// return new File("C:\\temp\\!matsimfiles\\fostercityca\\");
		return new File(System.getProperty("user.home"));
	}

	public EvacuationConfigModule getEvacuationConfigModule() {
		return this.evacuationConfigModule;
	}

	public void setEvacuationConfigModule(EvacuationConfigModule evacuationConfigModule) {
		this.evacuationConfigModule = evacuationConfigModule;
	}

	public boolean evacuationEvacuationConfig(File selectedFile) {
		boolean rv = false;
		try {
			this.evacuationConfigModule = new EvacuationConfigModule("evacuation");//, selectedFile.getAbsolutePath());
			EvacuationConfigReader parser = new EvacuationConfigReader(this.evacuationConfigModule);
			parser.parse(selectedFile.getAbsolutePath());

			this.scenarioPath = selectedFile.getParent();
			this.evacuationFile = selectedFile.getAbsolutePath();

			String osmf = this.evacuationConfigModule.getNetworkFileName();
			try {
				rv = this.readOSMFile(osmf);
				if (rv == false)
					JOptionPane.showConfirmDialog(null, "OSM file " + osmf + " does not exists.", "Fatal error. Exiting.", JOptionPane.WARNING_MESSAGE);

			} catch (Exception e) {
				return false;
			}
			this.setCurrentOSMFile(osmf);

			String areafile = this.evacuationConfigModule.getEvacuationAreaFileName();

			this.setWms(this.evacuationConfigModule.getWms());
			this.setLayer(this.evacuationConfigModule.getLayer());

			File file = new File(areafile);
			String dir = file.getParent();
			File dirfile = new File(dir);
			if (!dirfile.exists()) {
				JOptionPane.showConfirmDialog(null, "Area file " + areafile + " is located in an invalid directory.\nPlease select another area file.", "Warning", JOptionPane.WARNING_MESSAGE);
			}
			if (!file.getName().endsWith(".shp"))
				areafile += ".shp";
			EvacuationConfigModule gcm = this.getEvacuationConfigModule();
			gcm.setEvacuationAreaFileName(areafile);

			String popfile = this.evacuationConfigModule.getPopulationFileName();
			file = new File(popfile);
			dir = file.getParent();
			dirfile = new File(dir);
			if (!dirfile.exists()) {
				JOptionPane.showConfirmDialog(null, "Population file " + popfile + " is located in an invalid directory. \nPlease select another population file.", "Warning", JOptionPane.WARNING_MESSAGE);
			}
			if (!file.getName().endsWith(".shp"))
				popfile += ".shp";
			this.getEvacuationConfigModule().setPopulationFileName(popfile);

			if (!this.standAlone)
				updateOtherModules();

			return true;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			rv = false;
		}
		return rv;
	}

	private void setPopDensFilename(String popDensFilename) {
		// TODO Auto-generated method stub

	}

	private void setLayer(String layer) {
		this.layer = layer;
	}

	private void setWms(String wms) {
		this.wms = wms;
	}

	public String getEvacuationFile() {
		return this.evacuationFile;
	}

	public boolean openEvacuationConfig() {
		DefaultOpenDialog openDialog = new DefaultOpenDialog(this, "xml", this.locale.infoEvacuationFile(), false);
		int returnValue = openDialog.showOpenDialog(this.getParentComponent());

		if (returnValue == JFileChooser.APPROVE_OPTION)
			return evacuationEvacuationConfig(openDialog.getSelectedFile());
		else
			return false;

	}

	public boolean isEvacuationConfigOpenend() {
		return (this.evacuationConfigModule != null);

	}

	private void updateOtherModules() {

		// TODO add listener

	}

	public boolean readOSMFile(String networkFileName) {

		try {

			// has a config already been loaded?
			// if (matsimConfig==null)
			{
				this.matsimConfig = ConfigUtils.createConfig();
				
				determineTargetCoordinateSystem(networkFileName);
				
				this.matsimConfig.global().setCoordinateSystem(this.targetCoordinateSystem);

				this.scenario = ScenarioUtils.createScenario(this.matsimConfig);

			}

			// check if geo tranformation tools are available
			checkGeoTransformationTools();

			// finally read network
			//TODO c 'n p from ScenarioGenerator needs to be cleaned up [GL March '14]
			EvacuationConfigModule gcm = this.evacuationConfigModule;
			if (gcm.getMainTrafficType().equals("vehicular")) {
				OsmNetworkReader reader = new OsmNetworkReader(this.scenario.getNetwork(), this.ctOsm2Target, true);
				reader.setKeepPaths(true);
				reader.parse(networkFileName);
			} else if (gcm.getMainTrafficType().equals("pedestrian")) {
				OsmNetworkReader reader = new OsmNetworkReader(this.scenario.getNetwork(), this.ctOsm2Target, false);
				reader.setKeepPaths(true);
				
				double laneCap = 2808 * 2; // 2 lanes

				reader.setHighwayDefaults(2, "trunk", 2, 1.34, 1., laneCap);
				reader.setHighwayDefaults(2, "trunk_link", 2, 1.34, 1.0, laneCap);
				reader.setHighwayDefaults(3, "primary", 2, 1.34, 1.0, laneCap);
				reader.setHighwayDefaults(3, "primary_link", 2, 1.34, 1.0, laneCap);
				reader.setHighwayDefaults(4, "secondary", 2, 1.34, 1.0, laneCap);
				reader.setHighwayDefaults(5, "tertiary", 2, 1.34, 1.0, laneCap);
				reader.setHighwayDefaults(6, "minor", 2, 1.34, 1.0, laneCap);
				reader.setHighwayDefaults(6, "unclassified", 2, 1.34, 1.0, laneCap);
				reader.setHighwayDefaults(6, "residential", 2, 1.34, 1.0, laneCap);
				reader.setHighwayDefaults(6, "living_street", 2, 1.34, 1.0, laneCap);
				reader.setHighwayDefaults(6, "path", 2, 1.34, 1.0, laneCap);
				reader.setHighwayDefaults(6, "cycleway", 2, 1.34, 1.0, laneCap);
				reader.setHighwayDefaults(6, "footway", 2, 1.34, 1.0, laneCap);
				reader.setHighwayDefaults(6, "steps", 2, 1.34, 1.0, laneCap);
				reader.setHighwayDefaults(6, "pedestrian", 2, 1.34, 1.0, laneCap);

				// max density is set to 5.4 p/m^2
				((NetworkImpl) this.scenario.getNetwork()).setEffectiveLaneWidth(.6);
				((NetworkImpl) this.scenario.getNetwork()).setEffectiveCellSize(.31);
				reader.parse(networkFileName);
			} else if (gcm.getMainTrafficType().equals("mixed")) {
				throw new RuntimeException("not implemented yet!");
			}

			processNetwork(false);

			return true;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return false;
		}

	}

	private void determineTargetCoordinateSystem(String networkFileName) {
		OSMCenterCoordinateParser parser = new OSMCenterCoordinateParser();
		parser.setValidating(false);
		parser.parse(networkFileName);
		double lat = parser.getCenterLat();
		double lon = parser.getCenterLon();
		String epsg = MGC.getUTMEPSGCodeForWGS84Coordinate(lon, lat);
		this.targetCoordinateSystem = epsg;
		
	}

	private void checkGeoTransformationTools() {
		// are the transformation tools already at hand?
		if ((this.ctOsm2Target == null) || (this.ctTarget2Osm == null)) {
			this.ctOsm2Target = new GeotoolsTransformation(this.sourceCoordinateSystem, this.targetCoordinateSystem);
			this.ctTarget2Osm = new GeotoolsTransformation(this.targetCoordinateSystem, this.sourceCoordinateSystem);
		}
	}

	private void processNetwork(boolean processLinks) {
		// fill envelope with collected roads (links)
		Envelope e = new Envelope();
		for (Node node : this.scenario.getNetwork().getNodes().values()) {
			// ignore end nodes
			if (node.getId().toString().contains("en"))
				continue;

			e.expandToInclude(MGC.coord2Coordinate(node.getCoord()));
		}

		// calculate center and bounding box
		Coord centerC = new CoordImpl((e.getMaxX() + e.getMinX()) / 2, (e.getMaxY() + e.getMinY()) / 2);
		Coord min = new CoordImpl(e.getMinX(), e.getMinY());
		Coord max = new CoordImpl(e.getMaxX(), e.getMaxY());

		// also process links (to link quad tree)
		if (processLinks) {
			this.links = new LinkQuadTree(e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY());
			this.linkList = new ArrayList<Link>();
			NetworkImpl net = (NetworkImpl) this.scenario.getNetwork();
			for (Link link : net.getLinks().values()) {
				// ignore end links
				if (link.getId().toString().contains("el"))
					continue;
				this.links.put(link);
				this.linkList.add(link);
			}
		}

		// transform coordinates
		centerC = this.ctTarget2Osm.transform(centerC);
		min = this.ctTarget2Osm.transform(min);
		max = this.ctTarget2Osm.transform(max);

		// pass center position and bounding box to the controller
		this.centerPosition = new Point2D.Double(centerC.getX(), centerC.getY());
		this.boundingBox = new Rectangle2D.Double(min.getY(), min.getX(), max.getY() - min.getY(), max.getX() - min.getX());
	}

	public CoordinateTransformation getCtOsm2Target() {
		return this.ctOsm2Target;
	}

	public CoordinateTransformation getCtTarget2Osm() {
		return this.ctTarget2Osm;
	}

	public Point2D getCenterPosition() {
		return this.centerPosition;
	}

	public Rectangle2D getBoundingBox() {
		return this.boundingBox;
	}

	public void setMainPanelListeners(boolean removeAllExistingListeners) {
		if (this.mainPanel != null) {
			if (removeAllExistingListeners)
				removeAllPanelEventListeners();

			this.mainPanel.addMouseListener(getListener());
			this.mainPanel.addMouseMotionListener(getListener());
			this.mainPanel.addKeyListener(getListener());
			this.mainPanel.addMouseWheelListener(getListener());
		}

	}

	public void removeAllPanelEventListeners() {
		if (this.mainPanel != null) {
			MouseListener[] ms = this.mainPanel.getMouseListeners();
			MouseMotionListener[] mms = this.mainPanel.getMouseMotionListeners();
			MouseWheelListener[] mws = this.mainPanel.getMouseWheelListeners();
			KeyListener[] ks = this.mainPanel.getKeyListeners();

			for (MouseListener l : ms)
				this.mainPanel.removeMouseListener(l);
			for (MouseMotionListener m : mms)
				this.mainPanel.removeMouseMotionListener(m);
			for (MouseWheelListener mw : mws)
				this.mainPanel.removeMouseWheelListener(mw);
			for (KeyListener k : ks)
				this.mainPanel.removeKeyListener(k);
		}

	}

	/**
	 * Add a shape. Since no specific layer is given:
	 * 
	 * - scans for shapes - creates a new shape, if no shape found or replaces
	 * the old shape with the same id
	 * 
	 * @param shape
	 */
	public void addShape(Shape shape) {
		for (int i = 0; i < this.shapes.size(); i++) {

			if (this.shapes.get(i).getId().equals(shape.getId())) {
				this.shapes.set(i, shape);
				return;
			}
		}
		this.shapes.add(shape);

	}

	public Shape getShapeById(String id) {
		for (Shape shape : this.shapes)
			if (shape.getId().equals(id))
				return shape;

		return null;
	}

	public boolean hasShapeRenderer() {
		return this.visualizer.hasShapeRenderer();
	}

	public boolean hasSecondaryShapeRenderer() {
		return this.visualizer.hasSecondaryShapeRenderer();

	}

	public int getZoom() {
		if (this.visualizer.getActiveMapRenderLayer() != null)
			return this.visualizer.getActiveMapRenderLayer().getZoom();

		return 0;
	}

	public Scenario getScenario() {
		return this.scenario;
	}

	public void enableAllRenderLayers() {
		if (this.visualizer.getRenderLayers() != null)
			for (AbstractRenderLayer layer : this.visualizer.getRenderLayers())
				layer.setEnabled(true);
	}

	public void enableMapRenderer() {
		if (this.visualizer.getActiveMapRenderLayer() != null)
			this.visualizer.getActiveMapRenderLayer().setEnabled(true);
	}

	public void disableAllRenderLayers() {
		if (hasShapeRenderer())
			deselectShapes();
		if (this.visualizer.getRenderLayers() != null)
			for (AbstractRenderLayer layer : this.visualizer.getRenderLayers())
				layer.setEnabled(false);

	}

	/**
	 * Transform geometric coordinates to pixels -respecting the origin of
	 * computer graphic coordinates
	 * 
	 * @param rectangle
	 * @return
	 */
	public Rectangle geoToPixel(Rectangle2D rectangle) {

		Point2D minGeo = new Point2D.Double(rectangle.getX(), rectangle.getY());
		Point2D maxGeo = new Point2D.Double(rectangle.getX() + rectangle.getWidth(), rectangle.getY() + rectangle.getHeight());

		Point2D minPoint = this.visualizer.getActiveMapRenderLayer().geoToPixel(minGeo);
		Point2D maxPoint = this.visualizer.getActiveMapRenderLayer().geoToPixel(maxGeo);

		// preliminary assignment
		int x = (int) minPoint.getX();
		int y = (int) minPoint.getY();
		int w = (int) maxPoint.getX() - (int) minPoint.getX();
		int h = (int) maxPoint.getY() - (int) minPoint.getY();

		// correcting negative latitude / longitude values
		if (h < 0) {
			y += h;
			h = h * -1;
		}
		if (w < 0) {
			x += w;
			w = w * -1;
		}

		Rectangle newRect = new Rectangle(x, y, w, h);

		return newRect;
	}

	public Point geoToPixel(Point2D point) {
		return this.visualizer.getActiveMapRenderLayer().geoToPixel(point);
	}

	public Point2D pixelToGeo(Point2D point) {
		return this.visualizer.getActiveMapRenderLayer().pixelToGeo(point);
	}

	public void setZoom(int zoom) {
		if (this.visualizer.getActiveMapRenderLayer() != null)
			this.visualizer.getActiveMapRenderLayer().setZoom(zoom);
	}

	public void validateRenderLayers() {
	}

	public void setMainPanel(JPanel mainPanel, boolean updatePanelBounds) {

		if ((this.getParentComponent() != null))
			((DefaultWindow) this.getParentComponent()).setMainPanel(mainPanel);

		this.mainPanel = mainPanel;

		if (updatePanelBounds)
			updatePanelBounds();
	}

	public void updatePanelBounds() {
		this.mainPanelBounds = SwingUtilities.convertRectangle(this.mainPanel.getParent(), this.mainPanel.getBounds(), this.getParentComponent());
	}

	public JPanel getMainPanel() {
		return this.mainPanel;
	}

	public Rectangle getMainPanelBounds() {
		return this.mainPanelBounds;
	}

	public ShapeUtils getShapeUtils() {
		return this.shapeUtils;
	}

	public String getTargetCoordinateSystem() {
		return this.targetCoordinateSystem;
	}

	public String getSourceCoordinateSystem() {
		return this.sourceCoordinateSystem;
	}

	public String getConfigCoordinateSystem() {
		return this.targetCoordinateSystem;
	}

	public boolean saveShape(Shape shape, String fileName) {
		if (shape instanceof PolygonShape) {
			boolean saved = ShapeIO.savePolygon(this, (PolygonShape) shape, this.evacuationConfigModule.getEvacuationAreaFileName());
		}

		return true;
	}

	public Locale getLocale() {
		return this.locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public boolean isStandAlone() {
		return this.standAlone;
	}

	public void setStandAlone(boolean standAlone) {
		this.standAlone = standAlone;
	}

	public boolean hasEvacuationConfig() {
		if (this.evacuationConfigModule != null)
			return true;
		else
			return false;
	}

	public AbstractToolBox getActiveToolBox() {
		return this.activeToolBox;
	}

	public void setActiveToolBox(AbstractToolBox activeToolBox) {
		this.activeToolBox = activeToolBox;
		if ((this.parentComponent != null)) {
			if (this.parentComponent instanceof DefaultWindow) {
				((DefaultWindow) this.parentComponent).setToolBox(activeToolBox);
			}
		}

	}

	public boolean openEvacuationShape(String id) {
		
		//check if evac area is from file (if not: reopen!)
		if (this.getShapeById(id) != null) {
			if (this.getShapeById(id).isFromFile())
				return true;
			else
				this.removeShape(id);
		}
		

		ShapeStyle evacShapeStyle = Constants.SHAPESTYLE_EVACAREA;
		String dest;

		// evacuation config is not opened, check for file destination in scenario
		if (this.evacuationConfigModule == null) {
			if (this.scenario != null)
				dest = this.scenario.getConfig().getModule("evacuation").getValue("evacuationAreaFile");
			else
				return false;
		} else
			dest = this.evacuationConfigModule.getEvacuationAreaFileName();

		return openShape(id, dest, evacShapeStyle);
	}

	public boolean openShape(String id, String fileName, ShapeStyle style) {
		try {
			PolygonShape evacShape = ShapeIO.getShapeFromFile(this, id, fileName);
			evacShape.setStyle(style);
			addShape(evacShape);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public void removeShape(String id) {
		for (int i = 0; i < this.visualizer.getActiveShapes().size(); i++) {
			if (this.visualizer.getActiveShapes().get(i).getId().equals(id))
				this.visualizer.getActiveShapes().remove(i);
		}

	}

	public boolean saveShapes(String populationFileName) {
		ArrayList<PolygonShape> polygonShapes = new ArrayList<PolygonShape>();

		for (Shape shape : this.getActiveShapes())
			if ((shape instanceof PolygonShape) && (shape.getMetaData(Constants.POPULATION) != null))
				polygonShapes.add((PolygonShape) shape);

		if (polygonShapes.size() > 0)
			return ShapeIO.savePopulationAreaPolygons(this, polygonShapes, populationFileName);

		return false;

	}

	public void setTempLinkId(int n, Id<Link> id) {
		if (n == 0)
			this.linkID1 = id;
		else if (n == 1)
			this.linkID2 = id;
	}

	public Id<Link> getTempLinkId(int n) {
		if (n == 0)
			return this.linkID1;
		else
			return this.linkID2;

	}

	public boolean isMatsimConfigOpened() {
		return (this.matsimConfig != null);
	}

	public boolean openMastimConfig() {

		DefaultOpenDialog openDialog = new DefaultOpenDialog(this, "xml", this.locale.infoMatsimFile(), false);
		int returnValue = openDialog.showOpenDialog(this.getParentComponent());

		if (returnValue == JFileChooser.APPROVE_OPTION)
			return openMastimConfig(openDialog.getSelectedFile());
		else
			return false;
	}

	public boolean openMastimConfig(File file) {
		try {

			this.configFilePath = file.getAbsolutePath();
			this.scenarioPath = file.getParent();

			this.matsimConfigFile = file.getAbsolutePath();

			try {
				this.matsimConfig = ConfigUtils.loadConfig(this.matsimConfigFile);
			} catch (org.matsim.core.utils.io.UncheckedIOException e) {
				e.printStackTrace();
			}
			this.scenario = ScenarioUtils.loadScenario(this.matsimConfig);

			this.targetCoordinateSystem = this.matsimConfig.global().getCoordinateSystem();

			// check if geo tranformation tools are available
			checkGeoTransformationTools();

			// process the network
			processNetwork(true);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	public String getConfigFilePath() {
		return this.configFilePath;
	}

	public String getScenarioPath() {
		return this.scenarioPath;
	}

	// public void setConfigCoordinateSystem(String configCoordinateSystem) {
	// this.configCoordinateSystem = configCoordinateSystem;
	// }

	public void setScenarioPath(String scenarioPath) {
		this.scenarioPath = scenarioPath;
	}

	public boolean hasGridRenderer() {
		return this.visualizer.hasGridRenderer();
	}

	public LinkQuadTree getLinks() {
		return this.links;
	}

	public Point2D coordToPoint(Coord coord) {
		coord = this.ctTarget2Osm.transform(coord);
		return new Point2D.Double(coord.getY(), coord.getX());
	}

	public boolean isPopulationFileOpened() {
		return this.populationFileOpened;
	}

	public void setPopulationFileOpened(boolean populationFileOpened) {
		this.populationFileOpened = populationFileOpened;
	}

	public boolean openPopulationFile() {
		String dest;

		// evacuation config is not opened, check for file destination in scenario
		if (this.evacuationConfigModule == null) {
			if (this.scenario != null)
				dest = this.scenario.getConfig().getModule("evacuation").getValue("populationFile");
			else
				return false;
		} else
			dest = this.evacuationConfigModule.getPopulationFileName();

		ArrayList<PolygonShape> popShapes = ShapeIO.getShapesFromFile(this, dest, Constants.SHAPESTYLE_POPAREA);
		
		//remove existing shapes that were loaded from a file
		ArrayList<Shape> shapesToRemove = new ArrayList<Shape>();
		for (Shape shape : this.shapes)
		{
			if ((shape.getMetaData(Constants.POPULATION)!=null) && (shape.isFromFile()))
				shapesToRemove.add(shape);
		}	
		for (Shape shape : shapesToRemove)
			this.shapes.remove(shape);
		
		for (PolygonShape shape : popShapes)
			addShape(shape);

		this.populationFileOpened = true;

		return true;
	}

	public boolean openNetworkChangeEvents() {
		if (this.scenario != null) {
			Collection<NetworkChangeEvent> changeEvents = ((NetworkImpl) this.scenario.getNetwork()).getNetworkChangeEvents();
			int id = this.visualizer.getPrimaryShapeRenderLayer().getId();

			if (changeEvents != null) {
				for (NetworkChangeEvent event : changeEvents) {
					Collection<Link> changeEventLinks = event.getLinks();
					for (Link link : changeEventLinks) {
						Point2D from2D = this.coordToPoint(link.getFromNode().getCoord());
						Point2D to2D = this.coordToPoint(link.getToNode().getCoord());

						LineShape linkShape = ShapeFactory.getRoadClosureShape(id, link.getId().toString(), from2D, to2D);
						addShape(linkShape);
					}
				}
			}
		}

		return true;
	}

	public boolean isMainWindowUndecorated() {
		return this.mainWindowUndecorated;
	}

	public void setMainFrameUndecorated(boolean mainWindowUndecorated) {
		this.mainWindowUndecorated = mainWindowUndecorated;
	}

	public void addModuleChain(ArrayList<AbstractModule> moduleChain) {
		this.modules = moduleChain;
	}

	public ArrayList<AbstractModule> getModules() {
		return this.modules;
	}

	public ArrayList<Link> getLinkList() {
		return this.linkList;
	}

	public void readOSM(boolean b) {

	}

	public ModuleType getActiveModuleType() {
		return this.activeModuleType;
	}

	public void setActiveModuleType(ModuleType activeModuleType) {
		this.activeModuleType = activeModuleType;
	}

	public void setGoalAchieved(boolean goalAchieved) {
		if (goalAchieved) {
			AbstractModule module = getModuleByType(this.activeModuleType);
			if (module != null) {
				module.enableNextModules();
				module.disablePastModules();
			}
			updateParentUI();
		}
	}

	public AbstractModule getModuleByType(ModuleType moduleType) {
		if (this.modules == null)
			return null;
		for (AbstractModule module : this.modules) {
			if (module.getModuleType().equals(moduleType))
				return module;
		}
		return null;
	}

	public void enableModule(ModuleType moduleType) {
		AbstractModule module = getModuleByType(moduleType);
		if (module != null)
			module.setEnabled(true);

	}

	public void disableModule(ModuleType moduleType) {
		AbstractModule module = getModuleByType(moduleType);
		if (module != null)
			module.setEnabled(false);

	}

	public void updateParentUI() {
		if ((this.parentComponent != null) && (this.parentComponent instanceof DefaultWindow))
			((DefaultWindow) this.parentComponent).updateMask();

	}

	public boolean hasDefaultRenderPanel() {
		return (this.getMainPanel() instanceof DefaultRenderPanel);
	}

	public void setToolBoxVisible(boolean toggle) {
		if ((this.getParentComponent() != null) && (this.getParentComponent() instanceof DefaultWindow))
			((DefaultWindow) this.getParentComponent()).setToolBoxVisible(toggle);
	}

	public EventData getEventData() {
		return this.data;
	}

	public void setEventData(EventData data) {
		this.data = data;
	}

	public void disableShapeLayers() {
		for (AbstractRenderLayer layer : this.visualizer.getRenderLayers()) {
			if (layer instanceof ShapeRenderer)
				layer.setEnabled(false);
		}

	}

	public void disableGridLayer() {
		for (AbstractRenderLayer layer : this.visualizer.getRenderLayers()) {
			if (layer instanceof GridRenderer)
				layer.setEnabled(false);
		}

	}

	public String getIterationsOutputDirectory() {
		return this.scenario.getConfig().getModule("controler").getValue("outputDirectory");
	}

	public void exit(String exitString) {
		System.exit(0);
	}

	public String getWMS() {
		return this.wms;
	}

	public String getWMSLayer() {
		return this.layer;
	}

	public void setModuleChain(ModuleChain moduleChain) {
		this.moduleChain = moduleChain;
	}

	public ModuleChain getModuleChain() {
		return this.moduleChain;
	}

	public ArrayList<ModuleType> getNextModules(ModuleType moduleType) {
		if (this.moduleChain == null)
			return null;
		else
			return this.moduleChain.getNextModules(moduleType);

	}

	public ArrayList<ModuleType> getPastModules(ModuleType moduleType) {
		if (this.moduleChain == null)
			return null;
		else
			return this.moduleChain.getPastModules(moduleType);
	}

	public boolean isInSelection() {
		return this.inSelection;
	}

	public void setInSelection(boolean inSelection) {
		this.inSelection = inSelection;
	}

	public int getPopAreaCount() {

		for (AbstractModule module : this.modules) {
			if (module instanceof PopAreaSelector)
				return ((PopAreaSelector) module).getPopAreaCount();
		}
		return -1;
	}

	public void deselectShapes() {
		for (Shape shape : getActiveShapes())
			shape.setSelected(false);
	}

	public void deselectShapesByMetaData(String string) {
		for (Shape shape : getActiveShapes()) {
			if (shape.getMetaData(string) != null)
				shape.setSelected(false);
		}

	}

	public boolean writeEvacuationConfig() {
		if ((this.evacuationConfigModule != null) && (this.evacuationFile != null)) {
			return writeEvacuationConfig(this.evacuationConfigModule, this.evacuationFile);
		} else
			return false;

	}

	public boolean writeEvacuationConfig(EvacuationConfigModule evacuationConfigModule, String fileLocation) {

		if (evacuationConfigModule != null) {
			try {
				EvacuationConfigWriter gcs = new EvacuationConfigWriter(evacuationConfigModule);
				gcs.write(fileLocation);

			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}

			return true;
		} else
			return false;
	}

	public void setUnsavedChanges(boolean b) {
		getModuleByType(this.getActiveModuleType()).setUnsavedChanges(b);
	}

	public String getPopDensFilename() {
		return this.getEvacuationConfigModule().getPopDensFilename();
	}

}
