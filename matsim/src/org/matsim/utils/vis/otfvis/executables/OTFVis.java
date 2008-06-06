/* *********************************************************************** *
 * project: org.matsim.*
 * OTFVis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.utils.vis.otfvis.executables;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.swing.JFrame;

import org.matsim.gbl.Gbl;
import org.matsim.utils.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.utils.vis.otfvis.gui.OTFVisConfig;

/**
 * @deprecated 
 * use org.matsim.run.OTFVis instead
 */
@Deprecated
public class OTFVis   extends Thread {
	private final JFrame frame;
	private final OTFHostControlBar hostControl;
	
	public OTFVis(String address) throws RemoteException, InterruptedException, NotBoundException{
		if (Gbl.getConfig() == null) Gbl.createConfig(null);

		OTFVisConfig config = (OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);

		if (config == null) config = new OTFVisConfig();
	    Gbl.getConfig().addModule(OTFVisConfig.GROUP_NAME, config);

		frame = new JFrame("MATSIM NetVis" + address);
		hostControl = new OTFHostControlBar(address);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JFrame.setDefaultLookAndFeelDecorated(true);
		frame.add(hostControl);
		frame.setVisible(true);
	}
	
	
	
	public static void main(String[] args) {
		OTFVis client;
		try {
			client = new OTFVis("file:../MatsimJ/output/OTFQuadfile.mvi");
//			client = new OTFVis("rmi:127.0.0.1:4019");
			client.run();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
