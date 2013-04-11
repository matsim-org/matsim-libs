package playground.wdoering.grips.scenariomanager.model.shape;

import java.awt.Color;

import playground.wdoering.grips.scenariomanager.model.shape.Shape.DrawMode;

public class ShapeStyle
{
	private Color primaryColor;
	private Color hoverColor;
	private Color contourColor;
	private Color hoverContourColor;
	private Color selectColor;
	private float contourThickness;
	private DrawMode drawMode;

	
	public ShapeStyle(Color primaryColor, Color contourColor, float contourThickness, DrawMode drawMode)
	{
		
		this.primaryColor = this.hoverColor = primaryColor;
		this.contourColor = contourColor;
		this.contourThickness = contourThickness;
		this.drawMode = drawMode;
	}
	

	
	public void setPrimaryColor(Color primaryColor)
	{
		this.primaryColor = primaryColor;
	}
	
	public void setHoverColor(Color hoverColor)
	{
		this.hoverColor = hoverColor;
	}
	
	public void setContourColor(Color contourColor)
	{
		this.contourColor = contourColor;
	}
	
	public void setContourThickness(float contourThickness)
	{
		this.contourThickness = contourThickness;
	}
	
	public void setHoverContourColor(Color hoverContourColor)
	{
		this.hoverContourColor = hoverContourColor;
	}

	public DrawMode getDrawMode()
	{
		return drawMode;
	}

	public void setDrawMode(DrawMode drawMode)
	{
		this.drawMode = drawMode;
	}

	public Color getPrimaryColor()
	{
		return primaryColor;
	}

	public Color getHoverColor()
	{

		
		return hoverColor;
	}

	public Color getContourColor()
	{
		return contourColor;
	}

	public Color getHoverContourColor()
	{
		return hoverContourColor;
	}

	public float getContourThickness()
	{
		return contourThickness;
	}
	
	public void setSelectColor(Color selectColor)
	{
		this.selectColor = selectColor;
	}
	
	public Color getSelectColor()
	{
		return selectColor;
	}
	
	
	
	
}
