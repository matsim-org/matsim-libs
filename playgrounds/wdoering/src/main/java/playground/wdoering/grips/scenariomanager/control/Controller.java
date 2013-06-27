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

package playground.wdoering.grips.scenariomanager.control;

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
import java.util.HashMap;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.grips.analysis.control.EventReaderThread;
import org.matsim.contrib.grips.config.GripsConfigModule;
import org.matsim.contrib.grips.io.GripsConfigDeserializer;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.LinkQuadTree;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.io.OsmNetworkReader;

import playground.wdoering.grips.scenariomanager.control.eventlistener.AbstractListener;
import playground.wdoering.grips.scenariomanager.control.io.ShapeIO;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;
import playground.wdoering.grips.scenariomanager.model.AbstractToolBox;
import playground.wdoering.grips.scenariomanager.model.Constants;
import playground.wdoering.grips.scenariomanager.model.Constants.ModuleType;
import playground.wdoering.grips.scenariomanager.model.imagecontainer.ImageContainerInterface;
import playground.wdoering.grips.scenariomanager.model.locale.GermanLocale;
import playground.wdoering.grips.scenariomanager.model.locale.Locale;
import playground.wdoering.grips.scenariomanager.model.shape.LineShape;
import playground.wdoering.grips.scenariomanager.model.shape.PolygonShape;
import playground.wdoering.grips.scenariomanager.model.shape.Shape;
import playground.wdoering.grips.scenariomanager.model.shape.ShapeStyle;
import playground.wdoering.grips.scenariomanager.view.DefaultOpenDialog;
import playground.wdoering.grips.scenariomanager.view.DefaultRenderPanel;
import playground.wdoering.grips.scenariomanager.view.DefaultWindow;
import playground.wdoering.grips.scenariomanager.view.Visualizer;
import playground.wdoering.grips.scenariomanager.view.renderer.AbstractRenderLayer;
import playground.wdoering.grips.scenariomanager.view.renderer.GridRenderer;
import playground.wdoering.grips.scenariomanager.view.renderer.ShapeRenderer;
import playground.wdoering.grips.v2.analysis.control.EventHandler;
import playground.wdoering.grips.v2.analysis.data.ColorationMode;
import playground.wdoering.grips.v2.analysis.data.EventData;

import com.vividsolutions.jts.geom.Envelope;

public class Controller
{
	public static enum SelectionMode
	{
		CIRCLE, POLYGON
	};

	private ArrayList<Shape> shapes;
	private Visualizer visualizer;
	private Point mousePosition;
	private Rectangle viewportBounds;
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

	private GripsConfigModule gripsConfigModule;
	private Config matsimConfig;

	private String configCoordinateSystem = "EPSG:3395";
	private String sourceCoordinateSystem = "EPSG:4326";
	private String targetCoordinateSystem;
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
	public Id linkID1;
	public Id linkID2;
	private JPanel mainPanel;
	private Rectangle mainPanelBounds;

	private ShapeUtils shapeUtils;

	private AbstractToolBox activeToolBox;
	private HashMap<ModuleType, AbstractToolBox> toolBoxes;
	private ArrayList<AbstractModule> modules;

	private Locale locale = new GermanLocale();

	private LinkQuadTree links;
	private ArrayList<Link> linkList;

	// module running stand alone (by default: false)
	private boolean standAlone = false;
	private String matsimConfigFile;
	private String matsimConfigFilePath;
	private Config c;
	private String configFilePath;

	private String scenarioPath;
	private boolean populationFileOpened;

	private boolean mainWindowUndecorated = false;
	private boolean readOSM;
	private String gripsFile;

	private ModuleType activeModuleType;

	private EventData data;
	private EventHandler eventHandler;
	private Thread readerThread;
	private double gridSize;
	
	private String wms;
	private String layer;

	public Controller()
	{
		initListeners();

		this.shapes = new ArrayList<Shape>();
		this.selectionMode = SelectionMode.CIRCLE;
		this.shapeUtils = new ShapeUtils(this);
		this.mousePosition = new Point(-1, -1);
		EventQueue.invokeLater(new Runner(this));

	}

	public Controller(String[] args)
	{
		this();
		this.wms = null;
		this.layer = null;
		if (args.length == 4) {
			for (int i = 0; i < 4; i += 2) {
				if (args[i].equalsIgnoreCase("-wms")) {
					this.wms = args[i+1];
				}
				if (args[i].equalsIgnoreCase("-layer")) {
					this.layer = args[i+1];
				}
			}
		}		
	}

