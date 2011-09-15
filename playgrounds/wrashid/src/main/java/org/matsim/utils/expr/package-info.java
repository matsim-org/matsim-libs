/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

/**
 * This package contains an apdapted version of the parser, implemented by Darius Bacon.
 * For more information about the original code, see: https://github.com/darius/expr
 * 
 * <h2>Package Maintainer(s):</h2>
 * <ul>
 *   <li>Rashid Waraich</li>
 * </ul>
 * 
 * Simple usage:
 * 
 * 	Parser parser=new Parser("x+y^2");
 *	parser.setVariable("x", 1);
 *	parser.setVariable("y", 2);
 *	double result=parser.parse();
 * 
 * Notes:
 *  Same parser can be used multiple times with different variable assignment.
 * 
 * Changes by non-maintainers are strictly prohibited.
 */
package org.matsim.utils.expr;