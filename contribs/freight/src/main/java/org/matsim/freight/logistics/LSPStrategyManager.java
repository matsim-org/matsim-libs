/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.logistics;

import jakarta.inject.Provider;
import org.matsim.core.replanning.GenericStrategyManager;

/**
 * The current (jul'22) logic of this is:
 *
 * <ul>
 *   <li>There is a null binding of this interface in {@link LSPModule}. If one wants to use
 *       strategies, this needs to be overwritten.
 *   <li>Normally, the strategy manager is fixed infrastructure, and should just be configured.
 *       However, since it is not yet there before injection, it also cannot be configured before
 *       injection. Core matsim solves that by writing the corresponding configuration into the
 *       config. We could, in principle, do the same here. Don't want to do this yet.
 *   <li>So way to configure this "in code" is to bind {@link LSPStrategyManager} to a {@link
 *       Provider <LSPStrategyManager>} and then configure it in the provider.
 * </ul>
 */
public interface LSPStrategyManager extends GenericStrategyManager<LSPPlan, LSP> {
  // (this is mostly there so that it can be guice-bound.  kai, jul'22)

}
