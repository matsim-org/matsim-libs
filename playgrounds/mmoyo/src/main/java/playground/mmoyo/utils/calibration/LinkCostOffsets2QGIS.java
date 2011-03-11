///* *********************************************************************** *
// * project: org.matsim.*
// * LinkCostOffsets2QGISTest.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
//
//package playground.mmoyo.utils.calibration;
//
//import java.util.List;
//import java.util.Map;
//import java.util.TreeMap;
//
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.core.network.NetworkImpl;
//import org.matsim.counts.Counts;
//import org.matsim.counts.Count;
//import org.matsim.counts.Volume;
//import org.matsim.pt.transitSchedule.api.TransitRoute;
//import org.matsim.pt.transitSchedule.api.TransitRouteStop;
//import org.matsim.pt.transitSchedule.api.TransitSchedule;
//import org.matsim.pt.transitSchedule.api.TransitStopFacility;
//
//import playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy.PtBseLinkCostOffsetsXMLFileIO;
//import playground.mmoyo.utils.DataLoader;
//import playground.mmoyo.utils.TransitRouteUtil;
//import playground.yu.integration.cadyts.demandCalibration.withCarCounts.utils.qgis.LinkCostOffsets2QGISWithArrowhead;
//import playground.yu.utils.qgis.X2QGIS;
//
//import cadyts.utilities.misc.DynamicData;
//
///**
// * @author Manuel
// */
//public class LinkCostOffsets2QGIS {
//	final String netFilePath;
//	final NetworkImpl net;
//	final DynamicData<TransitStopFacility> stopOffsets;
//	final Counts counts;
//	final TransitSchedule schedule;
//	final String outDir;
//	
//	public LinkCostOffsets2QGIS(final String netFilePath, final NetworkImpl net, final DynamicData<TransitStopFacility> stopOffsets,   final Counts counts, final TransitSchedule schedule,  final String outDir){
//		this.netFilePath = netFilePath;
//		this.net = net;
//		this.stopOffsets = stopOffsets;
//		this.counts = counts;
//		this.schedule = schedule;
//		this.outDir = outDir; 
//	}
//	
//	public void run(final TransitRoute trRoute, final int arStartTime, final int arEndTime){
//
//		//transform stopOffsets into linkOffsets
//		Map <Id, Link> routeLinkMap = new TreeMap <Id, Link>();  
//		Map <Id, Id> stop2LinkMap = new TreeMap <Id, Id>();
//		int binCnt = stopOffsets.getBinCnt();
//		DynamicData<Link> linkOffsets= new DynamicData<Link>(stopOffsets.getStartTime_s(), stopOffsets.getBinSize_s(), binCnt);
//		TransitRouteUtil ptRouteUtil = new TransitRouteUtil(this.net, trRoute);
//		
//		for (TransitRouteStop stop: trRoute.getStops()){  //only considers stops belonging to the given trRoute!
//			if (stop!=null){ 
//				List <Link> outLinkList = ptRouteUtil.getOutLinks(stop, trRoute);  //get all trRoute links by getting the outgoing links of stops 
//				for(Link outLink : outLinkList){
//					routeLinkMap.put(outLink.getId(), outLink);
//					
//					//System.out.println(stopFac.getId() + " " + link.getId() );
//					stop2LinkMap.put(stop.getStopFacility().getId(), outLink.getId());
//					for (int bin=0; bin<binCnt; bin++){
//						linkOffsets.add(outLink, bin, stopOffsets.getBinValue(stop.getStopFacility(), bin));
//					}
//					
//					System.out.println(stop.getStopFacility().getId() + " " + outLink.getId() );
//					
//				}
//				
//				//transform stopCounts into link counts
//				Counts linkCounts = new Counts();
//				linkCounts.setName(counts.getName());
//				linkCounts.setDescription(counts.getDescription());
//				linkCounts.setYear(counts.getYear());
//				for (Count stopCount : counts.getCounts().values()){
//					Id LinkId = stop2LinkMap.get(stopCount.getLocId());
//					if (LinkId != null){
//						Count linkCount = linkCounts.createCount(stop2LinkMap.get(stopCount.getLocId()), stopCount.getCsId());
//						linkCount.setCoord(stopCount.getCoord());
//						for(Volume vol : stopCount.getVolumes().values() ){
//							linkCount.createVolume(vol.getHour(), vol.getValue());
//						}
//					}
//
//				}
//			}
//		}			
//		
//		for (int i = arStartTime; i <= arEndTime; i++) {
//			LinkCostOffsets2QGISWithArrowhead lco2QGSI = new LinkCostOffsets2QGISWithArrowhead (i, i, this.netFilePath, X2QGIS.gk4);
//			lco2QGSI.createLinkCostOffsets(routeLinkMap.values(), linkOffsets);
//			lco2QGSI.output(routeLinkMap.keySet(), outDir);
//		}
//		
//		routeLinkMap = null;
//		stop2LinkMap = null;
//		linkOffsets = null;
//		ptRouteUtil = null;
//	}
//	
//	public static void main(String[] args) {
//		String netFilePath;
//		String linkCostOffsetFilePath;
//		String countsFilePath;
//		String transitScheduleFilePath;
//		String outputDir;
//		String strRouteId;
//		
//		if (args.length ==0){
//			netFilePath = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
//			linkCostOffsetFilePath = "../playgrounds/mmoyo/output/tmp/500.linkCostOffsets.xml";
//			countsFilePath = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/chen/counts_occupancy_M44.xml";
//			transitScheduleFilePath = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
//			outputDir = "../playgrounds/mmoyo/output/tmp/output/";
//			strRouteId = "B-M44.101.901.H";
//		}else{
//			netFilePath = args[0];
//			linkCostOffsetFilePath = args[1];
//			countsFilePath = args[2];
//			transitScheduleFilePath = args[3];
//			outputDir = args[4];
//			strRouteId = args[5];
//		}
//		
//		//load data
//		DataLoader dLoader = new DataLoader();
//		NetworkImpl net =dLoader.readNetwork(netFilePath);
//		Counts counts = dLoader.readCounts(countsFilePath);
//		TransitSchedule schedule = dLoader.readTransitSchedule(net, transitScheduleFilePath);
//		PtBseLinkCostOffsetsXMLFileIO reader = new PtBseLinkCostOffsetsXMLFileIO (schedule);
//		DynamicData<TransitStopFacility> stopOffsets = reader.read(linkCostOffsetFilePath);
//		TransitRoute trRoute = dLoader.getTransitRoute(strRouteId, schedule);
//		
//		LinkCostOffsets2QGIS linkCostOffsets2QGIS = new LinkCostOffsets2QGIS( netFilePath, net, stopOffsets, counts, schedule,  outputDir);
//		linkCostOffsets2QGIS.run(trRoute, 6, 20);
//		
//	}
//}
