package vrp.algorithms.ruinAndRecreate.api;

import vrp.algorithms.ruinAndRecreate.RuinAndRecreateEvent;

/**
 * 
 * @author stefan schroeder
 *
 */

public interface RuinAndRecreateListener {
	public void inform(RuinAndRecreateEvent event);
	public void finish();
}
