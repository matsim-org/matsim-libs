/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyClientQuadSwing.java
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

package org.matsim.vis.otfvis;


import java.awt.BorderLayout;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Locale;

import org.matsim.core.utils.io.MatsimFileTypeGuesser;
import org.matsim.core.utils.io.MatsimFileTypeGuesser.FileType;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.fileio.queuesim.OTFQueueSimLinkAgentsWriter;
import org.matsim.vis.otfvis.gui.NetJComponent;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFTimeLine;
import org.matsim.vis.otfvis.opengl.gui.SettingsSaver;



/**
 * This Client is capable of running on SWING only computers. It does not need OpenGL acceleration.
 * But it does not feature the whole set of operations possible with the OpenGL client.
 * It is also very slow, but for small networks it should work.
 *
 * @author dstrippgen
 * @author dgrether
 */
public class OTFClientSwing extends OTFClient {

	private OTFConnectionManager connectionManager = new OTFConnectionManager();;

	public OTFClientSwing(String url) {
		super("file:" + url);
		connectionManager.connectWriterToReader(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
		connectionManager.connectReaderToReceiver(OTFLinkLanesAgentsNoParkingHandler.class, NetJComponent.SimpleQuadDrawer.class);
		connectionManager.connectReaderToReceiver(OTFLinkLanesAgentsNoParkingHandler.class,  NetJComponent.AgentDrawer.class);
		/*
		 * This entry is needed to couple the org.matsim.core.queuesim to the visualizer
		 */
		this.connectionManager.connectWriterToReader(OTFQueueSimLinkAgentsWriter.class, OTFLinkLanesAgentsNoParkingHandler.class);

	}

	@Override
	protected OTFDrawer createDrawer() {
		try {
			if(!hostControlBar.getOTFHostControl().isLiveHost()) {
				frame.getContentPane().add(new OTFTimeLine("time", hostControlBar), BorderLayout.SOUTH);
			} else  {
				throw new IllegalStateException("Server in live mode!");
			}
			NetJComponent mainDrawer = new NetJComponent(createNewView("swing", connectionManager, hostControlBar.getOTFHostControl()));
			hostControlBar.finishedInitialisition();
			return mainDrawer;
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

	@Override
	protected OTFVisConfig createOTFVisConfig() {
	    saver = new SettingsSaver(this.url);
	    OTFVisConfig visconf = new OTFVisConfig();
	    return visconf;
	}

	public static void main(String[] args) {
		String lcArg0 = args[0].toLowerCase(Locale.ROOT);
		if (lcArg0.endsWith(".mvi")) {
			new OTFClientSwing("file:" + args[0]).run();
		} else if (lcArg0.endsWith(".xml") || lcArg0.endsWith(".xml.gz")) {
			try {
				FileType fType = new MatsimFileTypeGuesser(args[0]).getGuessedFileType();
				if (FileType.Network.equals(fType)) {
					new OTFClientSwing("net:" + args[0]).run();
				} else {
					throw new RuntimeException("The provided file cannot be visualized.");
				}
			} catch (IOException e) {
				throw new RuntimeException("Could not guess type of file " + args[0]);
			}
		}
	}
}
