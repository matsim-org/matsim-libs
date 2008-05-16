package org.matsim.utils.vis.otfivs.interfaces;

import java.awt.Component;
import java.awt.geom.Point2D;
import java.rmi.RemoteException;

import org.matsim.utils.vis.otfivs.data.OTFClientQuad;


public interface OTFDrawer {
	public void invalidate(int time) throws RemoteException;
	public void redraw();
	public void handleClick(Point2D.Double point, int mouseButton);
	public OTFClientQuad getQuad();
	public Component getComponent();
	public void clearCache();
}
