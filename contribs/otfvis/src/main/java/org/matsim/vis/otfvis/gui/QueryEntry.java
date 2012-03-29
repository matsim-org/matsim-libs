/**
 * 
 */
package org.matsim.vis.otfvis.gui;

import org.matsim.vis.otfvis.interfaces.OTFQuery;

public class QueryEntry {
	public String shortName;
	public String toolTip;
	public Class<? extends OTFQuery> clazz;

	public QueryEntry(String string, String string2,
			Class<? extends OTFQuery> class1) {
		this.shortName = string;
		this.toolTip = string2;
		this.clazz = class1;
	}

	@Override
	public String toString() {
		return shortName;
	}

}