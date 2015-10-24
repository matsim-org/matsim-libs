/* *********************************************************************** *
 * project: org.matsim.*
 * ColorTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.io;

import org.matsim.contrib.socnetgen.sna.graph.spatial.io.ColorUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

/**
 * @author illenberger
 * 
 */
public class ColorTest extends JComponent {

	// /**
	// * @param args
	// * @throws IOException
	// */
	// public static void main(String[] args) throws IOException {
	// // TODO Auto-generated method stub
	//
	// BufferedWriter writer = new BufferedWriter(new
	// FileWriter("/Users/jillenberger/Desktop/test.net"));
	//
	// int num = 100;
	// double stepSize = 1/(double)num;
	//
	// writer.write("*Vertices " + num);
	// writer.newLine();
	//
	// for(int count = 1; count <= num; count++) {
	// Color c = ColorUtils.getGRBColor((count+1)/(double)(num+2));
	// String cStr = Integer.toHexString(c.getRGB() & 0x00ffffff);
	// writer.write(String.format("%1$s \"%2$s\" %4$s 0 0 ic #%3$s", count,
	// count, cStr, count));
	// writer.newLine();
	// }
	//
	//
	// writer.close();
	// }

	BufferedImage bufferedImage;

	public void initialize() {
		int wd = getSize().width;
		int ht = getSize().height;
		
		int[] data = new int[wd * ht];
		
		int prod = wd;
		int index = 0;
		for (int j = 1; j <= ht; j++) {
			for (int k = 1; k <= wd; k++) {
				double val = (k)/(double)prod;
				Color c = ColorUtils.getGRBColor(val);
				data[index++] = (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
			}
		}
		
		bufferedImage = new BufferedImage(wd, ht, BufferedImage.TYPE_INT_RGB);
		bufferedImage.setRGB(0, 0, wd, ht, data, 0, wd);
	}

	public void paint(Graphics g) {
		if (bufferedImage == null)
			initialize();
		g.drawImage(bufferedImage, 0, 0, this);
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("Show Rainbow Colors");
		frame.getContentPane().add(new ColorTest());
		frame.setSize(450, 300);
		frame.setLocation(100, 100);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				System.exit(0);
			}
		});
		frame.setVisible(true);
	}
}
