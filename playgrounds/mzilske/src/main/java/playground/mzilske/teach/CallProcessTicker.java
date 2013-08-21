package playground.mzilske.teach;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.events.handler.BasicEventHandler;

import playground.mzilske.cdr.CallProcess;

public class CallProcessTicker implements BasicEventHandler {

	private CallProcess callProcess;

	private double currentTime = -1.0;

	public CallProcessTicker(CallProcess callProcess) {
		this.callProcess = callProcess;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(Event event) {
			while (event.getTime() > currentTime) {
				currentTime = currentTime + 1.0;
				callProcess.tick(currentTime);
			}
		}

}
