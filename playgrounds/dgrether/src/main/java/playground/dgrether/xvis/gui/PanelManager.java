/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.dgrether.xvis.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import playground.dgrether.xvis.control.XVisControl;
import playground.dgrether.xvis.control.events.ShowPanelEvent;
import playground.dgrether.xvis.control.handlers.ShowPanelEventListener;


/**
 * @author dgrether
 *
 */
public class PanelManager implements ShowPanelEventListener {
	
	public enum Area {CENTER, BUTTON, LEFT, RIGHT}

	private JPanel mainPanel;
	private JSplitPane leftSplitPane;
	private JSplitPane centerRightSplitPane;
	private JPanel splitPanePanel;

	public PanelManager(){
		XVisControl.getInstance().getControlEventsManager().addControlListener(this);
	}
	
	public JPanel createMainPanel(){
		this.mainPanel = new JPanel();
		mainPanel.setPreferredSize(new Dimension(800, 600));
		mainPanel.setLayout(new BorderLayout());
		mainPanel.setVisible(true);
		this.splitPanePanel = new JPanel(new BorderLayout());
		this.createSplitPanes();
		this.mainPanel.add(splitPanePanel, BorderLayout.CENTER);
		return mainPanel;
	}
	
	private void createSplitPanes(){
		this.leftSplitPane = new JSplitPane();
		this.centerRightSplitPane = new JSplitPane();
		this.leftSplitPane.setRightComponent(this.centerRightSplitPane);
		this.splitPanePanel.add(this.leftSplitPane);
	}
	

	public void addContainer(Container container, Area a){
		if (Area.LEFT.equals(a)){
			this.addLeft(container);
		}
		else if (Area.CENTER.equals(a)){
			this.addCenter(container);
		}
		else if (Area.BUTTON.equals(a)){
			this.addButton(container);
		}
		else if (Area.RIGHT.equals(a)){
			this.addRight(container);
		}
		else {
			throw new UnsupportedOperationException("Area " + a + " not implemented");
		}
	}
	
	

	private void addRight(Container container) {
		this.centerRightSplitPane.setRightComponent(container);
//		this.centerRightSplitPane.setDividerLocation(0.8);
		this.centerRightSplitPane.setResizeWeight(0.3);
	}

	private void addButton(Container container) {
		this.mainPanel.add(container, BorderLayout.NORTH);
	}

	private void addCenter(Container container) {
//		if (this.splitPane == null){
//			JTabbedPane tabbedContainer = new JTabbedPane();
//			this.splitPane.setRightComponent(tabbedContainer);
			this.centerRightSplitPane.setLeftComponent(container);
//			this.centerRightSplitPane.setDividerLocation(0.8);
//		}
//		else {
//			JTabbedPane tabbedPane = (JTabbedPane) this.splitPane.getRightComponent();
//			tabbedPane.add(container);
//		}
	}

	private void addLeft(Container container) {
		this.leftSplitPane.setLeftComponent(container);
//		this.splitPane.
		this.leftSplitPane.setDividerLocation(0.5);
//		this.leftSplitPane.repaint();
	}

	@Override
	public void handleEvent(ShowPanelEvent e) {
		this.addContainer(e.getPanel(), e.getArea());
	}

}

