package playground.wdoering.grips.scenariomanager.view;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import playground.wdoering.grips.scenariomanager.model.Constants.ModuleType;

public class TabButton extends JButton
{
	private String title;
	private ModuleType moduleType;
	private JPanel backgroundPanel;
	
//	public static enum 
	
	private Color color = Color.white;  
	private Color hoverColor = Color.white;  

	public TabButton(ModuleType moduleType, JPanel backgroundPanel, int width, int height)
	{
		super("");
//		super(moduleType.toString().toLowerCase());
		
		this.moduleType = moduleType;
		this.backgroundPanel = backgroundPanel;
		
		this.setForeground(Color.BLACK);
		this.setBackground(Color.WHITE);
//		Border line = new LineBorder(Color.gray,4);
//		Border margin = new LineBorder(Color.DARK_GRAY,4);
//		Border margin = new EmptyBorder(20, 15, 0, 15);
//		Border margin = new EmptyBorder(20, 15, 0, 15);
//		Border compound = new CompoundBorder(line, margin);
//		this.setBorder(compound);
		this.setBorder(null);

		this.setPreferredSize(new Dimension(width, height));
		
	}
	
	public Color getHoverColor()
	{
		return hoverColor;
	}
	
	public void setHoverColor(Color hoverColor)
	{
		this.hoverColor = hoverColor;
	}
	
	public Color getColor()
	{
		return color;
	}
	
	public void setColor(Color color)
	{
		this.color = color;
		this.getBackgroundPanel().setBackground(color);
	}
	
	public JPanel getBackgroundPanel()
	{
		return backgroundPanel;
	}
	
	public ModuleType getModuleType()
	{
		return moduleType;
	}

	public void hover(boolean toggle)
	{
		if (this.isEnabled())
		{
			if (toggle)
				this.getBackgroundPanel().setBackground(hoverColor);
			else
				this.getBackgroundPanel().setBackground(color);
		}
		
	}
	
	@Override
	public void setEnabled(boolean b)
	{
		super.setEnabled(b);
		
		if (!b)
			this.getBackgroundPanel().setBackground(Color.gray);
	}

}
