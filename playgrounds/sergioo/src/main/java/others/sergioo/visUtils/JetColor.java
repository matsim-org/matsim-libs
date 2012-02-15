package others.sergioo.visUtils;

import java.awt.Color;

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
}
