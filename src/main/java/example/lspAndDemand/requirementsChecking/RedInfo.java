/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package example.lspAndDemand.requirementsChecking;

import lsp.LSPInfo;

/*package-private*/ class RedInfo extends LSPInfo {

	/*package-private*/ RedInfo() {
		this.getAttributes().putAttribute( "red", null );
	}

	@Override
	public String getName() {
		return "red";
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
	}

}
