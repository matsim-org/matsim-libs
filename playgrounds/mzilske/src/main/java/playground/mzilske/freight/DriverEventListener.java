package playground.mzilske.freight;

public interface DriverEventListener extends CarrierEventListener{
	public abstract void processEvent(DriverEvent event);
}
