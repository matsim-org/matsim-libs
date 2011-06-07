/* *********************************************************************** *
 * project: org.matsim.*
 * NOGATypes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.energyflows.facilities;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/*
 * Mapping between NOGA Types and MATSim Activity types.
 */
public class NOGATypes {
		
	public static final Set<Integer> shop_retail_gt2500sqmNOGAs = new TreeSet<Integer>(Arrays.asList(471101));
	
	public static final Set<Integer> shop_retail_get1000sqmNOGAs = new TreeSet<Integer>(Arrays.asList(471102));
	
	public static final Set<Integer> shop_retail_get400sqmNOGAs = new TreeSet<Integer>(Arrays.asList(471103));
	
	public static final Set<Integer> shop_retail_get100sqmNOGAs = new TreeSet<Integer>(Arrays.asList(471104));
	
	public static final Set<Integer> shop_retail_lt100sqmNOGAs = new TreeSet<Integer>(Arrays.asList(471105));
	
	public static final Set<Integer> shop_otherNOGAs = new TreeSet<Integer>(Arrays.asList(
	471901, 471902, 472100, 472200, 472300, 472401, 472402, 472500, 472600, 472901, 472902, 477300, 477400, 477501, 477502, 475100, 
	477101, 477102, 477103, 477104, 477105, 477201, 477202, 475902, 475903, 475300, 475400, 474300, 476300, 475901, 475201, 475202, 
	476100, 476201, 476202, 477601, 477602, 477603, 477801, 477802, 477803, 477700, 474200, 474100, 476500, 476401, 476402, 477804, 
	477805, 477806, 477901, 477902, 479100, 478100, 478900, 478200, 479900, 952300, 952200, 952100, 952500, 952900, 951200, 133000));
	
//	public static final Set<Integer> shopNOGAs = new TreeSet<Integer>(Arrays.asList(
//			471101, 471102, 471103, 471104, 471105, 471901, 471902, 472100, 472200, 472300, 472401, 472402, 472500, 472600, 472901, 472902, 
//			477300, 477400, 477501, 477502, 475100, 477101, 477102, 477103, 477104, 477105, 477201, 477202, 475902, 475903, 475300, 475400, 
//			474300, 476300, 475901, 475201, 475202, 476100, 476201, 476202, 477601, 477602, 477603, 477801, 477802, 477803, 477700, 474200, 
//			474100, 476500, 476401, 476402, 477804, 477805, 477806, 477901, 477902, 479100, 478100, 478900, 478200, 479900, 952300, 952200, 
//			952100, 952500, 952900, 951200, 133000));

	public static final Set<Integer> leisure_gastroNOGAs = new TreeSet<Integer>(Arrays.asList(
			551001, 551003, 551002, 552002, 552003, 553001, 553002, 552001, 559000, 561001, 561003, 561002, 563001, 563002, 562900, 562100));
	
	public static final Set<Integer> leisure_cultureNOGAs = new TreeSet<Integer>(Arrays.asList(
			591100, 592000, 591200, 591300, 591400, 601000, 602000, 900101, 900200, 900102, 900301, 900302, 900400, 799002, 932100, 932900, 
			855200, 639100, 900303, 742001, 910100, 910200, 910300, 910400));

	public static final Set<Integer> leisure_sportsNOGAs = new TreeSet<Integer>(Arrays.asList(931100, 931200, 855100, 931900));
	
//	public static final Set<Integer> leisureNOGAs = new TreeSet<Integer>(Arrays.asList(
//			551001, 551003, 551002, 552002, 552003, 553001, 553002, 552001, 559000, 561001, 561003, 561002, 563001, 563002, 562900, 562100, 
//			591100, 592000, 591200, 591300, 591400, 601000, 602000, 900101, 900200, 900102, 900301, 900302, 900400, 799002, 932100, 932900, 
//			855200, 639100, 900303, 742001, 910100, 910200, 910300, 910400, 931100, 931200, 855100, 931900));
	
	public static final Set<Integer> education_kindergartenNOGAs = new TreeSet<Integer>(Arrays.asList(851000));
	
	public static final Set<Integer> education_primaryNOGAs = new TreeSet<Integer>(Arrays.asList(852001));
	
	public static final Set<Integer> education_secondaryNOGAs = new TreeSet<Integer>(Arrays.asList(853101, 853102, 853103, 853200));

	public static final Set<Integer> education_higherNOGAs = new TreeSet<Integer>(Arrays.asList(854201, 854202, 854203, 854100));

	public static final Set<Integer> education_otherNOGAs = new TreeSet<Integer>(Arrays.asList(855300, 855903, 855200, 855901, 855902, 855904, 855100));
}