/* *********************************************************************** *
 * project: org.matsim.*
 * NetVisScrollPane.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.david.vis;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JScrollPane;



public class NetVisScrollPane extends JScrollPane{

	private NetJComponent networkComponent;
	
	public NetVisScrollPane(NetJComponent networkComponent) {
		super(networkComponent);
		this.networkComponent = networkComponent;
	}
    public void updateViewClipRect() {
        Dimension prefSize = networkComponent.getPreferredSize();
        Rectangle rect = getViewport().getViewRect();

        double relX = rect.getX() / prefSize.getWidth();
        double relY = rect.getY() / prefSize.getHeight();

        networkComponent.setViewClipCoords(relX, relY, relX
                + rect.getWidth() / prefSize.getWidth(), relY
                + rect.getHeight() / prefSize.getHeight());
    }

    public void moveNetwork(int deltax, int deltay) {
        Rectangle rect = getViewport().getViewRect();
        rect.setLocation(rect.x + deltax, rect.y + deltay);
        getViewport().scrollRectToVisible(rect);
        getViewport().setViewPosition(rect.getLocation());
    }

  
    public float scaleNetwork(Rectangle destrect, float factor) {
        Dimension prefSize = networkComponent.getPreferredSize();
        Rectangle rect = getViewport().getViewRect();
        double relX = (destrect.getX() + rect.getX()) / prefSize.getWidth();
        double relY = (destrect.getY() + rect.getY()) / prefSize.getHeight();

        factor *=  Math.min((double) rect.width / destrect.width,
                        (double) rect.height / destrect.height);

        networkComponent.scale(factor);
        networkComponent.revalidate();
        Dimension prefSize2 = networkComponent.getPreferredSize();
        networkComponent.setViewClipCoords(relX, relY, relX
                + rect.getWidth() / prefSize2.getWidth(), relY
                + rect.getHeight() / prefSize2.getHeight());
        Point erg = new Point();
        erg.x = (int) (relX * prefSize2.getWidth());
        erg.y = (int) (relY * prefSize2.getHeight());
        rect.setLocation(erg.x, erg.y);
        getViewport().scrollRectToVisible(rect);
        getViewport().setViewPosition(erg);
        getViewport().toViewCoordinates(erg);
        revalidate();

        repaint();
        return factor;
    }

    public void scaleNetwork(float factor) {
        Dimension prefSize = networkComponent.getPreferredSize();
        Rectangle rect = getViewport().getViewRect();
        double relX = rect.getX() / prefSize.getWidth();
        double relY = rect.getY() / prefSize.getHeight();
        networkComponent.scale(factor);
        networkComponent.revalidate();
        Dimension prefSize2 = networkComponent.getPreferredSize();
        networkComponent.setViewClipCoords(relX, relY, relX
                + rect.getWidth() / prefSize2.getWidth(), relY
                + rect.getHeight() / prefSize2.getHeight());
        rect.x = (int) (relX * prefSize2.getWidth() + 0.5 * (rect.getWidth() * (prefSize2
                .getWidth()
                / prefSize.getWidth() - 1.)));
        rect.y = (int) (relY * prefSize2.getHeight() + 0.5 * (rect.getHeight() * (prefSize2
                .getHeight()
                / prefSize.getHeight() - 1.)));

        getViewport().setViewPosition(rect.getLocation());

        revalidate();
        repaint();
    }

}
