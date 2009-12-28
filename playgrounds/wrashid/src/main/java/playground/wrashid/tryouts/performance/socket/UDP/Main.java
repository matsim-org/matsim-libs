package playground.wrashid.tryouts.performance.socket.UDP;


public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Thread t0 = new Thread(new PongRunner());
		t0.start();
		new Ping(1001,1000,1);
		
		
		
	}

	
	static class PongRunner implements Runnable {

		public void run() {
			new Pong(1000,1001);
		}

		public PongRunner() {
			
		}
	}
	
}
