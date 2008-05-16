package org.matsim.utils.vis.otfivs.opengl.drawer;

import java.awt.Component;
import java.awt.geom.Point2D.Double;
import java.rmi.RemoteException;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.matsim.utils.vis.otfivs.data.OTFClientQuad;
import org.matsim.utils.vis.otfivs.gui.NetJComponent.myNetVisScrollPane;
import org.matsim.utils.vis.otfivs.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfivs.interfaces.OTFQuery;


public class OTFSwingDrawer extends JComponent implements OTFDrawer {

	private final OTFClientQuad quad;

	public OTFSwingDrawer(JFrame frame, OTFClientQuad quad ) {
		this.quad = quad;
		myNetVisScrollPane networkScrollPane = new myNetVisScrollPane(null);
	}
	public Component getComponent() {
		return this;
	}

	public OTFClientQuad getQuad() {
		// TODO Auto-generated method stub
		return null;
	}

	public void handleClick(Double point, int mouseButton) {
		// TODO Auto-generated method stub

	}

	public void invalidate(int time) throws RemoteException {
		// TODO Auto-generated method stub

	}

	public void redraw() {
		// TODO Auto-generated method stub

	}
	public void addQuery(OTFQuery query) {
		// TODO Auto-generated method stub
		
	}
	public void removeQueries() {
		// TODO Auto-generated method stub
		
	}
	public void clearCache() {
		if(quad != null) quad.clearCache();
	}


}
