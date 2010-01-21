package org.matsim.vis.otfvis.interfaces;

import java.io.Serializable;

public interface OTFQueryResult extends Serializable {
	
	/**
	 * Everytime the display needs to be refreshed this 
	 * method is called for every active Query.
	 * 
	 * @param OTFDrawer drawer The drawer class responsible for 
	 * refreshing the view. Use this to identify which drawing 
	 * routines to use
	 */
	public void draw(OTFDrawer drawer);

	/**
	 * Remove is called when a query is removed, to give the query the option to
	 * cleanup things.
	 * 
	 */
	public void remove();
	
	/**
	 * As long as this returns true, the query will be called every time step.
	 * 
	 * @return boolean indicated if query needs updating
	 */
	public boolean isAlive();
	
}
