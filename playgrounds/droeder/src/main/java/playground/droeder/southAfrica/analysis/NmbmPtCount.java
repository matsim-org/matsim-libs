/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.droeder.southAfrica.analysis;


class NmbmPtCount {//extends AbstractAnalyisModule{
//
//	PtPaxVolumesHandler handler;
//	private ArrayList<Id> links;
//	/**
//	 * @param name
//	 */
//	public NmbmPtCount() {
//		super(NmbmPtCount.class.getSimpleName());
//		this.handler = new PtPaxVolumesHandler(3600.); 
//	}
//
//	@Override
//	public List<EventHandler> getEventHandler() {
//		List<EventHandler> handler = new ArrayList<EventHandler>();
//		handler.add(this.handler);
//		return handler;
//	}
//
//	@Override
//	public void preProcessData() {
//		this.links = new ArrayList<Id>();
//		links.add(new IdImpl("90409-90411-90413-90415-90417-90419"));
//		links.add(new IdImpl("90420-90418-90416-90414-90412-90410"));
//		links.add(new IdImpl("20706-20707"));
//		links.add(new IdImpl("72219-72220-72221"));
//		links.add(new IdImpl("72241-72242-72243-72244"));
//		links.add(new IdImpl("20726-20727-20728"));
//		links.add(new IdImpl("24360-24361-24362-24363-24364"));
//		links.add(new IdImpl("218-219-220-221-222"));
//		links.add(new IdImpl("34580-34581-34582-34583-34584"));
//		links.add(new IdImpl("73503-73504"));
//		links.add(new IdImpl("53096-53097-53098"));
//		links.add(new IdImpl("78332-78333-78334"));
//		links.add(new IdImpl("18607-18605-18603-18601-18599-18597-18595-18593-18591-18589-18587-18585-18583-18581-18579-18577"));
//		links.add(new IdImpl("18576-18578-18580-18582-18584-18586-18588-18590-18592-18594-18596-18598-18600-18602-18604"));
//	}
//
//	@Override
//	public void postProcessData() {
//		
//	}
//
//	@Override
//	public void writeResults(String outputFolder) {
//		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder + "ptPaxVolumes.csv");
//		try {
//			//header
//			writer.write("LinkId;total;");
//			for(int i = 0; i < this.handler.getMaxInterval() + 1; i++){
//					writer.write(String.valueOf(i) + ";");
//			}
//			writer.newLine();
//			//content
//			for(Id id: this.links){
//				writer.write(id.toString() + ";");
//				writer.write(this.handler.getPaxCountForLinkId(id) + ";");
//				for(int i = 0; i < this.handler.getMaxInterval() + 1; i++){
//					writer.write(this.handler.getPaxCountForLinkId(id, i) + ";");
//				}
//				writer.newLine();
//			}
//			writer.flush();
//			writer.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
}