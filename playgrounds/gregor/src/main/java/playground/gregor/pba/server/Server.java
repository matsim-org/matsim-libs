/* *********************************************************************** *
 * project: org.matsim.*
 * Server.java
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

package playground.gregor.pba.server;

import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class Server extends WebSocketServer {

	public Server(InetSocketAddress address) {
		super(address);
	}

	@Override
	public void onClose(WebSocket arg0, int arg1, String arg2, boolean arg3) {
		System.err.println(arg1);

	}

	@Override
	public void onError(WebSocket arg0, Exception arg1) {
		System.err.println(arg1);

	}

	@Override
	public void onMessage(WebSocket arg0, String arg1) {
		System.err.println(arg1);

	}

	@Override
	public void onOpen(WebSocket arg0, ClientHandshake arg1) {
		System.err.println(arg1);

	}

	public static void main(String [] args) {
		InetSocketAddress addr = new InetSocketAddress(8088);
		Server s = new Server(addr);
		Thread t = new Thread(s);
		t.start();
	}
}
