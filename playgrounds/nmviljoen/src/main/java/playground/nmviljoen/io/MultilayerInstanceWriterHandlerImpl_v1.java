/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreVehicleWriterHandlerImpl_v0.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.nmviljoen.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import playground.nmviljoen.gridExperiments.GridExperiment;
import playground.nmviljoen.gridExperiments.NmvLink;
import playground.nmviljoen.gridExperiments.NmvNode;

public class MultilayerInstanceWriterHandlerImpl_v1 implements
		MultilayerInstanceWriterHandler {
	private final GridExperiment experiment;
	
	public MultilayerInstanceWriterHandlerImpl_v1(GridExperiment experiment){
		this.experiment = experiment;
	}

	@Override
	public void startInstance(BufferedWriter out) throws IOException {
		out.write("\n<multilayerNetwork");
		out.write(" archetype=\"" + experiment.getArchetype().getDescription() + "\"");
		out.write(" number=\"" + experiment.getInstanceNumber() + "\"");
		out.write(">\n");
	}

	@Override
	public void endInstance(BufferedWriter out) throws IOException {
		out.write("</multilayerNetwork>");
	}

	@Override
	public void startPhysicalNetwork(BufferedWriter out) throws IOException {
		out.write("\t<physicalNetwork>\n");
	}

	@Override
	public void endPhysicalNetwork(BufferedWriter out) throws IOException {
		out.write("\t</physicalNetwork>\n");
	}

	@Override
	public void startPhysicalNodes(BufferedWriter out) throws IOException {
		out.write("\t\t<physicalNodes>\n");
	}
	
	@Override
	public void endPhysicalNodes(BufferedWriter out) throws IOException {
		out.write("\t\t</physicalNodes>\n");
	}
	
	@Override
	public void startPhysicalNode(BufferedWriter out, NmvNode node) throws IOException {
		out.write("\t\t\t<physicalNode id=\"" 
				+ node.getId() 
				+ "\" x=\"" + node.getXAsString()
				+ "\" y=\"" + node.getYAsString() + "\"");
	}

	@Override
	public void endPhysicalNode(BufferedWriter out) throws IOException {
		out.write("/>\n");
	}

	@Override
	public void startPhysicalEdges(BufferedWriter out) throws IOException {
		out.write("\n\t\t<physicalEdges>\n");
	}
	
	@Override
	public void endPhysicalEdges(BufferedWriter out) throws IOException {
		out.write("\t\t</physicalEdges>\n");
	}
	
	@Override
	public void startPhysicalEdge(BufferedWriter out, NmvLink link) throws IOException {
		String[] sa = link.getId().split("_");
		out.write("\t\t\t<physicalEdge fromId=\"" + sa[0] + "\" toId=\"" + sa[1] + "\" weight=\"" + link.getWeight() + "\"");
	}

	@Override
	public void endPhysicalEdge(BufferedWriter out) throws IOException {
		out.write("/>\n");
	}

	@Override
	public void startLogicalNetwork(BufferedWriter out) throws IOException {
		out.write("\n\t<logicalNetwork>\n");
	}

	@Override
	public void endLogicalNetwork(BufferedWriter out) throws IOException {
		out.write("\t</logicalNetwork>\n");
	}

	@Override
	public void startLogicalNodes(BufferedWriter out) throws IOException {
		out.write("\t\t<logicalNodes>\n");
	}

	@Override
	public void endLogicalNodes(BufferedWriter out) throws IOException {
		out.write("\t\t</logicalNodes>\n");
	}

	@Override
	public void startLogicalNode(BufferedWriter out, NmvNode node) throws IOException {
		out.write("\t\t\t<logicalNode id=\"" + node.getId() + "\"");
		if(node.getXAsString() != null){
			out.write(" x=\"" + node.getXAsString() + "\"");
		}
		if(node.getYAsString() != null){
			out.write(" y=\"" + node.getYAsString() + "\"");
		}
		//TODO Add optional name and capacity
	}

	@Override
	public void endLogicalNode(BufferedWriter out) throws IOException {
		out.write("/>\n");
	}

	@Override
	public void startLogicalEdges(BufferedWriter out) throws IOException {
		out.write("\n\t\t<logicalEdges>\n");
	}

	@Override
	public void endLogicalEdges(BufferedWriter out) throws IOException {
		out.write("\t\t</logicalEdges>\n");
	}

	@Override
	public void startLogicalEdge(BufferedWriter out, NmvLink link) throws IOException {
		String[] sa = link.getId().split("_");
		out.write("\t\t\t<logicalEdge fromId=\"" + sa[0] + "\" toId=\"" + sa[1] + "\" weight=\"" + link.getWeight() + "\"");
	}

	@Override
	public void endLogicalEdge(BufferedWriter out) throws IOException {
		out.write("/>\n");
	}

	
	@Override
	public void startAssociations(BufferedWriter out) throws IOException {
		out.write("\n\t<associations>\n");
	}

	@Override
	public void endAssociations(BufferedWriter out) throws IOException {
		out.write("\t</associations>\n");
	}

	@Override
	public void startAssociation(BufferedWriter out, String logicalId, String physicalId) throws IOException {
		out.write("\t\t<association " 
				+ "logicalId=\"" + logicalId
				+ "\" physicalId=\"" + physicalId
				+ "\"");
	}

	@Override
	public void endAssociation(BufferedWriter out) throws IOException {
		out.write("/>\n");
	}

	@Override
	public void startSets(BufferedWriter out) throws IOException {
		out.write("\n\t<shortestPathSets>\n");
	}

	@Override
	public void endSets(BufferedWriter out) throws IOException {
		out.write("\t</shortestPathSets>\n");
	}

	@Override
	public void startSet(BufferedWriter out, String fromId, String toId) throws IOException {
		out.write("\t\t<set fromId=\"" + fromId
				+ "\" toId=\"" + toId
				+ "\">\n");
	}

	@Override
	public void endSet(BufferedWriter out) throws IOException {
		out.write("\t\t</set>\n");
	}

	@Override
	public void startPath(BufferedWriter out, List<String> path) throws IOException {
		out.write("\t\t\t<path>");
		for(String s : path){
			out.write(" " + s);
		}
	}

	@Override
	public void endPath(BufferedWriter out) throws IOException {
		out.write("</path>\n");
	}

	@Override
	public void writeSeparator(BufferedWriter out) throws IOException {
		/* No separator. */
	}


//	@Override
//	public void startChain(BufferedWriter out)
//			throws IOException {
//		out.write("\t\t<chain>\n");
//	}
//
//	@Override
//	public void endChain(BufferedWriter out) throws IOException {
//		out.write("\t\t</chain>\n");
//	}
//
//	@Override
//	public void startActivity(DigicoreActivity activity, BufferedWriter out)
//			throws IOException {
//		out.write("\t\t\t<activity");
//		out.write(" type=\"" + activity.getType() + "\"\n");
//		out.write("\t\t\t\tstart=\"" + getDateString(activity.getStartTimeGregorianCalendar()) + "\"");
//		out.write(" end=\"" + getDateString(activity.getEndTimeGregorianCalendar()) + "\"\n");
//		out.write("\t\t\t\tx=\"" + String.format("%.2f", activity.getCoord().getX()) + "\"");
//		out.write(" y=\"" + String.format("%.2f", activity.getCoord().getY()) + "\"");
//		if(activity.getFacilityId() != null){
//			out.write(" facility=\"" + activity.getFacilityId().toString() + "\"");
//		}
//		if(activity.getLinkId() != null){
//			out.write(" link=\"" + activity.getLinkId().toString() + "\"");
//		}
//	}
//
//	@Override
//	public void endActivity(BufferedWriter out) throws IOException {
//		out.write("/>\n");
//		
//	}
//
//	@Override
//	public void writeSeparator(BufferedWriter out) throws IOException {
//		/* Don't think a separator will make the file more readable. */
//	}
//	
//	private String getDateString(GregorianCalendar cal){
//		String s = "";
//		int year = cal.get(Calendar.YEAR);
//		int month = cal.get(Calendar.MONTH)+1; // Seems to be a java thing that month is started at 0... 
//		int day = cal.get(Calendar.DAY_OF_MONTH);
//		int hour = cal.get(Calendar.HOUR_OF_DAY);
//		int minute = cal.get(Calendar.MINUTE);
//		int second = cal.get(Calendar.SECOND);
//			
//		s = String.format("%04d%02d%02d %02d:%02d:%02d", 
//				year, month, day, hour, minute, second);
//		
//		return s;
//	}
//
//	@Override
//	public void startTrace(DigicoreTrace trace, BufferedWriter out) throws IOException {
//		/* Do nothing: implemented from v2 */
//	}
//
//	@Override
//	public void endTrace(BufferedWriter out) throws IOException {
//		/* Do nothing: implemented from v2 */
//	}
//
//	@Override
//	public void startPosition(DigicorePosition pos, BufferedWriter out) throws IOException {
//		/* Do nothing: implemented from v2 */
//	}
//
//	@Override
//	public void endPosition(BufferedWriter out) throws IOException {
//		/* Do nothing: implemented from v2 */
//	}


}

