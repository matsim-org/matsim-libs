/* *********************************************************************** *
 * project: org.matsim.*
 * ClientEventManger.java
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

package playground.gregor.pba.client;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.matsim.core.api.experimental.events.EventsManager;

public class ClientEventManger extends WebSocketClient{

	private final EventsManager em;

	public ClientEventManger(URI serverURI, EventsManager e) {
		
		super(serverURI);
		this.em = e;
	}

	@Override
	public void onClose(int arg0, String arg1, boolean arg2) {
		System.err.println();	
		
	}

	@Override
	public void onError(Exception arg0) {
		System.err.println();	
		
	}

	@Override
	public void onMessage(String arg0) {
		System.err.println();	
		
	}

	@Override
	public void onOpen(ServerHandshake arg0) {
		System.err.println();		
	}
	
	

}
