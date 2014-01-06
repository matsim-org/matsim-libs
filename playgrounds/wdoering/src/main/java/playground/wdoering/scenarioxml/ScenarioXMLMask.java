/* *********************************************************************** *
 * project: org.matsim.*
 * MyMapViewer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wdoering.scenarioxml;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.matsim.contrib.grips.control.Controller;
import org.matsim.contrib.grips.control.ShapeFactory;
import org.matsim.contrib.grips.model.AbstractModule;
import org.matsim.contrib.grips.model.AbstractToolBox;
import org.matsim.contrib.grips.model.Constants;
import org.matsim.contrib.grips.model.SelectionModeSwitch;
import org.matsim.contrib.grips.model.config.GripsConfigModule;
import org.matsim.contrib.grips.model.shape.PolygonShape;
import org.matsim.contrib.grips.model.shape.Shape;
import org.matsim.core.config.Config;

/**
 * @author wdoering
 * 
 */
class ScenarioXMLMask extends JPanel {
	private static final long serialVersionUID = 1L;
	// Fields shall be moved to ScenarioXMLToolBox later
	private JPanel jPanel1;
	private JLabel labelNetworkFile;
	private JLabel labelTrafficType;
	private JLabel jLabel3;
	private JLabel jLabel4;
	private JLabel jLabel5;
	private JTextField textFieldNetworkFile;
	private JTextField textFieldTrafficType;
	private JTextField jTextField3;
	private JTextField jTextField4;
	private JTextField jTextField5;
	private JLabel jLabel6;
	private JTextField jTextField6;
	private JComboBox jComboBox1;
	private JLabel jLabel7;
	private JLabel jLabel8;
	private JTextField jTextField7;
	private JTextField jTextField8;
	private JLabel jLabel9;
	private JTextField jTextField9;
	private JLabel jLabel10;
	private JTextField jTextField10;
	private JLabel jLabel11;
	private Controller controller;

	private void initComponents() {

		
        this.setLayout(new GridLayout(2,2));
        
        this.add(labelNetworkFile);
        this.add(textFieldNetworkFile);
        this.add(labelTrafficType);
        this.add(textFieldTrafficType);
        
	}

	ScenarioXMLMask(AbstractModule module, Controller controller) {
		this.controller = controller;
		this.setLayout(new BorderLayout());
		initComponents();
	}
	
	public void readConfig()
	{
		GripsConfigModule gcm = this.controller.getGripsConfigModule();
		String nfn = gcm.getNetworkFileName();
		this.textFieldNetworkFile.setText(nfn);
		String mtt = gcm.getMainTrafficType();
		this.textFieldTrafficType.setText(mtt);
		
//		this.btRun.setEnabled(true);
	}


}