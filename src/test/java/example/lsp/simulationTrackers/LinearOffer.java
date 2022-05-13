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

import demand.decoratedLSP.LSPDecorator;
import demand.decoratedLSP.LogisticsSolutionDecorator;
import demand.offer.Offer;
import demand.offer.OfferVisitor;
import lsp.LSPInfo;

import java.util.Random;


public class LinearOffer implements Offer{

	private LSPDecorator  lsp;
	private LogisticsSolutionDecorator solution;
	private final String type;
	private double fix;
	private double linear;
	
	public LinearOffer(LogisticsSolutionDecorator solution) {
		this.lsp =  solution.getLSP();
		this.solution = solution;
		this.type = "linear";
		Random random = new Random(1);
		fix = random.nextDouble() * 10;
		linear = random.nextDouble() * 10;
	}
	
	@Override
	public LSPDecorator getLsp() {
		return lsp;
	}

	@Override
	public LogisticsSolutionDecorator getSolution() {
		return solution;
	}

	@Override
	public String getType() {
		return type;
	}

	public double getFix() {
		return fix;
	}

	public void setFix(double fix) {
		this.fix = fix;
	}

	public double getLinear() {
		return linear;
	}

	public void setLinear(double linear) {
		this.linear = linear;
	}

	@Override
	public void accept(OfferVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void update() {
		for(LSPInfo info : solution.getInfos()) {
			if(info instanceof CostInfo ) {
				CostInfo costInfo = (CostInfo) info;
				this.fix = costInfo.getFixedCost();
				this.linear = costInfo.getVariableCost();
			}
		}
	}

	@Override
	public void setLSP(LSPDecorator lsp) {
		this.lsp = lsp;
	}

	@Override
	public void setSolution(LogisticsSolutionDecorator solution) {
		this.solution = solution;
	}

}
