/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.vsptelematics.jamfromnowhere;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
/**
 * 
 * from http://www.cap-lore.com/code/java/JavaPixels.html
 * 
 * @author nagel
 *
 */
class PixelExample {
	static final int XX = 380, YY = 250;

	static public void main(String[] args){
		final BufferedImage img = new BufferedImage(XX, YY, BufferedImage.TYPE_INT_RGB);

		@SuppressWarnings("serial")
		final Canvas canvas = new Canvas(){
			@Override
			public void paint(Graphics g)
			{
				g.drawImage(img, 0, 0, Color.red, null);
			}	  
		};
		
		Frame f = new Frame( "paint Example" );
		f.add("Center", canvas);
		f.setSize(new Dimension(XX,YY+22));
		f.setVisible(true);

		for (int i = 0; i<XX; ++i) { 
			for(int j=0; j<YY; ++j) {
				img.setRGB(i, j, 0xffffff);
			}
			canvas.repaint(); 
		}


	}
}
