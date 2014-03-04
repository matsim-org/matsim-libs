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
package playground.dgrether.xvis.gui.processing;

import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import playground.dgrether.xvis.control.XVisControl;
import playground.dgrether.xvis.control.events.ScaleEvent;
import playground.dgrether.xvis.control.handlers.ScaleEventListener;
import playground.dgrether.xvis.layout.SpringUtilities;

/**
 * @author dgrether
 */
public class ControlButtonPanel extends JPanel implements ScaleEventListener {
	
	private final JLabel zoomLabel  = new JLabel();
	
	public ControlButtonPanel() {
		super();
		XVisControl.getInstance().getControlEventsManager().addControlListener(this);
		this.initGui();
	}

	private void initGui() {
		SpringLayout layout = new SpringLayout();
		this.setLayout(layout);

		this.updateZoomLabelText(1.0f);
		
		JButton zoomOutButton = new JButton();
		this.add(zoomOutButton);
		String filename = "jlfgr-1_0/toolbarButtonGraphics/general/ZoomOut24.gif";
		URL url = ControlButtonPanel.class.getClassLoader().getResource(filename);
		Icon icon = null;
		icon = new ImageIcon(url);
//		zoomOutButton.setAction(new AbstractAction("zoom out", icon){
		zoomOutButton.setAction(new AbstractAction("", icon){
			@Override
			public void actionPerformed(ActionEvent e) {
				XVisControl.getInstance().getDrawingPreferences().incrementScale(-0.3f);
			}
		});
		
		JButton zoomInButton = new JButton();
		this.add(zoomInButton);

		filename = "jlfgr-1_0/toolbarButtonGraphics/general/ZoomIn24.gif";
		url = ControlButtonPanel.class.getClassLoader().getResource(filename);
		icon = new ImageIcon(url);
//		zoomInButton.setAction(new AbstractAction("zoom in"){
		zoomInButton.setAction(new AbstractAction("", icon){
			@Override
			public void actionPerformed(ActionEvent e) {
				XVisControl.getInstance().getDrawingPreferences().incrementScale(0.3f);
			}
		});
		
		this.add(zoomLabel);
		
		int xPad = XVisControl.getInstance().getLayoutPreferences().getDefaultXPadding();
		int yPad = XVisControl.getInstance().getLayoutPreferences().getDefaultYPadding();
		SpringUtilities.makeCompactGrid(this, 1, this.getComponentCount(), xPad, yPad, xPad, yPad);

	}

	private void updateZoomLabelText(float s){
		zoomLabel.setText("Scale: " + Float.toString(s));
//		zoomLabel.repaint();
	}

	@Override
	public void handleEvent(ScaleEvent e) {
		this.updateZoomLabelText(e.getRealScale());
	}
	
	
}
