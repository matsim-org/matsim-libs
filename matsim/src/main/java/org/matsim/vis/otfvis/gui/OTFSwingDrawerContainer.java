package org.matsim.vis.otfvis.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D.Double;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFQueryHandler;

public class OTFSwingDrawerContainer extends JPanel implements OTFDrawer {
	
	final OTFSwingDrawer delegate;
	
	VizGuiHandler mouseMan;

	private MyNetVisScrollPane networkScrollPane;
	
	public Rectangle currentRect;
	
	public OTFSwingDrawerContainer(OTFClientQuad quad, OTFHostControlBar hostControlBar) {
		super(new BorderLayout());
		delegate = new OTFSwingDrawer(quad, hostControlBar, this);
		networkScrollPane = new MyNetVisScrollPane(delegate);
		networkScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		networkScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		VizGuiHandler handi = new VizGuiHandler(this, networkScrollPane);
		DragToScrollListener dragToScrollLister = new DragToScrollListener(networkScrollPane);
		delegate.addMouseListener(dragToScrollLister);
		delegate.addMouseMotionListener(dragToScrollLister);
		add(networkScrollPane, BorderLayout.CENTER);
		delegate.addMouseMotionListener(handi);
		delegate.addMouseListener(handi);
		delegate.addMouseWheelListener(handi);
		this.mouseMan = handi;
	}
	
	public void clearCache() {
		delegate.clearCache();
	}

	public Component getComponent() {
		return this;
	}

	public OTFClientQuad getQuad() {
		return delegate.getQuad();
	}

	public float getScale() {
		return delegate.getScale();
	}

	public void handleClick(Double point, int mouseButton, MouseEvent e) {
		delegate.handleClick(point, mouseButton, e);
	}

	public void handleClick(Rectangle currentRect, int button) {
		delegate.handleClick(currentRect, button);
	}

	public void redraw() {
		delegate.repaint();
	}

	public void setQueryHandler(OTFQueryHandler queryHandler) {
		delegate.setQueryHandler(queryHandler);
	}

	public void setScale(float scale) {
		this.networkScrollPane.scaleNetwork(scale);
		this.delegate.hostControlBar.updateScaleLabel();
	}
	
	public void setScale(Rectangle destrect, float factor){
		this.networkScrollPane.scaleNetwork(destrect, factor);
		this.delegate.hostControlBar.updateScaleLabel();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
	}

}
