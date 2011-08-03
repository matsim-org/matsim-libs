package org.matsim.vis.otfvis.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D.Double;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.matsim.vis.otfvis.data.OTFClientQuadTree;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFQueryHandler;

public class OTFSwingDrawerContainer extends JPanel implements OTFDrawer {
	
	final OTFSwingDrawer delegate;
	
	VizGuiHandler mouseMan;

	private MyNetVisScrollPane networkScrollPane;
	
	public Rectangle currentRect;
	
	public OTFSwingDrawerContainer(OTFClientQuadTree quad, OTFHostControlBar hostControlBar) {
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
	
	@Override
	public void clearCache() {
		delegate.clearCache();
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public OTFClientQuadTree getQuad() {
		return delegate.getQuad();
	}

	@Override
	public double getScale() {
		return delegate.getScale();
	}

	@Override
	public void handleClick(Double point, int mouseButton, MouseEvent e) {
		delegate.handleClick(point, mouseButton, e);
	}

	@Override
	public void handleClick(Rectangle currentRect, int button) {
		delegate.handleClick(currentRect, button);
	}

	@Override
	public void redraw() {
		delegate.repaint();
	}

	@Override
	public void setQueryHandler(OTFQueryHandler queryHandler) {
		delegate.setQueryHandler(queryHandler);
	}

	@Override
	public void setScale(double scale) {
		this.networkScrollPane.scaleNetwork(scale);
		this.delegate.hostControlBar.updateScaleLabel();
	}
	
	public void setScale(Rectangle destrect, double scale){
		this.networkScrollPane.scaleNetwork(destrect, scale);
		this.delegate.hostControlBar.updateScaleLabel();
	}

}
