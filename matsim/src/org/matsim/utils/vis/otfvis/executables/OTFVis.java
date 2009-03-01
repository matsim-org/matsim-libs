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

/* was set to deprecated a while ago. If you still have a reason to use this class
 * instead of org.matsim.run.OTFVis, then please tell me so. Otherwise I might
 * delete this class completely after some while... / marcel,20081104
 * 
 * commented out most of the code to force people switch to OTFVis / marcel,20090301
 */
/**
 * @deprecated use org.matsim.run.OTFVis instead
 */
@Deprecated
public class OTFVis   extends Thread {
//	private final JFrame frame;
//	private final OTFHostControlBar hostControl;
	
	public OTFVis(/*String address*/) /*throws RemoteException, InterruptedException, NotBoundException*/ {
		throw new RuntimeException("This class is deprecated, please use org.matsim.run.OTFVis.");
//		if (Gbl.getConfig() == null) Gbl.createConfig(null);
//
//		OTFVisConfig config = (OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);
//
//		if (config == null) config = new OTFVisConfig();
//	    Gbl.getConfig().addModule(OTFVisConfig.GROUP_NAME, config);
//
//		frame = new JFrame("MATSIM OTFVis" + address);
//		hostControl = new OTFHostControlBar(address);
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		JFrame.setDefaultLookAndFeelDecorated(true);
//		frame.add(hostControl);
//		frame.setVisible(true);
	}
	
	public static void main(String[] args) {
		throw new RuntimeException("This class is deprecated, please use org.matsim.run.OTFVis.");
//		OTFVis client;
//		try {
//			client = new OTFVis("file:../MatsimJ/output/OTFQuadfile.mvi");
////			client = new OTFVis("rmi:127.0.0.1:4019");
//			client.run();
//		} catch (RemoteException e) {
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		} catch (NotBoundException e) {
//			e.printStackTrace();
//		}
	}

}
