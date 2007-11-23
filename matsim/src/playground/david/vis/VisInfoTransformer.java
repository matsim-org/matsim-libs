/* *********************************************************************** *
 * project: org.matsim.*
 * VisInfoTransformer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.david.vis;

import java.io.DataOutputStream;
import java.io.IOException;

import org.matsim.mobsim.QueueLink;

public class VisInfoTransformer {

	public boolean hasLinkInfo() {return false;};
	public boolean hasNodeInfo(){return false;};
	public boolean hasAgentInfo(){return false;};
	
	public void writeLinkInfo( QueueLink link, DataOutputStream out) throws IOException{};
	public void writeNodeInfo( QueueLink link, DataOutputStream out) throws IOException{};
	public void writeAgentInfo( QueueLink link, DataOutputStream out) throws IOException{};
	
}
