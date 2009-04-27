package playground.gregor.otf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;

import com.sun.opengl.util.texture.TextureIO;

public class TileLoader extends Thread {

	PriorityBlockingQueue<TileRequest> requests = new PriorityBlockingQueue<TileRequest>();
	private final Map<String, Tile> tiles;
	private final Map<String,TileRequest> openRequests = Collections.synchronizedMap(new HashMap<String,TileRequest>());
	private final Queue<Tile> tilesQueue;
	private long oldTime;
	private static final int MAX_CACHE = 2048;
	public TileLoader(Map<String,Tile> tiles) {
		this.tiles = tiles;
		this.tilesQueue = new ConcurrentLinkedQueue<Tile>();
	}

	@Override
	public void run(){
		while (true) {
			if (System.currentTimeMillis() > this.oldTime + 2000){
				this.oldTime = System.currentTimeMillis();
				System.out.println("dynamic cache:" + this.tilesQueue.size()  + " open requests:" + this.openRequests.size() + " static cache:" + this.tiles.size() + " requests:" + this.requests.size());
			}
			if (this.requests.size() == 0) {
				try {
					sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}

			TileRequest tr =this.requests.poll();
//			if (tr.obs) {
//				this.openRequests.remove(tr.id);
//			}
			handleRequest(tr);

			if (this.requests.size() > 1024) {
				this.requests.clear();
				this.openRequests.clear();
			}

		}
		

	}

	private void handleRequest(TileRequest tr) {


		InputStream is = null;
		try {
			is = getBGImageStream(tr.refCoordX, tr.refCoordY, tr.geoWidth, tr.geoWidth, Tile.LENGTH, Tile.LENGTH);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			tr.tile.tx = TextureIO.newTextureData(is,false,"png");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.tiles.put(tr.id, tr.tile);
		if (tr.tile.zoom < 4){
			this.tilesQueue.add(tr.tile);
		}
		
		this.openRequests.remove(tr.id);

		if (this.tilesQueue.size() > MAX_CACHE ) {

			while (this.tilesQueue.size() > MAX_CACHE/2) {
				Tile tile = this.tilesQueue.poll();
				this.tiles.remove(tile.id);
				
			}
			this.openRequests.clear();
			this.requests.clear();
		}

	}

	private InputStream getBGImageStream(double topX, double topY, double xSize, double ySize, int pxSize, int pySize) throws IOException{
		Socket socket = new Socket("localhost", 8080);
		String query = "GET /?x="+topX+"&y="+topY+"&xSize="+xSize+"&ySize="+ySize+"&pxSize="+pxSize+"&pySize="+pySize;
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

//	public void withdrawRequest(String id) {
//		this.openRequests.get(id).setIsObsolete(true);
//	}
//
//	public void withdrawRequests() {
//		for (TileRequest tr : this.openRequests.values()) {
//			tr.setIsObsolete(true);
//		}
//	}

	public void addRequest(Tile t, double refCoordX, double refCoordY, double geoWidth){
		if (this.openRequests.containsKey(t.id)) {
			TileRequest tr = this.openRequests.get(t.id);
//			tr.setIsObsolete(true);
			this.openRequests.remove(t.id);
			this.requests.remove(tr);
//			return;
		}

		TileRequest req = new TileRequest();
		t.time = System.currentTimeMillis();
		this.openRequests.put(t.id,req);
		req.id = t.id;
		req.tile = t;
		req.refCoordX = refCoordX;
		req.refCoordY = refCoordY;
		req.geoWidth = geoWidth;
		this.requests.add(req);
	}

	private static class TileRequest implements Comparable<TileRequest> {

		public double geoWidth;
		public double refCoordY;
		public double refCoordX;
		public Tile tile;
		public String id;
//		private boolean obs = false;
		public int compareTo(TileRequest o) {
			return this.tile.compareTo(o.tile);

		}

//		synchronized public boolean setIsObsolete(boolean obs) {
//			boolean old = this.obs ;
//			this.obs = obs;
//
//			return old;
//		}

	}
}
