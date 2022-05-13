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

package example.lsp.simulationTrackers;

import lsp.LSPInfo;

/*package-private*/ class CostInfo extends LSPInfo {

	CostInfo() {
		setFixedCost( null );
		setVariableCost( null );
	}
	void setVariableCost( Double value ){
		this.getAttributes().putAttribute( "variableCost", value );
	}
	void setFixedCost( Double value ){
		this.getAttributes().putAttribute( "fixedCost", value );
	}
	Double getFixedCost() {
		return (Double) this.getAttributes().getAttribute( "fixedCost" );
	}
	Double getVariableCost(){
		return (Double) this.getAttributes().getAttribute( "variableCost" );
	}


	@Override
	public String getName() {
		return "cost_function";
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
