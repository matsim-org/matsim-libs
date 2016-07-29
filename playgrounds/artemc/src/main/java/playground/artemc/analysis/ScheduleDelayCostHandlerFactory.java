package playground.artemc.analysis;

import org.matsim.core.controler.Controler;

import java.util.HashSet;

/**
 * Created by artemc on 09/10/15.
 */
public interface ScheduleDelayCostHandlerFactory{

	public HashSet<String> getUsedModes();
	public void setControler(Controler controler);
}
