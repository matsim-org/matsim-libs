/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.pieter.singapore.hits;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

class HITSElement implements Serializable {
	/**
	 * Awful hack to return trimmed strings
	 * 
	 * @param prs
	 * @param string
	 * @return
	 * @throws SQLException
	 */
	String getTrimmedStringFromResultSet(ResultSet prs, String string) throws SQLException {
		String outString = prs.getString(string);
		if (outString != null)
			return outString.trim();
		else
			return null;

	}
}
