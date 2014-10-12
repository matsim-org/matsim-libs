/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.jbischoff.BAsignalsDemand;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.signalsystems.MatsimSignalSystemsReader;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlDataImpl;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlReader20;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signalsystems.model.SignalPlan;
import org.matsim.signalsystems.model.SignalSystem;
import org.xml.sax.SAXException;

import playground.jbischoff.BAsignals.JbBaPaths;
/**
 * 
 * @author jbischoff
 *
 */
public class CottbusT90Converter {
	
	
	public static void main(String[] args) throws JAXBException, SAXException, ParserConfigurationException, IOException {
		SignalControlData nscd = new SignalControlDataImpl();

		for(int i = 1; i<30; i++){
			SignalSystemControllerData nsscd = nscd.getFactory().createSignalSystemControllerData(Id.create(String.valueOf(i), SignalSystem.class));
			nsscd.setControllerIdentifier("DefaultPlanbasedSignalSystemController");
			SignalPlanData nspd = nscd.getFactory().createSignalPlanData(Id.create("1", SignalPlan.class));
			nspd.setCycleTime(90);
			nspd.setStartTime(0.0);
			nspd.setEndTime(0.0);
			nspd.setOffset(3);
			nsscd.addSignalPlanData(nspd);
			nscd.addSignalSystemControllerData(nsscd);
		}
		
		SignalControlData scd = new SignalControlDataImpl();
		SignalControlReader20 reader = new SignalControlReader20(scd, MatsimSignalSystemsReader.SIGNALCONTROL20);
		reader.readFile(JbBaPaths.CBH+"originaldaten/signalControlCottbusT90_v2.0.xml");
		SignalSystemControllerData oldsscd = scd.getSignalSystemControllerDataBySystemId().get(Id.create("1", SignalSystem.class));
		SignalPlanData spd = oldsscd.getSignalPlanData().get(Id.create("1", SignalPlan.class));
		for (SignalGroupSettingsData sgsd : spd.getSignalGroupSettingsDataByGroupId().values()){
			Id ssid = CottbusT90Converter.returnProperSSID(sgsd.getSignalGroupId());
			nscd.getSignalSystemControllerDataBySystemId().get(ssid).getSignalPlanData().get(Id.create("1", SignalPlan.class)).addSignalGroupSettings(sgsd);
			
		}
		SignalControlWriter20 writer = new SignalControlWriter20(nscd);
		writer.write(JbBaPaths.CBH+"cottbus_20_jb/signalControlCottbusT90_v2.0_jb.xml");
		
		
	}
	
	public static Id<SignalSystem> returnProperSSID(Id refid){
		char[] refbeg ;
		refbeg = new char[2];
		refid.toString().getChars(0, 2, refbeg, 0);
		String refbegst = String.valueOf(refbeg);
		switch (Integer.parseInt(refbegst))  {
		case 11 : return Id.create("14", SignalSystem.class);
		case 12 : return Id.create("16", SignalSystem.class);
		case 13 : return Id.create("15", SignalSystem.class);
		case 14 : return Id.create("12", SignalSystem.class); 
		case 15 : return Id.create( "6", SignalSystem.class); 
		case 16 : return Id.create( "8", SignalSystem.class);
		case 17 : return Id.create( "2", SignalSystem.class);
		case 18 : return Id.create( "5", SignalSystem.class);
		case 19 : return Id.create( "1", SignalSystem.class);
		case 20 : return Id.create("18", SignalSystem.class);
		case 21 : return Id.create("17", SignalSystem.class);
		case 22 : return Id.create("19", SignalSystem.class);
		case 23 : return Id.create("20", SignalSystem.class);
		case 24 : return Id.create("21", SignalSystem.class);
		case 25 : return Id.create("22", SignalSystem.class);
		case 26 : return Id.create("23", SignalSystem.class);
		case 27 : return Id.create("24", SignalSystem.class);
		case 28 : return Id.create("25", SignalSystem.class); 
		case 29 : return Id.create("26", SignalSystem.class);
		case 30 : return Id.create("27", SignalSystem.class); 
		case 31 : return Id.create("29", SignalSystem.class);
		case 32 : return Id.create("28", SignalSystem.class);
		case 33 : return Id.create( "7", SignalSystem.class);
		case 34 : return Id.create( "9", SignalSystem.class);
		case 35 : return Id.create( "3", SignalSystem.class);
		case 36 : return Id.create( "4", SignalSystem.class);
		case 37 : return Id.create("13", SignalSystem.class);
		case 38 : return Id.create("11", SignalSystem.class);
		case 39 : return Id.create("10", SignalSystem.class); 
		}
	System.err.println(refid + "- could not find proper ID for lane, will use 9999");
	return Id.create("9999", SignalSystem.class);
	}

		
		
	
	
	
}
