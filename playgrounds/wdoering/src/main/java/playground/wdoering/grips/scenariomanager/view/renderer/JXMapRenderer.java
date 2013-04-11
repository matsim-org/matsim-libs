package playground.wdoering.grips.scenariomanager.view.renderer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.Random;

import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.matsim.contrib.grips.jxmapviewerhelper.TileFactoryBuilder;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.JXMap;
import playground.wdoering.grips.scenariomanager.model.imagecontainer.ImageContainerInterface;

public class JXMapRenderer extends AbstractSlippyMapRenderLayer
{
	
	private JXMap mapViewer;
	private BufferedImage mapImage;
	private Graphics mapGraphics;

	public JXMapRenderer(Controller controller, String wms, String layer)
	{
		super(controller);
		
//		wms = "http://localhost:8080/geoserver/wms?service=WMS&";
//		layer = "berlingrouplayer";
		
		//create image to use for export
		mapImage = new BufferedImage(imageContainer.getWidth(), imageContainer.getHeight(), BufferedImage.TYPE_INT_ARGB);
		mapGraphics = mapImage.getGraphics();
		
		//create a new JXMapviewer frame
		mapViewer = new JXMap(controller);
		mapViewer.setBounds(0,0,imageContainer.getWidth(),imageContainer.getHeight());
		
		//add new tile factory, depending on input
		TileFactory tileFactory;
		if (wms == null)
			tileFactory = TileFactoryBuilder.getOsmTileFactory();
		else
			tileFactory = TileFactoryBuilder.getWMSTileFactory(wms, layer);
		
		mapViewer.setTileFactory(tileFactory);
		
		mapViewer.setPanEnabled(true);
		mapViewer.setZoomEnabled(true);
		mapViewer.setCenterPosition(new GeoPosition(controller.getCenterPosition().getY(), controller.getCenterPosition().getX()));
//		mapViewer.setCenterPosition(new GeoPosition(52.517579,13.399701));
		mapViewer.setZoom(2);
		
		System.out.println("map viewer started");
		
	}
	
	@Override
	public void paintLayer()
	{
		if (enabled)
		{
			mapViewer.paint(mapGraphics);
			imageContainer.drawBufferedImage(0, 0, mapImage);
		}
	}
	
	@Override
	public ArrayList<EventListener> getInheritedEventListeners()
	{
		return mapViewer.getEventListeners();
	}
	
	@Override
	public Rectangle getViewportBounds()
	{
		return mapViewer.getViewportBounds();
	}
	
	@Override
	public int getZoom()
	{
		return mapViewer.getZoom();
	}
	
	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		this.mapViewer.setEnabled(true);
		this.mapViewer.repaint();
	}
	
	@Override
	public Point geoToPixel(Point2D point)
	{
		Point2D point2D = this.mapViewer.getTileFactory().geoToPixel(new GeoPosition(point.getX(), point.getY()), this.mapViewer.getZoom());
		return new Point((int)point2D.getX(), (int)point2D.getY());
	}
	
	
	@Override
	public Point2D pixelToGeo(Point2D point)
	{
		GeoPosition geoPos = this.mapViewer.getTileFactory().pixelToGeo(point, this.mapViewer.getZoom());
		return new Point2D.Double(geoPos.getLatitude(),geoPos.getLongitude());
	}
	
	@Override
	public void setZoom(int zoom)
	{
		this.mapViewer.setZoom(zoom);
	}
	
	@Override
	public Rectangle getBounds()
	{
		return this.mapViewer.getBounds();

	}
	
	public void setPosition(Point2D position)
	{
		this.mapViewer.setCenterPosition(new GeoPosition(position.getY(), position.getX()));
//		this.mapViewer.setCenterPosition(new GeoPosition(controller.getCenterPosition().getY(), controller.getCenterPosition().getX()));
	}
	
	

}
