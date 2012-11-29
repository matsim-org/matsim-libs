package others.sergioo.visUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map.Entry;
import java.util.SortedMap;

public class ScaleColor {

	//Attributes
	
	//Methods
	public static Color getScaleColor(SortedMap<Float, Color> colorPosition, float proportion) {
		if(colorPosition==null || colorPosition.size()==0)
			return Color.BLACK;
		else {
			Entry<Float, Color> currentColor = colorPosition.entrySet().iterator().next();
			for(Entry<Float, Color> color:colorPosition.entrySet()) {
				if(proportion<color.getKey()) {
					float rate = color.getKey()==currentColor.getKey()?0.5f:(proportion-currentColor.getKey())/(color.getKey()-currentColor.getKey());
					return new Color((int)((1-rate)*currentColor.getValue().getRed()+rate*color.getValue().getRed()), (int)((1-rate)*currentColor.getValue().getGreen()+rate*color.getValue().getGreen()), (int)((1-rate)*currentColor.getValue().getBlue()+rate*color.getValue().getBlue()));
				}
				currentColor = color;
			}
			return currentColor.getValue();
		}
	}
	public static Color getScaleColor(SortedMap<Float, Color> colorPosition, float proportion, int alpha) {
		Color c = getScaleColor(colorPosition, proportion);
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}
	public static void paintScale(SortedMap<Float, Color> colorPosition, Graphics2D g2, int x, int y, int w, int l, Font font, Color textColor, double minValue, double maxValue, int numNumbers) {
		g2.setStroke(new BasicStroke(1));
		for(int i=x; i<w+x; i++) {
			g2.setColor(getScaleColor(colorPosition, (i-x)/(float)w));
			g2.drawLine(i, y, i, y+l);
		}
		double interval = (maxValue-minValue)/(numNumbers-1);
		NumberFormat format = new DecimalFormat("######");
		g2.setColor(textColor);
		g2.setFont(font);
		for(int i=0; i<numNumbers; i++) {
			g2.drawLine(x+i*(w/(numNumbers-1)), y+l, x+i*(w/(numNumbers-1)), y+l+font.getSize()/2);
			String number = format.format(minValue+interval*i);
			g2.drawString(number, x+i*(w/(numNumbers-1))-number.length()*font.getSize()/4, y+l+3*font.getSize()/2);
		}
	}
	public static void paintLogScale(SortedMap<Float, Color> colorPosition, Graphics2D g2, int x, int y, int w, int l, Font font, Color textColor, double minValue, double maxValue, int numNumbers) {
		g2.setStroke(new BasicStroke(1));
		for(int i=x; i<w+x; i++) {
			g2.setColor(getScaleColor(colorPosition, (i-x)/(float)w));
			g2.drawLine(i, y, i, y+l);
		}
		NumberFormat format = new DecimalFormat("######");
		g2.setColor(textColor);
		g2.setFont(font);
		for(int i=0; i<numNumbers; i++) {
			g2.drawLine(x+i*(w/(numNumbers-1)), y+l, x+i*(w/(numNumbers-1)), y+l+font.getSize()/2);
			String number = format.format(minValue+Math.exp(i*Math.log(maxValue-minValue+1)/(numNumbers-1))-1);
			g2.drawString(number, x+i*(w/(numNumbers-1))-number.length()*font.getSize()/4, y+l+3*font.getSize()/2);
		}
	}

}
