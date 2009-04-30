package playground.gregor.otf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;

import com.sun.opengl.util.texture.TextureIO;

public class TileLoader extends Thread {

	PriorityBlockingQueue<TileRequest> requests = new PriorityBlockingQueue<TileRequest>();
	private final Map<String, Tile> tiles;
	private final Queue<Tile> tilesQueue;
	private long oldTime;
	private static final int MAX_CACHE = 2048;
	public TileLoader(Map<String,Tile> tiles) {
//		this.tiles = Collections.synchronizedMap(tiles);
		this.tiles = tiles;
		this.tilesQueue = new ConcurrentLinkedQueue<Tile>();
	}

	@Override
	public void run(){
		while (true) {
			if (System.currentTimeMillis() > this.oldTime + 2000){
				this.oldTime = System.currentTimeMillis();
				System.out.println("dynamic cache:" + this.tilesQueue.size()  + " static cache:" + this.tiles.size() + " requests:" + this.requests.size());
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
			handleRequest(tr);
			if (this.requests.size() > 2048) {
				this.requests.clear();
			}

		}
		

	}

	private void handleRequest(TileRequest tr) {

		if (this.tiles.containsKey(tr.tile.id)) {
			return;
		}
		
		InputStream is = null;
		try {
			is = getBGImageStream(tr.refCoordX, tr.refCoordY, tr.geoWidth, tr.geoWidth, Tile.LENGTH, Tile.LENGTH);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			tr.tile.tx = TextureIO.newTextureData(is,false,"png");
		} catch (IOException e) {
			e.printStackTrace();
		}
//		try {
//			is.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		this.tiles.put(tr.tile.id, tr.tile);
		if (tr.tile.zoom < 4){
			this.tilesQueue.add(tr.tile);
		}
		

		if (this.tilesQueue.size() > MAX_CACHE ) {

			while (this.tilesQueue.size() > MAX_CACHE*0.75) {
				Tile tile = this.tilesQueue.poll();
				this.tiles.remove(tile.id);
				
			}
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


	public void addRequest(Tile t, double refCoordX, double refCoordY, double geoWidth){

		TileRequest req = new TileRequest();
		t.time = System.currentTimeMillis();
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
		public int compareTo(TileRequest o) {
			return this.tile.compareTo(o.tile);

		}


	}
}
