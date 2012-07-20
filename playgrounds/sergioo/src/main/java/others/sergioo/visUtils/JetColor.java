package others.sergioo.visUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class JetColor {

	//Attributes

	//Methods
	public static Color getJetColor(float proportion) {
		float r=0, g=0, b=0, slope = 4f;
		if(proportion<0.125) {
			r = 1;
			g = 0;
			b = -slope*(proportion+.125f)+1;
		}
		else if(proportion<0.25) {
			r = 1;
			g = slope*(proportion-.375f)+1;
			b = 0;
		}
		else if(proportion<0.375) {
			r = -slope*(proportion-.25f)+1;
			g = slope*(proportion-.375f)+1;
			b = 0;
		}
		else if(proportion<0.5) {
			r = -slope*(proportion-.25f)+1;
			g = 1;
			b = 0;
		}
		else if(proportion<0.625) {
			r = 0;
			g = 1;
			b = slope*(proportion-.75f)+1;
		}
		else if(proportion<0.75) {
			r = 0;
			g = -slope*(proportion-.625f)+1;
			b = slope*(proportion-.75f)+1;
		}
		else if(proportion<0.875) {
			r = 0;
			g = -slope*(proportion-.625f)+1;
			b = 1;
		}
		else {
			r = slope*(proportion-1.125f)+1;
			g = 0;
			b = 1;
		}
		try {
		return new Color(r, g, b);
		} catch(Exception e) {
			return null;
		}
	}

	public static void paintScale(Graphics2D g2, int x, int y, int w, int l, Font font, Color textColor, double minValue, double maxValue, int numNumbers) {
		g2.setStroke(new BasicStroke(1));
		for(int i=x; i<w+x; i++) {
			g2.setColor(getJetColor((i-x)/(float)w));
			g2.drawLine(i, y, i, y+l);
		}
		double interval = (maxValue-minValue)/(numNumbers-1);
		NumberFormat format = new DecimalFormat("######");
		g2.setColor(textColor);
		g2.setFont(font);
		for(int i=0; i<numNumbers; i++) {
			g2.drawLine(x+i*(w/(numNumbers-1)), y+l, x+i*(w/(numNumbers-1)), y+l+font.getSize()/2);
			g2.drawString(format.format(minValue+interval*i), x+i*(w/(numNumbers-1)), y+l+3*font.getSize()/2);
		}
	}

	public static Color getJetColor(float proportion, float alpha) {
		float r=0, g=0, b=0, slope = 4f;
		if(proportion<0.125) {
			r = 1;
			g = 0;
			b = -slope*(proportion+.125f)+1;
		}
		else if(proportion<0.25) {
			r = 1;
			g = slope*(proportion-.375f)+1;
			b = 0;
		}
		else if(proportion<0.375) {
			r = -slope*(proportion-.25f)+1;
			g = slope*(proportion-.375f)+1;
			b = 0;
		}
		else if(proportion<0.5) {
			r = -slope*(proportion-.25f)+1;
			g = 1;
			b = 0;
		}
		else if(proportion<0.625) {
			r = 0;
			g = 1;
			b = slope*(proportion-.75f)+1;
		}
		else if(proportion<0.75) {
			r = 0;
			g = -slope*(proportion-.625f)+1;
			b = slope*(proportion-.75f)+1;
		}
		else if(proportion<0.875) {
			r = 0;
			g = -slope*(proportion-.625f)+1;
			b = 1;
		}
		else {
			r = slope*(proportion-1.125f)+1;
			g = 0;
			b = 1;
		}
		try {
		return new Color(r, g, b, alpha);
		} catch(Exception e) {
			return null;
		}
	}
}
