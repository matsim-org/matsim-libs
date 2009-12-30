package org.matsim.vis.otfvis.caching;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;


/**
 * 
 * The OTFVis client needs to keep some client data which doesn't belong in any low-level graphics buffers
 * or queries or control elements (!) or anywhere else.
 * 
 * This is an attempt to establish something like that.
 * 
 * @author michaz
 *
 */
public class ClientDataBase {
	
	private static ClientDataBase instance;
	
	public static synchronized ClientDataBase getInstance() {
		if (instance == null) {
			instance = new ClientDataBase();
		}
		return instance;
	}

	private Map<Id, Id> piggyBackingMap = new HashMap<Id, Id>();

	/**
	 * A map which tells us which agent is riding along with which other agent.
	 * Needed for public transit. 1->3 means we thing agent 1 is riding along with agent 3 at the moment.
	 * 
	 * @return the piggy-backing map
	 */
	public Map<Id, Id> getPiggyBackingMap() {
		return piggyBackingMap;
	}
	

}
