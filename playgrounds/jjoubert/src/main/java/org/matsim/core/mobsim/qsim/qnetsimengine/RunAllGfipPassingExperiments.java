/* *********************************************************************** *
 * project: org.matsim.*
 * RunAllGfipPassingExperiments.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
 * 
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.core.mobsim.qsim.qnetsimengine.GfipMultimodalQSimFactory.QueueType;

import playground.southafrica.utilities.Header;

/**
 * A single class to run all the experiments required for the GFIP-passing 
 * paper (working paper 047) prepared by Joubert & De Koker.
 * 
 * @author jwjoubert
 */
public class RunAllGfipPassingExperiments {
	final private static String NETWORK_0050 = "networks/triLink_0050.xml.gz";
	final private static String NETWORK_0050_START = "23_l0001";
	final private static String NETWORK_0050_END = "23_l0100";
	final private static String NETWORK_0100 = "networks/triLink_0100.xml.gz";
	final private static String NETWORK_0100_START = "23_l0001";
	final private static String NETWORK_0100_END = "23_l0050";
	final private static String NETWORK_0200 = "networks/triLink_0200.xml.gz";
	final private static String NETWORK_0200_START = "23_l0001";
	final private static String NETWORK_0200_END = "23_l0025";
	final private static String NETWORK_0500 = "networks/triLink_0500.xml.gz";
	final private static String NETWORK_0500_START = "23_l0001";
	final private static String NETWORK_0500_END = "23_l0010";
	final private static String NETWORK_1000 = "networks/triLink_1000.xml.gz";
	final private static String NETWORK_1000_START = "23_l0001";
	final private static String NETWORK_1000_END = "23_l0005";
	final private static String NETWORK_5000 = "networks/triLink.xml.gz";
	final private static String NETWORK_5000_START = "23";
	final private static String NETWORK_5000_END = "23";
	
	private static String PATH;

	final private static String fifo = QueueType.FIFO.name();
	final private static String basicPassing = QueueType.BASIC_PASSING.name();
	final private static String gfipFifo = QueueType.GFIP_FIFO.name(); 
	final private static String gfipPassing = QueueType.GFIP_PASSING.name(); 

	/**
	 * 
	 * @param args Only a single argument is required, namely the absolute path to the 
	 * clone folder of the working paper 047 repository.
	 */
	public static void main(String[] args) {
		Header.printHeader(RunAllGfipPassingExperiments.class.toString(), args);
		PATH = args[0];
		PATH += PATH.endsWith("/") ? "" : "/";
		
		runFifoExperiments();
		runBasicPassingExperiments();
		runGfipFifoExperiments();
		runGfipPassingExperiments();
		
		Header.printFooter();
	}
	
	private static void runFifoExperiments(){
		GfipQueuePassingControler.main(getArgs(fifo, 200, 17000));
		
		GfipQueuePassingControler.main(getArgs(fifo, 5000, 30000));
	}
	
	private static void runBasicPassingExperiments(){
		GfipQueuePassingControler.main(getArgs(basicPassing, 200, 17000));

		GfipQueuePassingControler.main(getArgs(basicPassing, 5000, 30000));
	}
	
	private static void runGfipFifoExperiments(){
		GfipQueuePassingControler.main(getArgs(gfipFifo, 200, 17000));

		GfipQueuePassingControler.main(getArgs(gfipFifo, 5000, 30000));
	}

	private static void runGfipPassingExperiments(){
		/*=======================  Population of 5000  ==================== */
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 5000));
		
		/*=======================  Population of 10000  =================== */
		GfipQueuePassingControler.main(getArgs(gfipPassing, 50, 10000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 100, 10000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 10000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 500, 10000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 1000, 10000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 5000, 10000));
		
		/*====================  Population of 11K - 29K  ================== */
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 11000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 12000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 13000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 14000));
		
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 16000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 17000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 18000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 19000));
		
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 21000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 22000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 23000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 24000));
		
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 26000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 27000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 28000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 29000));
		
		/*=======================  Population of 15000  =================== */
		GfipQueuePassingControler.main(getArgs(gfipPassing, 50, 15000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 100, 15000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 15000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 500, 15000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 1000, 15000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 5000, 15000));
		
		/*=======================  Population of 20000  =================== */
		GfipQueuePassingControler.main(getArgs(gfipPassing, 50, 20000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 100, 20000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 20000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 500, 20000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 1000, 20000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 5000, 20000));
		
		/*=======================  Population of 25000  =================== */
		GfipQueuePassingControler.main(getArgs(gfipPassing, 50, 25000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 100, 25000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 25000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 500, 25000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 1000, 25000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 5000, 25000));
		
		/*=======================  Population of 30000  =================== */
		GfipQueuePassingControler.main(getArgs(gfipPassing, 50, 30000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 100, 30000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 30000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 500, 30000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 1000, 30000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 5000, 30000));
		
		/*=======================  Population of 35000  =================== */
		GfipQueuePassingControler.main(getArgs(gfipPassing, 50, 35000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 100, 35000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 200, 35000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 500, 35000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 1000, 35000));
		GfipQueuePassingControler.main(getArgs(gfipPassing, 5000, 35000));
	}
	
	private static String[] getArgs(String passingRegime, int linkLength, int population){
		String[] sa = {"", "", "", "", "", "", ""};
		sa[0] = PATH;
		sa[2] = String.format("%s%s/population%02dK/output_%04d/", PATH, passingRegime, population/1000, linkLength);
		sa[3] = passingRegime;
		sa[6] = String.valueOf(population);
		
		/* Network */
		switch (linkLength) {
		case 50:
			sa[1] = NETWORK_0050;
			sa[4] = NETWORK_0050_START;
			sa[5] = NETWORK_0050_END;
			break;
		case 100:
			sa[1] = NETWORK_0100;
			sa[4] = NETWORK_0100_START;
			sa[5] = NETWORK_0100_END;
			break;
		case 200:
			sa[1] = NETWORK_0200;
			sa[4] = NETWORK_0200_START;
			sa[5] = NETWORK_0200_END;
			break;
		case 500:
			sa[1] = NETWORK_0500;
			sa[4] = NETWORK_0500_START;
			sa[5] = NETWORK_0500_END;
			break;
		case 1000:
			sa[1] = NETWORK_1000;
			sa[4] = NETWORK_1000_START;
			sa[5] = NETWORK_1000_END;
			break;
		case 5000:
			sa[1] = NETWORK_5000;
			sa[4] = NETWORK_5000_START;
			sa[5] = NETWORK_5000_END;
			break;
		default:
			throw new RuntimeException("Don't know how to interpret link length " + linkLength + "m.");
		}
		return sa;
	}


}
