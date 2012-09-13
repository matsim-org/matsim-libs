///* *********************************************************************** *
// * project: org.matsim.*
// * PlotDataFile.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2011 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//package playground.thibautd.analysis.possiblesharedrides;
//
///**
// * @author thibautd
// */
//public class PlotDataFile {
//	private static final double DIST=500d;
//	private static final double TIME=15*60d;
//
//	public static final void main(String[] args) {
//		String fileName = args[0];
//		String outputFile = args[1];
//		int width = 1024;
//		int height = 800;
//
//		CountPossibleSharedRideNew counter = new CountPossibleSharedRideNew(DIST,TIME);
//		
//		counter.loadTripData(fileName);
//
//		counter.getBoxAndWhiskersPerTimeBin(24).saveAsPng(outputFile+"-per-time-bin.png", width, height);
//		counter.getBoxAndWhiskersPerTimeBin(24,1).saveAsPng(outputFile+"-per-time-bin-1km.png", width, height);
//		counter.getBoxAndWhiskersPerTimeBin(24,2).saveAsPng(outputFile+"-per-time-bin-2km.png", width, height);
//		counter.getBoxAndWhiskersPerTimeBin(24,3).saveAsPng(outputFile+"-per-time-bin-3km.png", width, height);
//
//		counter.getBoxAndWhiskersPerDistanceBin(1,Double.POSITIVE_INFINITY).saveAsPng(outputFile+"-per-dist-bin.png", width, height);
//		counter.getBoxAndWhiskersPerDistanceBin(1,10).saveAsPng(outputFile+"-per-dist-bin-10km.png", width, height);
//		counter.getBoxAndWhiskersPerDistanceBin(1,25).saveAsPng(outputFile+"-per-dist-bin-25km.png", width, height);
//	}
//}
//
