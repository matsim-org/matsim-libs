package playground.wrashid.PDES;

public class MessageExecutor implements Runnable {
	int i=0;
	private Message message;
	 
    private static ThreadLocal simTime = new ThreadLocal();

    public static double getSimTime() {
        return ((Integer) (simTime.get())).doubleValue();
    }
	
    public static void setSimTime(double obj) {
    	simTime.set(obj);
    }
	
	
	public MessageExecutor(Message message){
		this.message=message;
	}
	
	@Override
	public void run() {
		MessageExecutor.setSimTime(message.getMessageArrivalTime());
		message.printMessageLogString();
		if (message instanceof SelfhandleMessage){
			((SelfhandleMessage) message).selfhandleMessage();
		} else {
			message.receivingUnit.handleMessage(message);
		}
	}

}
