package playground.wdoering.grips.evacuationanalysis.control.vis;

import java.awt.Color;
import java.awt.Graphics;

import playground.wdoering.grips.evacuationanalysis.data.Cell;
import playground.wdoering.grips.evacuationanalysis.data.ColorationMode;

public class Coloration
{

	public static Color getColor(double value, ColorationMode mode, float alpha)
	{
		Color color;
		int alphaInt = (int)(255*alpha);
		
			//depending on the selected colorization, set red, green and blue values
			if (mode.equals(ColorationMode.GREEN_YELLOW_RED))
			{
				int red,green,blue;
				
				if (value>.5)
				{
					red = 255;
					green = (int)(255 - 255*(value-.5)*2);
					blue = 0;
				}
				else
				{
					red = (int)(255*value*2);
					green = 255;
					blue = 0;
					
				}
				color = new Color(red,green,blue,alphaInt);
			}
			else if (mode.equals(ColorationMode.GREEN_RED))
			{
				int red,green,blue;
				
				red = (int)(255*value);
				green = (int)(255 - 255*value);
				blue = 0;
				
				color = new Color(red,green,blue,alphaInt);
			}
			else
				color = new Color(0,127,(int)(255*value),alphaInt);

		
		return color;
	}
	
}
