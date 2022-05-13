
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

package lsp;

import java.util.ArrayList;
import java.util.Collection;

/* package-private */ class LSPPlanImpl implements LSPPlan{

	private LSP lsp;
	private double score;
	private final Collection<LogisticsSolution> solutions;
	private ShipmentAssigner assigner;
	
	LSPPlanImpl() {
		this.solutions = new ArrayList<>();
	}
	
	@Override public LSPPlan addSolution( LogisticsSolution solution ) {
		this.solutions.add(solution);
		solution.setLSP(this.lsp);
		return this;
	}
	
	@Override public Collection<LogisticsSolution> getSolutions() {
		return solutions;
	}

	@Override public ShipmentAssigner getAssigner() {
		return assigner;
	}
	
	@Override public LSPPlan setAssigner( ShipmentAssigner assigner ) {
		this.assigner = assigner;
		this.assigner.setLSP(this.lsp);
		return this;
	}

	@Override
	public Double getScore() {
		return score;
	}

	@Override
	public void setScore(Double score) {
		this.score = score;
	}

	@Override public void setLSP(LSP lsp ) {
		this.lsp = lsp;
		if(assigner != null) {
			this.assigner.setLSP(lsp);
		}
		for(LogisticsSolution solution : solutions) {
			solution.setLSP(lsp);
		}
	}
	
	@Override public LSP getLsp() {
		return lsp;
	}
	
}
