/* *********************************************************************** *
 * project: org.matsim.*
 * GAUtils.java
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

package playground.meisterk.org.matsim.utils;

public class GAUtils {

	public static long getGreenhalghAndMarshallValue(double pOpt, int stringLength, int popSize, double pMut) {

		System.out.println("pOpt = " + Double.toString(pOpt));
		System.out.println("stringLength = " + Integer.toString(stringLength));
		System.out.println("popSize = " + Integer.toString(popSize));
		System.out.println("pMut = " + Double.toString(pMut));

		double numerator = Math.log(1 - pOpt);
		double denominator = popSize * Math.log(1 - Math.min(Math.pow((1 - pMut), (stringLength - 1)) * pMut, Math.pow(pMut, stringLength)));

		double numGen = numerator / denominator;

		return (long) Math.ceil(numGen);

	}

	public static long getAytugAndKoehler2000Value(double pOpt, int stringLength, int popSize, double pMut) {

		double numerator = Math.log(1 - pOpt);

		double denominator = popSize * Math.log(1 - Math.min(Math.pow(pMut, stringLength), Math.pow(1 - pMut, stringLength)));

		double numGen = numerator / denominator;

		return (long) Math.ceil(numGen);

	}

	public static void main(String[] args) {
		System.out.println(
				"Greenhalgh and Marshall: " + 
				Long.toString(GAUtils.getGreenhalghAndMarshallValue(
						Double.parseDouble(args[0]),
						Integer.parseInt(args[1]), 
						Integer.parseInt(args[2]), 
						Double.parseDouble(args[3])))); 
		System.out.println(
				"Aytug and Koehler (2000): " + 
				Long.toString(GAUtils.getAytugAndKoehler2000Value(
						Double.parseDouble(args[0]),
						Integer.parseInt(args[1]), 
						Integer.parseInt(args[2]), 
						Double.parseDouble(args[3])))); 
	}

}
