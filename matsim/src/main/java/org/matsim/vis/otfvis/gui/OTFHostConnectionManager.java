/* *********************************************************************** *
 * project: org.matsim.*
 * OTFHostControl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.vis.otfvis.gui;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFLiveServerRemote;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;
import org.matsim.vis.otfvis.opengl.gui.OTFTimeLine;


/**
 * @author dgrether
 *
 */
public class OTFHostConnectionManager {

	private String address;

	private OTFServerRemote host = null;
	protected OTFLiveServerRemote liveHost = null;

	protected Object blockReading = new Object();

	protected int controllerStatus = 0;

	private final Map <String,OTFClientQuad> quads = new HashMap<String,OTFClientQuad>();
	private final Map <String,OTFDrawer> drawer = new HashMap<String,OTFDrawer>();

	public OTFHostConnectionManager(String url) throws RemoteException, InterruptedException, NotBoundException{
		this.openAddress(url);
	}

	public OTFServerRemote getOTFServer(){
		return this.host;
	}
	private void openAddress(final String address) throws RemoteException, InterruptedException, NotBoundException {
		this.address = address;
		this.host = new OTFHostConnectionBuilder().createRemoteServerConnection(address);
		if (host != null) {
			if (host.isLive()){
				liveHost = (OTFLiveServerRemote)host;
				controllerStatus = liveHost.getControllerStatus();
			}
		}
	}

	public boolean isLiveHost() {
		return liveHost != null;
	}

	public Collection<Double> getTimeStepsdrawer() {
		try {
			return this.host.getTimeSteps();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;
	}

	public double getTime() {
		try {
			return host.getLocalTime();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public String getAddress() {
		return address;
	}

	public Map<String, OTFClientQuad> getQuads() {
		return quads;
	}

	public Map<String, OTFDrawer> getDrawer() {
		return drawer;
	}

	public void finishedInitialisition() {
		try {
			if(!getOTFServer().isLive() && OTFClientControl.getInstance().getOTFVisConfig().isCachingAllowed()) {
				new PreloadHelper().start();
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	protected boolean preCacheCurrentTime(int time, OTFTimeLine timeLine) throws IOException {
		boolean result = getOTFServer().requestNewTime(time, OTFServerRemote.TimePreference.LATER);

		for(OTFDrawer handler : getDrawer().values()) {
			if(handler != timeLine) {
			  handler.getQuad().getSceneGraphNoCache(getOTFServer().getLocalTime(), null, handler);
			}
		}
		return result;
	}


	private class PreloadHelper extends Thread {

		public void preloadCache() {
			boolean hasNext = true;
			OTFTimeLine timeLine = (OTFTimeLine)drawer.get("timeline");

			try {
//				int	time = getOTFServer().getLocalTime();

				while (hasNext && !(timeLine.isCancelCaching)) {
					int time;
					synchronized(blockReading) {
						// remember time the block had before caching next step
//						int	origtime = getOTFServer().getLocalTime();
						time = getOTFServer().getLocalTime() + 1;
						hasNext = preCacheCurrentTime(time,timeLine);
						time = getOTFServer().getLocalTime();
					}
					timeLine.setCachedTime(time); // add this to the cached times in the time line drawer
				}
				if (timeLine != null) timeLine.setCachedTime(-1);
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

//			((OTFQuadFileHandler.Reader)host).closeFile();
		}
		@Override
		public void run() {
			preloadCache();
		}
	}

}
