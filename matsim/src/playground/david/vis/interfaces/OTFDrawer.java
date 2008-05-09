package playground.david.vis.interfaces;

import java.awt.Component;
import java.awt.geom.Point2D;
import java.rmi.RemoteException;

import playground.david.vis.data.OTFClientQuad;

public interface OTFDrawer {
	public void invalidate(int time) throws RemoteException;
	public void redraw();
	public void handleClick(Point2D.Double point, int mouseButton);
	public OTFClientQuad getQuad();
	public Component getComponent();
	public void clearCache();
}
