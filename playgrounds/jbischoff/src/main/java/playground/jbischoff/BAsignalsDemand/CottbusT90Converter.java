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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.signalsystems.MatsimSignalSystemsReader;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlDataImpl;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlReader20;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
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
			SignalSystemControllerData nsscd = nscd.getFactory().createSignalSystemControllerData(new IdImpl(String.valueOf(i)));
			nsscd.setControllerIdentifier("DefaultPlanbasedSignalSystemController");
			SignalPlanData nspd = nscd.getFactory().createSignalPlanData(new IdImpl("1"));
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
		SignalSystemControllerData oldsscd = scd.getSignalSystemControllerDataBySystemId().get(new IdImpl("1"));
		SignalPlanData spd = oldsscd.getSignalPlanData().get(new IdImpl("1"));
		for (SignalGroupSettingsData sgsd : spd.getSignalGroupSettingsDataByGroupId().values()){
			Id ssid = CottbusT90Converter.returnProperSSID(sgsd.getSignalGroupId());
			nscd.getSignalSystemControllerDataBySystemId().get(ssid).getSignalPlanData().get(new IdImpl("1")).addSignalGroupSettings(sgsd);
			
		}
		SignalControlWriter20 writer = new SignalControlWriter20(nscd);
		writer.write(JbBaPaths.CBH+"cottbus_20_jb/signalControlCottbusT90_v2.0_jb.xml");
		
		
	}
	
	public static Id returnProperSSID(Id refid){
		char[] refbeg ;
		refbeg = new char[2];
		refid.toString().getChars(0, 2, refbeg, 0);
		String refbegst = String.valueOf(refbeg);
		switch (Integer.parseInt(refbegst))  {
		case 11 : return new IdImpl("14");
		case 12 : return new IdImpl("16");
		case 13 : return new IdImpl("15");
		case 14 : return new IdImpl("12"); 
		case 15 : return new IdImpl("6"); 
		case 16 : return new IdImpl("8");
		case 17 : return new IdImpl("2");
		case 18 : return new IdImpl("5");
		case 19 : return new IdImpl("1");
		case 20 : return new IdImpl("18");
		case 21 : return new IdImpl("17");
		case 22 : return new IdImpl("19");
		case 23 : return new IdImpl("20");
		case 24 : return new IdImpl("21");
		case 25 : return new IdImpl("22");
		case 26 : return new IdImpl("23");
		case 27 : return new IdImpl("24");
		case 28 : return new IdImpl("25"); 
		case 29 : return new IdImpl("26");
		case 30 : return new IdImpl("27"); 
		case 31 : return new IdImpl("29");
		case 32 : return new IdImpl("28");
		case 33 : return new IdImpl("7");
		case 34 : return new IdImpl("9");
		case 35 : return new IdImpl("3");
		case 36 : return new IdImpl("4");
		case 37 : return new IdImpl("13");
		case 38 : return new IdImpl("11");
		case 39 : return new IdImpl("10"); 
		}
	System.err.println(refid + "- could not find proper ID for lane, will use 9999");
	return new IdImpl("9999");
	}

		
		
	
	
	
}
