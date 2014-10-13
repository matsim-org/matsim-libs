/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.pieter.events;

import java.awt.*;


class NeuronSoma extends Canvas {
  private final double[] Weight ;
  private double Threshold;
  
  NeuronSoma () {
    super ();
    int Count = 2;
    setSize (200, 200);
    setBackground (Color.yellow);
    Weight = new double[Count];
    Threshold = -2;
    Weight [0] = -2;
    Weight [1] = -2;
  }
  
  public int Row (double Y) {
    return (int) (100 - 50 * Y);
  }
  
  public int Col (double X) {
    return (int) (100 + 50 * X);
  }
  
  public int X (double Y) {
    return (int) (Threshold / Weight [0]
           + Y * Weight[1] / Weight [0]);
  }
  
  private void paintPoints (Graphics g) {
    g.setColor (Color.blue);
    g.fillOval (146, 46, 9, 9);
    g.setColor (Color.red);
    g.drawOval (96, 96, 9, 9);
    g.drawOval (96, 46, 9, 9);
    g.drawOval (146, 96, 9, 9);
  }    
  
  public void paintAxes (Graphics g) {
    g.setColor (Color.black);
    g.drawLine (0, 100, 200, 100);
    g.drawLine (100, 0, 100, 200);
    g.drawLine (50, 90, 50, 110);
    g.drawLine (150, 90, 150, 110);
    g.drawLine (90, 50, 110, 50);
    g.drawLine (90, 150, 110, 150);
    g.drawString ("x1", 175, 95);
    g.drawString ("x2", 105, 15);
  }

public double[] getWeight() {
	return Weight;
}

public void setWeight(int i,double weight) {
	Weight[i] = weight;
}

public double getThreshold() {
	return Threshold;
}

public void setThreshold(double threshold) {
	Threshold = threshold;
}
}