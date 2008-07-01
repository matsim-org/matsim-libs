package playground.wrashid.test;

import java.util.ArrayList;

import playground.wrashid.DES.EventLog;
import playground.wrashid.DES.SimulationParameters;
import playground.wrashid.deqsim.DEQSimStarter;

public class PCppEventFileParser extends Thread {
	Lock lock;
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
		
		System.out.print("event files read");
	}

	public PCppEventFileParser(String fileName, Lock lock) {
		this.lock=lock;
		this.fileName=fileName;
	}

}
