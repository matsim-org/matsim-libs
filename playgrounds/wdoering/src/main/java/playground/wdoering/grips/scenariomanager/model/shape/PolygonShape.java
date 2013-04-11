package playground.wdoering.grips.scenariomanager.model.shape;

import java.awt.Color;

import com.vividsolutions.jts.geom.Polygon;


public class PolygonShape extends Shape
{
	private Polygon polygon;
	private java.awt.Polygon pixelPolygon;
	
	public PolygonShape(int layerID, Polygon polygon)
	{
		this.layerID = layerID;
		this.polygon = polygon;
		
		this.id = (++this.currentNumberId) + "_poly";

	}
	
	public Polygon getPolygon()
	{
		return polygon;
	}
	
	public java.awt.Polygon getPixelPolygon()
	{
		return pixelPolygon;
	}
	
	public void setPixelPolygon(java.awt.Polygon pixelPolygon)
	{
		this.pixelPolygon = pixelPolygon;
	}

	public void setPolygon(Polygon polygon)
	{
		this.polygon = polygon;
	}
	


}
