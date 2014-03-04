/* *********************************************************************** *
 * project: org.matsim.*
 * Menu
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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.apache.log4j.Logger;

import playground.dgrether.xvis.control.XVisControl;


/**
 * @author dgrether
 *
 */
public class Menu implements ItemListener{

	private static final Logger log = Logger.getLogger(Menu.class);
	
	private JCheckBoxMenuItem[] cbmi;

	public void init(JFrame frame){
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		Action exitAction = new ExitAction(); 	
		fileMenu.add(exitAction);
		JMenu viewMenu = this.createViewMenu();
		menuBar.add(viewMenu);
		frame.setJMenuBar(menuBar);
	}
	
  protected JMenu createViewMenu() {
    JMenu ableMenu = new JMenu("View");
    this.cbmi = new JCheckBoxMenuItem[3];
    cbmi[0] = new JCheckBoxMenuItem("Link Ids");
    cbmi[0].setSelected(XVisControl.getInstance().getDrawingPreferences().isShowLinkIds());
    cbmi[0].addItemListener(this);
    ableMenu.add(cbmi[0]);

    cbmi[1] = new JCheckBoxMenuItem("Lane Ids");
    cbmi[1].setSelected(XVisControl.getInstance().getDrawingPreferences().isShowLaneIds());
    cbmi[1].addItemListener(this);
    ableMenu.add(cbmi[1]);
   
    cbmi[2] = new JCheckBoxMenuItem("Link 2 Link Lines");
    cbmi[2].setSelected(XVisControl.getInstance().getDrawingPreferences().isShowLink2LinkLines());
    cbmi[2].addItemListener(this);
    ableMenu.add(cbmi[2]);

    return ableMenu;
}

public void itemStateChanged(ItemEvent e) {
    JCheckBoxMenuItem mi = (JCheckBoxMenuItem)(e.getSource());
    boolean selected =
        (e.getStateChange() == ItemEvent.SELECTED);
    log.debug("selected: " + selected);
    if (mi == cbmi[0]) {
    		XVisControl.getInstance().getDrawingPreferences().setShowLinkIds(selected);
    } 
    else if (mi == cbmi[1]) {
  		XVisControl.getInstance().getDrawingPreferences().setShowLaneIds(selected);
    }
    else if (mi == cbmi[2]) {
  		XVisControl.getInstance().getDrawingPreferences().setShowLink2LinkLines(selected);
    }
}
}
