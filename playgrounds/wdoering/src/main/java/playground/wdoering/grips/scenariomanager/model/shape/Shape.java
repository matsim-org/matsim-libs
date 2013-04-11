package playground.wdoering.grips.scenariomanager.model.shape;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public abstract class Shape
{
	public static enum DrawMode { FILL, FILL_WITH_CONTOUR, CONTOUR, IMAGE_FILL }
	
	protected String id;
	protected int layerID;
	protected boolean hover = false;
	protected boolean selected = false;
	protected String description;
	protected HashMap<String, String> metaData;
	protected boolean visible = true;
	
	
	protected ShapeStyle style;
	
	public static int currentNumberId = -1;
	
	
	public String getId()
	{
		return id;
	}
	
	public void setId(String id)
	{
		this.id = id;
	}
	
	public void setLayerID(int layerID)
	{
		this.layerID = layerID;
	}
	
	public int getLayerID()
	{
		return layerID;
	}
	
	public boolean isHover()
	{
		return hover;
	}
	
	public boolean isSelected()
	{
		return selected;
	}
	
	public void setHover(boolean hover)
	{
		this.hover = hover;
	}
	
	public void setSelected(boolean selected)
	{
		this.selected = selected;
	}
	
	public void setColor(Color color)
	{
		this.style.setPrimaryColor(color);
	}
	
	public Color getColor()
	{
		return this.style.getPrimaryColor();
	}
	
	public String getDescription()
	{
		return description;
	}
	
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	public HashMap<String, String> getAllMetaData()
	{
		return metaData;
	}
	
	public void setMetaData(HashMap<String, String> metaData)
	{
		this.metaData = metaData;
	}
	
	public void putMetaData(String key, String value)
	{
		if (this.metaData == null)
			this.metaData = new HashMap<String, String>();
		
		this.metaData.put(key, value);
	}
	
	public String getMetaData(String key)
	{
		if (this.metaData == null)
			return null;
		
		return metaData.get(key);
	}
	
	public float getThickness()
	{
		return this.style.getContourThickness();
	}
	
	public void setThickness(float thickness)
	{
		this.style.setContourThickness(thickness);
	}
	
	public DrawMode getDrawMode()
	{
		return this.style.getDrawMode();
	}
	
	public void setDrawMode(DrawMode drawMode)
	{
		this.style.setDrawMode(drawMode);
	}
	
	public Color getContourColor()
	{
		if (this.style.getContourColor()==null)
			this.style.setContourColor(new Color(this.style.getPrimaryColor().getRed(), this.style.getPrimaryColor().getGreen(), this.style.getPrimaryColor().getBlue()));
		
		return this.style.getContourColor();
	}
	
	public void setContourColor(Color contourColor)
	{
		this.style.setContourColor(contourColor);
	}
	
	public void setVisible(boolean visible)
	{
		this.visible = visible;
	}
	
	public boolean isVisible()
	{
		return visible;
	}
	
	public ShapeStyle getStyle()
	{
		return style;
	}
	
	public void setStyle(ShapeStyle style)
	{
		this.style = style;
	}
	
	

}
