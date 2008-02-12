package playground.david.vis.gui;

import java.awt.geom.Point2D;
import java.rmi.RemoteException;

public interface OTFEventHandler {

	public void invalidate(int time) throws RemoteException;
	public void redraw();
	public void handleClick(Point2D.Double point);
}
