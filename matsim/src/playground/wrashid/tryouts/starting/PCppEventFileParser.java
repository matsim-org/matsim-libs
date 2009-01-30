package playground.wrashid.tryouts.starting;

public class PCppEventFileParser extends Thread {
	Object lock;
	String fileName;
	public volatile boolean taskCompleted=false;
	public void run() {
		CppEventFileParser parser=new CppEventFileParser();
		String[] args=new String[1];
		args[0]=fileName;
		CppEventFileParser.main(args);
		
		taskCompleted=true;
		
		synchronized (lock){
			lock.notify();
		}
		
		System.out.println("cpp event file read completed.");
	}

	public PCppEventFileParser(String fileName, Object lock) {
		this.lock=lock;
		this.fileName=fileName;
	}

}
