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
import lsp.LSPs;

public class LSPReplanningUtils {
	public static LSPReplannerImpl createDefaultLSPReplanner(LSP lsp) {
		return new LSPReplannerImpl(lsp);
	}

	public static LSPReplanningModuleImpl createDefaultLSPReplanningModule(LSPs lsps) {
		return new LSPReplanningModuleImpl(lsps);
	}
}
