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

package lsp.replanning;

import lsp.LSP;
import lsp.LSPPlan;
import org.matsim.core.replanning.GenericStrategyManager;

public final class LSPReplanningUtils {
	private LSPReplanningUtils(){
	}
	public static LSPReplanner createDefaultLSPReplanner( GenericStrategyManager<LSPPlan, LSP> strategyManager ) {
		return new LSPReplannerImpl( strategyManager );
	}
}
