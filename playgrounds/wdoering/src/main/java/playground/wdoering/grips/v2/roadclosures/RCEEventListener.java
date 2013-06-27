package playground.wdoering.grips.v2.roadclosures;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkQuadTree;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.ShapeFactory;
import playground.wdoering.grips.scenariomanager.control.eventlistener.AbstractListener;
import playground.wdoering.grips.scenariomanager.model.Constants;
import playground.wdoering.grips.scenariomanager.model.shape.LineShape;

public class RCEEventListener extends AbstractListener
{

	private boolean editMode = false;
	private boolean freezeMode = false;
	private ArrayList<Link> currentHoverLinks;
	private Rectangle viewPortBounds;
	private int offsetX;
	private int offsetY;
	private int border;
	
	private LineShape hoverLine;
	private LineShape primarySelectLine;
	private LineShape secondarySelectLine;
	private Link hoverLink;

	public RCEEventListener(Controller controller)
	{
		super(controller);
		
		this.border = controller.getImageContainer().getBorderWidth();
		this.offsetX = this.border;
		this.offsetY = this.border;
		
//		this.hoverLine = new LineShape(controller.getVisualizer().getPrimaryShapeRenderLayer().getId(), controller.getCenterPosition(), controller.getCenterPosition());
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{

		// if left mouse button was clicked
		if (e.getButton() == MouseEvent.BUTTON1)
		{
			// if edit mode is off
			if (!this.controller.isEditMode())
			{
				// if there was no prior selection
				if (!this.freezeMode)
				{
					// activate edition mode (in gui)
					this.controller.setEditMode(true);

					if (hoverLink!=null)
					{
						// links are being selected. Freeze the selection
						this.freezeMode = true;

						// give gui the id of the first selected link
						this.controller.setTempLinkId(0, hoverLink.getId());
						
						this.primarySelectLine.setC0(this.hoverLine.getC0());
						this.primarySelectLine.setC1(this.hoverLine.getC1());
						this.primarySelectLine.setVisible(true);
						
						//check for the secondary link
						boolean found = false;
						for (Link revL : hoverLink.getToNode().getOutLinks().values())
						{
							if (revL.getToNode() == hoverLink.getFromNode())
							{
								this.controller.setTempLinkId(1, revL.getId());
								this.secondarySelectLine.setC0(this.hoverLine.getC1());
								this.secondarySelectLine.setC1(this.hoverLine.getC0());
								this.secondarySelectLine.setVisible(true);
								found = true;
								break;
							}
						}
						if (!found)
						{
							this.controller.setTempLinkId(1, null);
							this.secondarySelectLine.setVisible(false);
						}
						
					}
					else
					{
						// if nothing is selected, set them null
						this.controller.setTempLinkId(0, null);
						this.controller.setTempLinkId(1, null);
					}
				}
				else
				{
					this.controller.setEditMode(false);
					this.freezeMode = false;
				}
				
			}
			else
			{
				this.primarySelectLine.setVisible(false);
				this.secondarySelectLine.setVisible(false);
				this.controller.setEditMode(false);
			}
			
			this.controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(this.primarySelectLine);
			this.controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(this.secondarySelectLine);
			this.controller.getActiveToolBox().updateMask();
			this.controller.paintLayers();
		}

	}

	@Override
	public void mouseReleased(MouseEvent e)
	{

//		if (!this.controller.isEditMode())
			super.mouseReleased(e);

//		if (e.getButton() == MouseEvent.BUTTON1)
//			controller.paintLayers();

	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		super.mouseWheelMoved(e);
		controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(true);
	}


	@Override
	public void mouseMoved(MouseEvent arg0)
	{

		if (!this.controller.isEditMode())
		{
			Coord wCoord = getGeoPoint(arg0.getPoint());
			
			LinkQuadTree links = controller.getLinks();
			if (links != null)
			{
				hoverLink = controller.getLinks().getNearest(wCoord.getX(),wCoord.getY());
				
//				Coord from = this.controller.getCtTarget2Osm().transform(hoverLink.getFromNode().getCoord());
//				Coord to = this.controller.getCtTarget2Osm().transform(hoverLink.getToNode().getCoord());
				
				Point2D from2D = this.controller.coordToPoint(hoverLink.getFromNode().getCoord()); 
				Point2D to2D = this.controller.coordToPoint(hoverLink.getToNode().getCoord()); 
	
//				Point2D from2D = this.controller.geoToPixel(new Point2D.Double(from.getY(),from.getX()));
//				Point2D to2D = this.controller.geoToPixel(new Point2D.Double(to.getY(),to.getX()));
				
//				System.out.println("- - ");
//				System.out.println("from: " + from2D + " | " + viewPortBounds.x);
//				System.out.println("to: " + to2D + " | "  + viewPortBounds.y);
//				System.out.println();
				
				this.hoverLine.setC0(from2D);
				this.hoverLine.setC1(to2D);
				this.hoverLine.setVisible(true);
				this.controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(this.hoverLine);
				
				this.controller.paintLayers();
			}
			
		}
	}



	private Coord getGeoPoint(Point point)
	{
		updateMousePosition(getGeoPixelPoint(point));
		Point2D geoMousePos = controller.pixelToGeo(controller.getMousePosition());
		
		Coord wCoord = new CoordImpl(geoMousePos.getY(), geoMousePos.getX()); // *********** TODO
		wCoord = this.controller.getCtOsm2Target().transform(wCoord);
		return wCoord;
	}
	
	public Point getGeoPixelPoint(Point mousePoint)
	{
		viewPortBounds = this.controller.getViewportBounds();
		return new Point(mousePoint.x+viewPortBounds.x-offsetX,mousePoint.y+viewPortBounds.y-offsetY);
	}	
	
	@Override
	public void init() {
		
		System.out.println("initializing RCE");
		
		//initialize 3 elements
		int layerID = controller.getVisualizer().getSecondaryShapeRenderLayer().getId();
		this.hoverLine = ShapeFactory.getHoverLineShape(layerID, controller.getCenterPosition(), controller.getCenterPosition()); 
		this.primarySelectLine = ShapeFactory.getPrimarySelectedLineShape(layerID, controller.getCenterPosition(), controller.getCenterPosition()); 
		this.secondarySelectLine = ShapeFactory.getSecondarySelectedLineShape(layerID, controller.getCenterPosition(), controller.getCenterPosition());
		this.hoverLine.setVisible(false);
		this.primarySelectLine.setVisible(false);
		this.secondarySelectLine.setVisible(false);
		this.controller.addShape(this.hoverLine);
		this.controller.addShape(this.primarySelectLine);
		this.controller.addShape(this.secondarySelectLine);
		
		this.editMode = false;
		this.freezeMode = false;
		
	}

}
