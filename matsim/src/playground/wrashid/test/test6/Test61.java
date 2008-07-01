package playground.wrashid.test.test6;

import java.util.ArrayList;



import playground.wrashid.DES.EventLog;
import playground.wrashid.DES.SimulationParameters;
import playground.wrashid.deqsim.DEQSimStarter;
import playground.wrashid.test.CppEventFileParser;
import playground.wrashid.test.Lock;
import playground.wrashid.test.PCppEventFileParser;

public class Test61 {

	/**
	 * @param args
	 */
	
	public static void main(String[] args) {
		Test61 t6=new Test61();
		t6.testTest6();
	}
	
	public void testTest6() {
		String baseDir="src/playground/wrashid/test/test6/";
		String[] args=new String[1];
		
		
		Lock lock=new Lock();
		PCppEventFileParser parallelParser=new PCppEventFileParser(baseDir + "deq_events.txt",lock);
		parallelParser.start();	
		
		
		
		
			
		args[0]= baseDir + "config.xml";
		DEQSimStarter.main(args);
		
		//args[0]= baseDir + "deq_events.txt";
		//CppEventFileParser.main(args);
		
		
		if (!parallelParser.taskCompleted){
			try {
				lock.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 
		
		
		ArrayList<EventLog> eventLog1= SimulationParameters.eventOutputLog;
		
		ArrayList<EventLog> eventLog2= CppEventFileParser.eventLog;
		
		//EventLog.print(eventLog1);
		
		EventLog.compare(eventLog1,eventLog2);
	}
}
