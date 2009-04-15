package playground.gregor.otf;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.PriorityQueue;

import com.sun.opengl.util.texture.TextureData;
import com.sun.opengl.util.texture.TextureIO;


public class BGLoader extends Thread {

	private final InetAddress addr;
	private final int port;

	PriorityQueue<BGRequest> requests = new PriorityQueue<BGRequest>();

	public BGLoader(InetAddress addr, int port) {
		this.addr = addr;
		this.port = port;

	}

	@Override
	public void run() {
		while (true) {
			if (this.requests.size() == 0) {
				try {
					sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			if (this.requests.peek().getLock()){
					BGRequest bgr =this.requests.poll();
					handleRequest(bgr);
					bgr.unLock();
					
			} else {
				try {
					sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		}

	}

	private void handleRequest(BGRequest bgr) {
		if (bgr.getState() == BGRequest.State.obsolete) {
			return;
		}
		
		if (bgr.getState() != BGRequest.State.open) {
			System.err.println("this should not happen!");
			return;
		}		
		
		
		BackgroundFromStreamDrawer bgs = bgr.getBGS();
		double topX = bgs.getTopX();
		double topY = bgs.getTopY();
		double xSize = bgs.getXs();
		double ySize = bgs.getYs();
		int pxSize = bgs.getPxSize();
		int pySize = bgs.getPySize();
		InputStream is  = null;
		try {
			is = getBGImageStream(topX, topY, xSize, ySize, pxSize, pySize);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Rectangle2D.Float koords =  new Rectangle2D.Float((int)Math.round(topX)+(int)Math.round(xSize),(int)Math.round(topY)-(int)Math.round(ySize),-(int)Math.round(xSize),(int)Math.round(xSize));


		TextureData t = null;

		try {
			t = TextureIO.newTextureData(is,false,"png");
		} catch (IOException e) {
			e.printStackTrace();
		}

		bgr.response(t,koords);
		bgr.setState(BGRequest.State.processed);
		
	}

	public void addRequest(BGRequest r) {
		this.requests.add(r);
	}

	private InputStream getBGImageStream(double topX, double topY, double xSize, double ySize, int pxSize, int pySize) throws IOException{
		Socket socket = new Socket(this.addr, this.port);
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




}
