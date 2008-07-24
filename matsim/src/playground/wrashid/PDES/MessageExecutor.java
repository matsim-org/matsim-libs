package playground.wrashid.PDES;

public class MessageExecutor implements Runnable {
	int i=0;
	private Message message;
	public volatile boolean hasAqiredLocks=false;

    private static ThreadLocal simTime = new ThreadLocal();

    public static double getSimTime() {
        return ((Double) (simTime.get())).doubleValue();
    }

    public static void setSimTime(double obj) {
    	simTime.set(obj);
    }


	public MessageExecutor(Message message){
		this.message=message;
	}

	public void run() {
		if (message.firstLock!=null){
			synchronized (message.firstLock){
				hasAqiredLocks=true;
				executeMessage();
			}
		} else {
			hasAqiredLocks=true;
			executeMessage();
		}
	}

	private void executeMessage(){
		MessageExecutor.setSimTime(message.getMessageArrivalTime());
		message.printMessageLogString();
		if (message instanceof SelfhandleMessage){
			((SelfhandleMessage) message).selfhandleMessage();
		} else {
			message.receivingUnit.handleMessage(message);
		}
	}

}