	public String getMatsimConfigFile()
	{
		return matsimConfigFile;
	}

	private void initListeners()
	{
		this.mouseListener = new ArrayList<MouseListener>();
		this.mouseMotionListener = new ArrayList<MouseMotionListener>();
		this.mouseWheelListener = new ArrayList<MouseWheelListener>();
		this.keyListener = new ArrayList<KeyListener>();

		this.slippyListenersAdded = false;
		
	}

	private static final class Runner implements Runnable
	{
		private Controller controller;

		public Runner(Controller controller)
		{
			this.controller = controller;
		}

		@Override
		public void run()
		{
			try
			{
				controller.setVisualizer(new Visualizer(controller));
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public ArrayList<Shape> getActiveShapes()
	{
		return shapes;
	}

	public void setVisualizer(Visualizer visualizer)
	{
		this.visualizer = visualizer;

	}

	public void addRenderLayer(AbstractRenderLayer layer)
	{
		visualizer.addRenderLayer(layer);
	}

	public Visualizer getVisualizer()
	{
		return visualizer;
	}

	public void resetRenderer(boolean leaveMapRenderer)
	{
		this.visualizer.removeAllLayers(leaveMapRenderer);
	}

	public Point getMousePosition()
	{
		return mousePosition;
	}

	public void setMousePosition(Point mousePosition)
	{
		this.mousePosition = mousePosition;

		// boolean foundHover = false;
		// boolean deactivated = false;
		// for (Shape shape : this.getActiveShapes())
		// if ((shape instanceof PolygonShape) &&
		// (!shape.getId().equals(Constants.ID_EVACAREAPOLY)))
		// {
		//
		// if (((PolygonShape) shape).getPixelPolygon().contains(mousePosition))
		// {
		// shape.setHover(true);
		// foundHover = true;
		// }
		// else
		// {
		// deactivated = true;
		// shape.setHover(false);
		// }
		// }
		//
		// if (foundHover)
		// {
		// this.paintLayers();
		// this.getParentComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		// }
		// else
		// {
		// this.getParentComponent().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		// if (deactivated)
		// this.paintLayers();
		// }

	}

	public void setMousePosition(int x, int y)
	{
		this.mousePosition.x = x;
		this.mousePosition.y = y;
	}

	public Rectangle getViewportBounds()
	{
		// if (this.visualizer.getActiveMapRenderLayer()!=null)
		return visualizer.getActiveMapRenderLayer().getViewportBounds();
		// else
		// return null;
	}

	public SelectionMode getSelectionMode()
	{
		return selectionMode;
	}

	public void setSelectionMode(SelectionMode selectionMode)
	{
		this.selectionMode = selectionMode;
	}

	public boolean isHoveringOverPoint()
	{
		return hoveringOverPoint;
	}

	public void setHoveringOverPoint(boolean hoveringOverPoint)
	{
		this.hoveringOverPoint = hoveringOverPoint;
	}

	public boolean isEditMode()
	{
		return editMode;
	}

	public void setEditMode(boolean editMode)
	{
		this.editMode = editMode;
	}

	public ImageContainerInterface getImageContainer()
	{
		return imageContainer;
	}

	public void setImageContainer(ImageContainerInterface imageContainer)
	{
		this.imageContainer = imageContainer;
		
		updateMapLayerImages();
	}
	

	public void updateMapLayerImages()
	{
		if (hasMapRenderer()) 
			this.visualizer.getActiveMapRenderLayer().updateMapImage();
	}

	public void paintLayers()
	{

		if (this.parentComponent != null)
			this.parentComponent.repaint();
		else
			this.visualizer.paintLayers();

	}

	public void setParentComponent(Component parentComponent)
	{
		this.parentComponent = parentComponent;
	}

	public Component getParentComponent()
	{
		return parentComponent;
	}

	public void repaintParent()
	{
		if (this.parentComponent != null)
			this.parentComponent.repaint();
	}

	public void setSlippyMapEventListeners(ArrayList<EventListener> eventListeners)
	{

		// put each listener to the corresponding array list
		initListeners();
		for (EventListener listener : eventListeners)
		{
			if (listener instanceof MouseListener)
			{
				mouseListener.add((MouseListener) listener);
				mouseMotionListener.add((MouseMotionListener) listener);
			} else if (listener instanceof MouseWheelListener)
				mouseWheelListener.add((MouseWheelListener) listener);
			else if (listener instanceof KeyListener)
				keyListener.add((KeyListener) listener);
		}

		if (eventListeners.size() > 0)
			slippyListenersAdded = true;
	}

	public ArrayList<MouseListener> getMouseListener()
	{
		return mouseListener;
	}

	public ArrayList<MouseMotionListener> getMouseMotionListener()
	{
		return mouseMotionListener;
	}

	public ArrayList<MouseWheelListener> getMouseWheelListener()
	{
		return mouseWheelListener;
	}

	public ArrayList<KeyListener> getKeyListener()
	{
		return keyListener;
	}

	public boolean slippyEventListenersAvailable()
	{
		return slippyListenersAdded;
	}

	public void setListener(AbstractListener listener)
	{
		this.listener = listener;
	}

	public AbstractListener getListener()
	{
		return listener;
	}

	public boolean hasMapRenderer()
	{
		return this.visualizer.hasMapRenderer();
	}

	public String getCurrentOSMFile()
	{
		return currentOSMFile;
	}

	public void setCurrentOSMFile(String currentOSMFile)
	{
		this.currentOSMFile = currentOSMFile;
	}

	public File getCurrentWorkingDirectory()
	{
//		return new File("C:\\temp\\!matsimfiles\\fostercityca\\");
		return new File(System.getProperty("user.home"));
	}

	public GripsConfigModule getGripsConfigModule()
	{
		return gripsConfigModule;
	}

	public void setGripsConfigModule(GripsConfigModule gripsConfigModule)
	{
		this.gripsConfigModule = gripsConfigModule;
	}

	public boolean openGripsConfig(File selectedFile)
	{
		try
		{
			this.gripsConfigModule = new GripsConfigModule("grips");
			GripsConfigDeserializer parser = new GripsConfigDeserializer(this.gripsConfigModule);
			parser.readFile(selectedFile.getAbsolutePath());

			this.scenarioPath = selectedFile.getParent();
			this.gripsFile = selectedFile.getAbsolutePath();

			System.out.println("scenarioPath:" + this.scenarioPath);
			System.out.println("gripsfile:" + this.gripsFile);

			this.readOSMFile(this.gripsConfigModule.getNetworkFileName());
			this.setCurrentOSMFile(this.gripsConfigModule.getNetworkFileName());

			if (!standAlone)
				updateOtherModules();

			return true;
		} 
		catch (Exception e)
		{
			System.err.println(e.getMessage());
			return false;
		}
	}

	public String getGripsFile()
	{
		return gripsFile;
	}

	public boolean openGripsConfig()
	{
		DefaultOpenDialog openDialog = new DefaultOpenDialog(this, "xml", locale.infoGripsFile(), true);
		int returnValue = openDialog.showOpenDialog(this.getParentComponent());

		if (returnValue == JFileChooser.APPROVE_OPTION)
			return openGripsConfig(openDialog.getSelectedFile());
		else
			return false;

	}

	public boolean isGripsConfigOpenend()
	{
		return (this.gripsConfigModule != null);

	}

	private void updateOtherModules()
	{

	}

	public boolean readOSMFile(String networkFileName)
	{

		try
		{

			// has a config already been loaded?
			// if (matsimConfig==null)
			{
				matsimConfig = ConfigUtils.createConfig();
				matsimConfig.global().setCoordinateSystem(this.configCoordinateSystem);

				this.targetCoordinateSystem = matsimConfig.global().getCoordinateSystem();
				this.scenario = ScenarioUtils.createScenario(matsimConfig);

			}

			// check if geo tranformation tools are available
			checkGeoTransformationTools();

			// finally read network
			OsmNetworkReader reader = new OsmNetworkReader(this.scenario.getNetwork(), ctOsm2Target, true);
			reader.setKeepPaths(true);
			reader.parse(networkFileName);

			processNetwork(false);

			return true;
		} catch (Exception e)
		{
			System.err.println(e.getMessage());
			return false;
		}

	}

	private void checkGeoTransformationTools()
	{
		// are the transformation tools already at hand?
		if ((ctOsm2Target == null) || (ctTarget2Osm == null))
		{
			this.ctOsm2Target = new GeotoolsTransformation(this.sourceCoordinateSystem, this.targetCoordinateSystem);
			this.ctTarget2Osm = new GeotoolsTransformation(this.targetCoordinateSystem, this.sourceCoordinateSystem);
		}
	}

	private void processNetwork(boolean processLinks)
	{
		// fill envelope with collected roads (links)
		Envelope e = new Envelope();
		for (Node node : this.scenario.getNetwork().getNodes().values())
		{
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
		if (processLinks)
		{
			this.links = new LinkQuadTree(e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY());
			this.linkList = new ArrayList<Link>();
			NetworkImpl net = (NetworkImpl) this.scenario.getNetwork();
			for (Link link : net.getLinks().values())
			{
				// ignore end links
				if (link.getId().toString().contains("el"))
					continue;
				this.links.put(link);
				this.linkList.add(link);
			}
		}

		// transform coordinates
		centerC = ctTarget2Osm.transform(centerC);
		min = ctTarget2Osm.transform(min);
		max = ctTarget2Osm.transform(max);

		// pass center position and bounding box to the controller
		this.centerPosition = new Point2D.Double(centerC.getX(), centerC.getY());
		this.boundingBox = new Rectangle2D.Double(min.getY(), min.getX(), max.getY() - min.getY(), max.getX() - min.getX());
	}

	public CoordinateTransformation getCtOsm2Target()
	{
		return ctOsm2Target;
	}

	public CoordinateTransformation getCtTarget2Osm()
	{
		return ctTarget2Osm;
	}

	public Point2D getCenterPosition()
	{
		return centerPosition;
	}

	public Rectangle2D getBoundingBox()
	{
		return boundingBox;
	}

	public void setMainPanelListeners(boolean removeAllExistingListeners)
	{
		if (this.mainPanel != null)
		{
			if (removeAllExistingListeners)
				removeAllPanelEventListeners();

			this.mainPanel.addMouseListener(getListener());
			this.mainPanel.addMouseMotionListener(getListener());
			this.mainPanel.addKeyListener(getListener());
			this.mainPanel.addMouseWheelListener(getListener());
		}

	}

	public void removeAllPanelEventListeners()
	{
		if (this.mainPanel != null)
		{
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
	 * - scans for shape layers - creates a new shape layer, if no shape layer
	 * found
	 * 
	 * @param shape
	 */
	public void addShape(Shape shape)
	{
		for (int i = 0; i < this.shapes.size(); i++)
		{
			// System.out.println(this.shapes.get(i).getDescription());
			// String existingShapeId = this.shapes.get(i).getId();
			// String shapeId = shape.getId();
			//
			// System.out.println(existingShapeId);
			// System.out.println(shapeId);

			if (this.shapes.get(i).getId().equals(shape.getId()))
			{
				this.shapes.set(i, shape);
				return;
			}
		}
		this.shapes.add(shape);

	}

	public Shape getShapeById(String id)
	{
		for (Shape shape : this.shapes)
			if (shape.getId().equals(id))
				return shape;

		return null;
	}

	public boolean hasShapeRenderer()
	{
		return this.visualizer.hasShapeRenderer();
	}

	public boolean hasSecondaryShapeRenderer()
	{
		System.out.println("has secondary shape layer?" + this.visualizer.hasSecondaryShapeRenderer());
		return this.visualizer.hasSecondaryShapeRenderer();

	}

	public int getZoom()
	{
		if (this.visualizer.getActiveMapRenderLayer() != null)
			return visualizer.getActiveMapRenderLayer().getZoom();

		return 0;
	}

	public Scenario getScenario()
	{
		return scenario;
	}

	public void enableAllRenderLayers()
	{
		if (this.visualizer.getRenderLayers() != null)
			for (AbstractRenderLayer layer : this.visualizer.getRenderLayers())
				layer.setEnabled(true);
	}

	public void enableMapRenderer()
	{
		if (this.visualizer.getActiveMapRenderLayer() != null)
			this.visualizer.getActiveMapRenderLayer().setEnabled(true);
	}
	
	public void disableAllRenderLayers()
	{
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
	public Rectangle geoToPixel(Rectangle2D rectangle)
	{

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
		if (h < 0)
		{
			y += h;
			h = h * -1;
		}
		if (w < 0)
		{
			x += w;
			w = w * -1;
		}

		Rectangle newRect = new Rectangle(x, y, w, h);

		// TODO
		// GeoPosition minGeo = new GeoPosition(rectangle.getMinX(),
		// rectangle.getMinY());
		// GeoPosition maxGeo = new GeoPosition(rectangle.getMaxX(),
		// rectangle.getMaxY());
		//
		// Point2D minPoint =
		// this.visualizer.getActiveMapRenderLayer().geoToPixel(minGeo);
		// Point2D maxPoint =
		// this.visualizer.getActiveMapRenderLayer().geoToPixel(maxGeo);
		//
		// Rectangle newRect = new Rectangle((int)minPoint.getX(),
		// (int)minPoint.getY(), (int)maxPoint.getX(), (int)maxPoint.getY());

		return newRect;
	}

	public Point geoToPixel(Point2D point)
	{
		return this.visualizer.getActiveMapRenderLayer().geoToPixel(point);
	}

	public Point2D pixelToGeo(Point2D point)
	{
		return this.visualizer.getActiveMapRenderLayer().pixelToGeo(point);
	}

	// public Point2D pixelToGeoPoint2D(Point2D point)
	// {
	// GeoPosition
	// return ;
	// }

	public void setZoom(int zoom)
	{
		if (this.visualizer.getActiveMapRenderLayer() != null)
			this.visualizer.getActiveMapRenderLayer().setZoom(zoom);
	}

	public void validateRenderLayers()
	{
		// TODO
		// for (AbstractRenderLayer layer : this.visualizer.getRenderLayers())
		// {
		// // if (l)
		// }

	}

	public void setMainPanel(JPanel mainPanel, boolean updatePanelBounds)
	{
//		System.out.println("pc:" + (this.getParentComponent() != null));
//		System.out.println("instance: " + (!(this.getParentComponent() instanceof DefaultWindow)));

		if ((this.getParentComponent() != null))
			((DefaultWindow) this.getParentComponent()).setMainPanel(mainPanel);

		this.mainPanel = mainPanel;

		if (updatePanelBounds)
			updatePanelBounds();
	}

	public void updatePanelBounds()
	{
		mainPanelBounds = SwingUtilities.convertRectangle(mainPanel.getParent(), mainPanel.getBounds(), this.getParentComponent());
	}

	public JPanel getMainPanel()
	{
		return mainPanel;
	}

	public Rectangle getMainPanelBounds()
	{
		return mainPanelBounds;
	}

	public ShapeUtils getShapeUtils()
	{
		return shapeUtils;
	}

	public String getTargetCoordinateSystem()
	{
		return targetCoordinateSystem;
	}

	public String getSourceCoordinateSystem()
	{
		return sourceCoordinateSystem;
	}

	public String getConfigCoordinateSystem()
	{
		return configCoordinateSystem;
	}

	public boolean saveShape(Shape shape, String fileName)
	{
		if (shape instanceof PolygonShape)
		{
			boolean saved = ShapeIO.savePolygon(this, (PolygonShape) shape, this.gripsConfigModule.getEvacuationAreaFileName());
			if (saved)
				System.out.println("saved polygon to " + this.gripsConfigModule.getEvacuationAreaFileName());
		}

		return true;
	}

	public Locale getLocale()
	{
		return locale;
	}

	public void setLocale(Locale locale)
	{
		this.locale = locale;
	}

	public boolean isStandAlone()
	{
		return standAlone;
	}

	public void setStandAlone(boolean standAlone)
	{
		this.standAlone = standAlone;
	}

	public boolean hasGripsConfig()
	{
		if (this.gripsConfigModule != null)
			return true;
		else
			return false;
	}

	public AbstractToolBox getActiveToolBox()
	{
		return activeToolBox;
	}

	public void setActiveToolBox(AbstractToolBox activeToolBox)
	{
		this.activeToolBox = activeToolBox;
		if ((parentComponent != null))
		{
			// System.out.println("is it " + (parentComponent instanceof
			// DefaultWindow));
			if (parentComponent instanceof DefaultWindow)
			{
				((DefaultWindow) parentComponent).setToolBox(activeToolBox);
			}
		}

	}

	public boolean openEvacuationShape(String id)
	{
		if (this.getShapeById(id) != null)
			return true;

		ShapeStyle evacShapeStyle = Constants.SHAPESTYLE_EVACAREA;
		String dest;

		// grips config is not opened, check for file destination in scenario
		if (this.gripsConfigModule == null)
		{
			if (this.scenario != null)
				dest = this.scenario.getConfig().getModule("grips").getValue("evacuationAreaFile");
			else
				return false;
		} else
			dest = this.gripsConfigModule.getEvacuationAreaFileName();

		return openShape(id, dest, evacShapeStyle);
	}

	public boolean openShape(String id, String fileName, ShapeStyle style)
	{
		try
		{
			PolygonShape evacShape = ShapeIO.getShapeFromFile(this, id, fileName);
			evacShape.setStyle(style);
			addShape(evacShape);
			return true;
		} catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public void removeShape(String id)
	{
		for (int i = 0; i < this.visualizer.getActiveShapes().size(); i++)
		{
			if (this.visualizer.getActiveShapes().get(i).getId().equals(id))
				this.visualizer.getActiveShapes().remove(i);
		}

	}

	public boolean saveShapes(String populationFileName)
	{
		ArrayList<PolygonShape> polygonShapes = new ArrayList<PolygonShape>();

		for (Shape shape : this.getActiveShapes())
			if ((shape instanceof PolygonShape) && (shape.getMetaData(Constants.POPULATION) != null))
				polygonShapes.add((PolygonShape) shape);

		if (polygonShapes.size() > 0)
			return ShapeIO.savePopulationAreaPolygons(this, polygonShapes, populationFileName);

		return false;

	}

	public void setTempLinkId(int n, Id id)
	{
		if (n == 0)
			this.linkID1 = id;
		else if (n == 1)
			this.linkID2 = id;
	}

	public Id getTempLinkId(int n)
	{
		if (n == 0)
			return linkID1;
		else
			return linkID2;

	}

	public boolean isMatsimConfigOpened()
	{
		return (this.matsimConfig != null);
	}

	public boolean openMastimConfig()
	{
		DefaultOpenDialog openDialog = new DefaultOpenDialog(this, "xml", locale.infoMatsimFile(), true);
		int returnValue = openDialog.showOpenDialog(this.getParentComponent());

		if (returnValue == JFileChooser.APPROVE_OPTION)
			return openMastimConfig(openDialog.getSelectedFile());
		else
			return false;
	}

	public boolean openMastimConfig(File file)
	{
		try
		{
			System.out.println("Opening: " + file.getAbsolutePath() + ".");

			this.configFilePath = file.getAbsolutePath();
			this.scenarioPath = file.getParent();

			this.matsimConfigFile = file.getAbsolutePath();
			this.matsimConfigFilePath = file.getParent();

			this.matsimConfig = ConfigUtils.loadConfig(this.matsimConfigFile);
			this.scenario = ScenarioUtils.loadScenario(this.matsimConfig);

			// this.matsimConfig.global().get
			// this.scenario.getConfig().getModule("grips").getValue("populationFile");

			this.targetCoordinateSystem = matsimConfig.global().getCoordinateSystem();

			// check if geo tranformation tools are available
			checkGeoTransformationTools();

			// process the network
			processNetwork(true);

			return true;
		} catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}

		// String shp =
		// this.sc.getConfig().getModule("grips").getValue("evacuationAreaFile");
		// readShapeFile(shp);
		// loadMapView();
	}

	public String getConfigFilePath()
	{
		return configFilePath;
	}

	public String getScenarioPath()
	{
		return scenarioPath;
	}

	public void setConfigCoordinateSystem(String configCoordinateSystem)
	{
		this.configCoordinateSystem = configCoordinateSystem;
	}

	public void setScenarioPath(String scenarioPath)
	{
		this.scenarioPath = scenarioPath;
	}

	public boolean hasGridRenderer()
	{
		return this.visualizer.hasGridRenderer();
	}

	public LinkQuadTree getLinks()
	{
		return links;
	}

	public Point2D coordToPoint(Coord coord)
	{
		coord = ctTarget2Osm.transform(coord);
		return new Point2D.Double(coord.getY(), coord.getX());
	}

	public boolean isPopulationFileOpened()
	{
		return populationFileOpened;
	}

	public void setPopulationFileOpened(boolean populationFileOpened)
	{
		this.populationFileOpened = populationFileOpened;
	}

	public boolean openPopulationFile()
	{
		String dest;

		// grips config is not opened, check for file destination in scenario
		if (this.gripsConfigModule == null)
		{
			if (this.scenario != null)
				dest = this.scenario.getConfig().getModule("grips").getValue("populationFile");
			else
				return false;
		} else
			dest = this.gripsConfigModule.getPopulationFileName();

		ArrayList<PolygonShape> popShapes = ShapeIO.getShapesFromFile(this, dest, Constants.SHAPESTYLE_POPAREA);
		for (PolygonShape shape : popShapes)
			addShape(shape);

		this.populationFileOpened = true;

		return true;
	}

	public boolean openNetworkChangeEvents()
	{
		if (this.scenario != null)
		{
			Collection<NetworkChangeEvent> changeEvents = ((NetworkImpl) this.scenario.getNetwork()).getNetworkChangeEvents();
			int id = visualizer.getPrimaryShapeRenderLayer().getId();

			if (changeEvents != null)
			{
				for (NetworkChangeEvent event : changeEvents)
				{
					Collection<Link> changeEventLinks = event.getLinks();
					for (Link link : changeEventLinks)
					{
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

	public boolean isMainWindowUndecorated()
	{
		return mainWindowUndecorated;
	}

	public void setMainFrameUndecorated(boolean mainWindowUndecorated)
	{
		this.mainWindowUndecorated = mainWindowUndecorated;
	}

	public void addModuleChain(ArrayList<AbstractModule> moduleChain)
	{
		this.modules = moduleChain;
	}

	public ArrayList<AbstractModule> getModules()
	{
		return modules;
	}

	public ArrayList<Link> getLinkList()
	{
		return linkList;
	}

	public void readOSM(boolean b)
	{
		this.readOSM = b;

	}

	public ModuleType getActiveModuleType()
	{
		return activeModuleType;
	}

	public void setActiveModuleType(ModuleType activeModuleType)
	{
		this.activeModuleType = activeModuleType;
	}

	public void setGoalAchieved(boolean goalAchieved)
	{
		if (goalAchieved)
		{
			AbstractModule module = getModuleByType(activeModuleType);
			if (module != null)
				module.enableNextModules();
		}
	}

	public AbstractModule getModuleByType(ModuleType moduleType)
	{
		if (this.modules == null)
			return null;
		for (AbstractModule module : modules)
		{
			if (module.getModuleType().equals(moduleType))
				return module;
		}
		return null;
	}

	public void enableModule(ModuleType moduleType)
	{
		AbstractModule module = getModuleByType(moduleType);
		// System.out.println(module.getModuleType());
		if (module != null)
		{
			// System.out.println("module is enabled: " + module.isEnabled());
			module.setEnabled(true);
		}

	}

	public void updateParentUI()
	{
		if ((parentComponent != null) && (parentComponent instanceof DefaultWindow))
			((DefaultWindow) parentComponent).updateMask();

	}

	public boolean hasDefaultRenderPanel()
	{
		System.out.println(this.getMainPanel() instanceof DefaultRenderPanel);
		return (this.getMainPanel() instanceof DefaultRenderPanel);
	}

	public void setToolBoxVisible(boolean toggle)
	{
		if ((this.getParentComponent() != null) && (this.getParentComponent() instanceof DefaultWindow))
			((DefaultWindow) this.getParentComponent()).setToolBoxVisible(toggle);
	}

	public EventData getEventData()
	{
		return data;
	}

	public void setEventData(EventData data)
	{
		this.data = data;
	}

	public void disableShapeLayers()
	{
		for (AbstractRenderLayer layer : this.visualizer.getRenderLayers())
		{
			if (layer instanceof ShapeRenderer)
				layer.setEnabled(false);
		}

	}

	public void disableGridLayer()
	{
		for (AbstractRenderLayer layer : this.visualizer.getRenderLayers())
		{
			if (layer instanceof GridRenderer)
				layer.setEnabled(false);
		}

	}

	public String getIterationsOutputDirectory()
	{
		return this.scenario.getConfig().getModule("controler").getValue("outputDirectory");
	}
	
	public void exit(String exitString)
	{
		System.out.println(exitString);
		System.exit(0);
	}

	public String getWMS()
	{
		return this.wms;
	}

	public String getWMSLayer()
	{
		return this.layer;
	}
	


}
