/*
 *   *********************************************************************** *
 *   project: org.matsim.*													 *
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2025 by the members listed in the COPYING, 		 *
 *                          LICENSE and WARRANTY file.  					 *
 *   email           : info at matsim dot org   							 *
 *                                                                         	 *
 *   *********************************************************************** *
 *                                                                        	 *
 *     This program is free software; you can redistribute it and/or modify  *
 *      it under the terms of the GNU General Public License as published by *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.								     *
 *     See also COPYING, LICENSE and WARRANTY file						 	 *
 *                                                                           *
 *   *********************************************************************** *
 */


/**
 * Package that contains classes for the freight logistics module.
 * This module is used for modeling and simulating logistics operations in MATSim.
 * The central agent  in this module is the LSP (Logistics Service Provider): {@link org.matsim.freight.logistics.LSP}.
 * <p>
 * Some references:
 * <ol>
 * 	<li> K. Martins-Turner, K. Nagel (2025). Agent-based solving of the 2-echelon Vehicle Routing Problem; Transportation Research Procedia, Volume 82, 2025, Pages 3912-3924.(VSP working Paper 22-22) <a href="https://doi.org/10.1016/j.trpro.2024.12.005">https://doi.org/10.1016/j.trpro.2024.12.005</a>
 * 	</li>
 * 	<li> N. Richter, K. Martins-Turner, K. Nagel (2024). Extension of an agent-based simulation for the optimized allocation of freight requests to differently structured supply chains; Procedia Computer Science, Volume 238, 2024, pages 728-735. <a href="https://doi.org/10.1016/j.procs.2024.06.084">https://doi.org/10.1016/j.procs.2024.06.084</a>
 *  </li>
 *  <li>Matteis, T., W. Wisetjindawat, and G. Liedtke (2019). Modelling interactions between freight forwarders and recipients – An extension of the MATSim toolkit. In 15th World Conference on Transport Research. <a href="https://elib.dlr.de/134376/">https://elib.dlr.de/134376/</a>
 *  </li>
 * </ol>
 * and the upcoming PhD theses of Tilman Matteis and Kai Martins-Turner.
 * <p>
 * The first implementation of this module was written by Tilman Matteis (DLR), many changes and updates were made by nagel and kturner (TUB-VSP)
 */
package org.matsim.freight.logistics;
