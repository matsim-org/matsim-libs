/**
 * 
 */
package org.matsim.vis.otfvis.gui;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.NoninvertibleTransformException;

import javax.swing.JScrollPane;

public class MyNetVisScrollPane extends JScrollPane {

	private final OTFSwingDrawer otfSwingDrawer;

	private static final long serialVersionUID = 1L;

	public MyNetVisScrollPane(OTFSwingDrawer networkComponent) {
		super(networkComponent);
		this.otfSwingDrawer = networkComponent;
	}

	public void moveNetwork(int deltax, int deltay) {
		Point viewPosition = getViewport().getViewPosition();
		viewPosition.move((int) viewPosition.getX() + deltax, (int) viewPosition.getY() + deltay);
		getViewport().setViewPosition(viewPosition);
		revalidate();
	}

	public void scaleNetwork(double factor){
		Rectangle viewRect = getViewport().getViewRect();
		Point p = viewRect.getLocation(); // top-left
		p.translate(viewRect.width / 2, viewRect.height / 2); // center 
		try {
			otfSwingDrawer.getBoxTransform().inverseTransform(p, p); // center in world coordinates
		} catch (NoninvertibleTransformException e) {
			throw new RuntimeException(e);
		}
		this.otfSwingDrawer.scale(factor);
		System.out.println("Scale " + this.otfSwingDrawer.scale);
		otfSwingDrawer.getBoxTransform().transform(p, p);
		p.translate(- viewRect.width / 2, - viewRect.height / 2); // center 
		getViewport().setViewPosition(p);
		revalidate();
	}

	public double scaleNetwork(Rectangle destrect, float factor) {
		Rectangle viewRect = getViewport().getViewRect();
		Point p = viewRect.getLocation(); // top-left
		p.translate(viewRect.width / 2, viewRect.height / 2); // center 
		Point destP = destrect.getLocation(); // top-left
		destP.translate(destrect.width / 2, destrect.height / 2); // center 
		moveNetwork(destP.x - p.x, destP.y - p.y);
		double scaleUp = Math.min(viewRect.width / destrect.width, viewRect.height / destrect.height);
		double newScale = scaleUp * factor;
		scaleNetwork(newScale);
		return newScale;
	}
	
}