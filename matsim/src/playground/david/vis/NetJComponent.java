/* *********************************************************************** *
 * project: org.matsim.*
 * NetJComponent.java
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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import javax.swing.JComponent;

/**
 * @author gunnar
 *
 */
public class NetJComponent extends JComponent {

	private static final long serialVersionUID = 1L;

		private static final double BORDER_FACTOR = 0.0;

    private final OTFVisNet network;

    private final RendererA networkRenderer;

    private final int frameDefaultWidth;

    private final int frameDefaultHeight;


    private double viewMinX, viewMinY, viewMaxX, viewMaxY;

    public void setViewClipCoords( double minX, double minY, double maxX, double maxY) {
    	viewMinX  = networkClippingMinEasting() +  minX * networkClippingWidth();
    	viewMaxX  = networkClippingMinEasting() +  maxX * networkClippingWidth();

    	viewMinY  = networkClippingMinNorthing() + (1.-maxY) * networkClippingHeight();
    	viewMaxY  = networkClippingMinNorthing() + (1.-minY) * networkClippingHeight();
    }

    public void moveViewClipCoords( double deltaX, double deltaY) {
    	viewMinX  += deltaX * networkClippingWidth();
    	viewMaxX  += deltaX * networkClippingWidth();

    	viewMinY  -= deltaY * networkClippingHeight();
    	viewMaxY  -= deltaY * networkClippingHeight();
    }

    // returns something like this
    // 0  1  2
    // 4  5  6
    // 8  9 10
    //
    // so 5 means the cord is IN the clipping region
    public int checkViewClip(double x, double y) {
    	// check for quadrant
    	int xquart = x < viewMinX ? 0 : x > viewMaxX ? 2 : 1;
    	int yquart = y < viewMinY ? 0 : y > viewMaxY ? 2 : 1;
    	return xquart + 4* yquart;
    }

    public boolean checkLineInClip(double sx, double sy, double ex, double ey) {
    	int qstart = checkViewClip(sx,sy);
    	int qend = checkViewClip(ex,ey);

    	// both in same sector, that is not middle sector
    	if ( (qstart == qend) && qstart != 5) return false;
    	// both are either left or right and not in the middle
    	if( (qstart % 4) == ( qend % 4) && (qstart % 4) != 1 ) return false;
    	// both are either top or bottom but not in the middle
    	if( (qstart / 4) == ( qend / 4) && (qstart / 4) != 1 ) return false;

    	return true; // all other cases are possibly visible
    }

    // --------------- CONSTRUCTION ---------------

    public NetJComponent(OTFVisNet network, RendererA networkRenderer) {
        this.network = network;
        this.networkRenderer = networkRenderer;
 
        // calculate size of frame

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double factor = screenSize.getWidth() / networkClippingWidth();
        factor = Math.min(factor, screenSize.getHeight() / networkClippingHeight());
        factor *= 0.8f;

        frameDefaultWidth = (int) Math.floor(networkClippingWidth() * factor);
        frameDefaultHeight = (int) Math.floor(networkClippingHeight() * factor);

        scale(1);
        setViewClipCoords(0,0,1,1);

        // linkWidth = 5;
        // nodeRadius = 5;
        //
        // networkRenderer.setNodeRadius(nodeRadius);
        // networkRenderer.setLinkWidth(linkWidth);
    }

    public void scale(double factor) {
        if (factor > 0) {
            int scaledWidth = (int) Math.round(factor * frameDefaultWidth);
            int scaledHeight = (int) Math.round(factor * frameDefaultHeight);

            this.setPreferredSize(new Dimension(scaledWidth, scaledHeight));
        }
    }

    // -------------------- COORDINATE TRANSFORMATION --------------------

    private double networkClippingEastingBorder() {
        return Math.max(1, BORDER_FACTOR
                * (network.maxEasting() - network.minEasting()));
    }

    private double networkClippingNorthingBorder() {
        return Math.max(1, BORDER_FACTOR
                * (network.maxNorthing() - network.minNorthing()));
    }

    private double networkClippingMinEasting() {
        return 0 - networkClippingEastingBorder();
    }

    private double networkClippingMaxEasting() {
        return network.maxEasting() -network.minEasting() + networkClippingEastingBorder();
    }

    private double networkClippingMinNorthing() {
        return 0 - networkClippingNorthingBorder();
    }

    private double networkClippingMaxNorthing() {
        return network.maxNorthing() - network.minNorthing() + networkClippingNorthingBorder();
    }

    private double networkClippingWidth() {
        return networkClippingMaxEasting() - networkClippingMinEasting();
    }

    private double networkClippingHeight() {
        return networkClippingMaxNorthing() - networkClippingMinNorthing();
    }

    private AffineTransform getBoxTransform() {

        // two original extreme coordinates ...

        double v1 = networkClippingMinEasting();
        double w1 = networkClippingMinNorthing();

        double v2 = networkClippingMaxEasting();
        double w2 = networkClippingMaxNorthing();

        // ... mapped onto two extreme picture coordinates ...

        Dimension prefSize = this.getPreferredSize();

        double x1 = 0;
        double y1 = (int) prefSize.getHeight();

        double x2 = (int) prefSize.getWidth();
        double y2 = 0;

        // ... yields a simple affine transformation without shearing:

        double m00 = (x1 - x2) / (v1 - v2);
        double m02 = x1 - m00 * v1;

        double m11 = (y1 - y2) / (w1 - w2);
        double m12 = y1 - m11 * w1;

        return new AffineTransform(m00, 0.0, 0.0, m11, m02, m12);
    }

    // --------------------
    public Point2D.Double getNetCoord (double x, double y) {
    	Point2D.Double result = new Point2D.Double();
        Dimension prefSize = getPreferredSize();
    	result.x = x /prefSize.width;
    	result.y = 1.- y /prefSize.height;
    	result.x *= (network.maxEasting() - network.minEasting());
    	result.y *= (network.maxNorthing() - network.minNorthing());
    	//result.x += network.minEasting();
    	//result.y += network.minNorthing();

    	return result;
    }
    // -------------------- PAINTING --------------------

    @Override
		public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        boolean useAntiAliasing = false;
        
		if (useAntiAliasing ) {
        	g2.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
        } else {
        	g2.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF));
        }

        // AffineTransform boxTransform = getBoxTransform();
        networkRenderer.render(g2, getBoxTransform());
    }

}