package playground.wrashid.tryouts.performance;

import playground.wrashid.DES.utils.Timer;



public class Test1 {

	/**
	 * @param args
	 */
	
	public volatile int g=0;
	
	// just compares, no/with synchronized and volatile 
	
	public static void main(String[] args) {
		Timer t=new Timer();
		int f=0;
		Test1 t1=new Test1();
		int target=1000000000;
		t.startTimer();
		for (int i=0;i<target;i++){
			f=0;
		}
		t.endTimer();
		t.printMeasuredTime("time without synchronized: ");
		
		t.resetTimer();
		t.startTimer();
		for (int i=0;i<target;i++){
			synchronized(t){
				
			}
		}
		t.endTimer();
		t.printMeasuredTime("time with synchronized: ");
		
		
		t.resetTimer();
		t.startTimer();
		for (int i=0;i<target;i++){
			t1.g=0;
		}
		t.endTimer();
		t.printMeasuredTime("time with volatile: ");
		
		
	}

}
