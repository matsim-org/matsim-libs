package playground.wrashid.PDES;

public class TaskA implements Runnable {
	int i=0;
	
	
	private static int nextSerialNum = 0;
	 
    private static ThreadLocal simTime = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return new Integer(nextSerialNum++);
        }
    };

    public static int get() {
        return ((Integer) (simTime.get())).intValue();
    }
	
    public static void set(int obj) {
    	simTime.set(obj);
    }
	
	
	
	
	
	public TaskA(int j){
		i=j;
	}
	
	@Override
	public void run() {
		TaskA.set(99);
	}

}
