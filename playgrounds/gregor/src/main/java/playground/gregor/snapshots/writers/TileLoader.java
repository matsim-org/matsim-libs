/* *********************************************************************** *
 * project: org.matsim.*
 * TileLoader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.snapshots.writers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;


import com.sun.opengl.util.texture.TextureIO;

public class TileLoader extends Thread implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -1656742752661670725L;
//	PriorityBlockingQueue<TileRequest> requests = new PriorityBlockingQueue<TileRequest>();
	Stack<TileRequest> requests = new Stack<TileRequest>();
	private final Map<String, Tile> tiles;
	private final Queue<Tile> tilesQueue;
//	private final long oldTime = 0;
//	private long oldTime;
	private static final int MAX_CACHE = 512;
	private boolean running = true;
	public TileLoader(Map<String,Tile> tiles) {
//		this.tiles = Collections.synchronizedMap(tiles);
		this.tiles = tiles;
		this.tilesQueue = new ConcurrentLinkedQueue<Tile>();
	}

	@Override
	public void run(){
		while (this.running) {
//			if (System.currentTimeMillis() > this.oldTime  + 2000){
//				this.oldTime = System.currentTimeMillis();
//			}
			if (this.requests.size() == 0) {
				try {
					sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}

			TileRequest tr =this.requests.pop();
			handleRequest(tr);
			if (this.requests.size() > 256) {
				this.requests.clear();
			}

		}


	}

	public void kill() {
		this.running = false;
	}
	private void handleRequest(TileRequest tr) {

		if (this.tiles.containsKey(tr.tile.id)) {
			return;
		}

		InputStream is = null;
		try {
			is = getBGImageStream(tr.refCoordX, tr.refCoordY, tr.geoWidth, tr.geoWidth, Tile.LENGTH, Tile.LENGTH);
			tr.tile.setTx(TextureIO.newTextureData(is,false,"png"));
		} catch (IOException e) {

			this.requests.clear();
			tr.tile.setTx(null);
		}
//		try {
//			is.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		this.tiles.put(tr.tile.id, tr.tile);
		this.tilesQueue.add(tr.tile);



		if (this.tilesQueue.size() > MAX_CACHE ) {

			while (this.tilesQueue.size() > MAX_CACHE*0.75) {
				Tile tile = this.tilesQueue.poll();
//				tile.setTex(null);
				if (!tile.locked) {
					this.tiles.remove(tile.id);
				}

			}
		}

	}

	private InputStream getBGImageStream(double topX, double topY, double xSize, double ySize, int pxSize, int pySize) throws IOException{
		Socket socket = new Socket("localhost", 8080);
		String query = "GET /?x="+topX+"&y="+topY+"&xSize="+xSize+"&ySize="+ySize+"&pxSize="+pxSize+"&pySize="+pySize;
//		System.out.println("http://localhost:8080/?x="+topX+"&y="+topY+"&xSize="+xSize+"&ySize="+ySize+"&pxSize="+pxSize+"&pySize="+pySize);
		OutputStream os = socket.getOutputStream();
		os.write(query.getBytes());
		os.write("\r\n".getBytes());
		os.write("\r\n".getBytes());

		InputStream is = socket.getInputStream();

		StringBuffer sb = new StringBuffer();
		while (true) {
			char c = (char) is.read();
			sb.append(c);
			if (c == '\n') {
				if (sb.toString().startsWith("\r")) {
					break;
				}
				sb = new StringBuffer();
			}
		}



		return is;
	}


	public void addRequest(Tile t, double refCoordX, double refCoordY, double geoWidth){

		TileRequest req = new TileRequest();
//		t.setTime(System.currentTimeMillis());
		req.tile = t;
		req.refCoordX = refCoordX;
		req.refCoordY = refCoordY;
		req.geoWidth = geoWidth;
		this.requests.push(req);
	}

	private static class TileRequest { //implements Comparable<TileRequest> {

		public double geoWidth;
		public double refCoordY;
		public double refCoordX;
		public Tile tile;
//		public int compareTo(TileRequest o) {
//			return this.tile.compareTo(o.tile);
//
//		}


	}

}
