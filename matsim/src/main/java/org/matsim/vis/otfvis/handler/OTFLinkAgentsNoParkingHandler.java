/* *********************************************************************** *
 * project: org.matsim.*
 * OTFLinkAgentsNoParkingHandler.java
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

package org.matsim.vis.otfvis.handler;



/**
 * OTFLinkAgentsNoParkingHandler enables the user (per preferences) to select whether she/he wants to
 * see parked vehicles or not.
 * <p/>
 * I don't understand this.  There is this class which writes all agents that are not at an 
 * activity (= not parked) ... but then there is also a "not parked" flag in the OTFLinkAgentsHandler
 * that seems to be used.  Having both approaches at the same time does not make sense to me.  
 * <p/>
 * The class itself presumably needs to say because of the usual ConnectionManager/mvi issues.
 * kai, jan'11
 *
 * @author david
 *
 */
public class OTFLinkAgentsNoParkingHandler extends OTFLinkAgentsHandler {

	static public class Writer extends  OTFLinkAgentsHandler.Writer {
		private static final long serialVersionUID = 6541770536927233851L;

// I am somewhat hopeful that none of this is needed.  
// The classes need to stay there as usual since we don't know in which mvi files they are used.
//		
//		/**The API method is writeDynData.  writeDynData calls writeAgent.  Could make it "protected", but I don't
//		 * want proliferation of inheritance all over the project.  Leaving it "package-private" so that inheritance
//		 * within the package is possible.  kai, jan'11  
//		 */
//		@Override
//		void writeAllAgents(ByteBuffer out) {
//			// Write additional agent data
//			positions.clear();
////			this.src.getVisData().getVehiclePositions(this.src.getQSimEngine().getQSim().getSimTimer().getTimeOfDay(), positions);
//			this.src.getVisData().getVehiclePositions( positions);
//			int valid = 0;
//			for (AgentSnapshotInfo pos : positions) {
//				if (pos.getAgentState() != AgentState.PERSON_AT_ACTIVITY) valid++;
//			}
//			out.putInt(valid);
//
//			for (AgentSnapshotInfo pos : positions) {
//				if (pos.getAgentState() != AgentState.PERSON_AT_ACTIVITY) writeAgent(pos, out);
//			}
//		}
//
//		@Override
//		public OTFDataWriter<VisLink> getWriter() {
//			return new Writer();
//		}

	}

}
