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
		reader.readFile(JbBaPaths.CBW+"originaldaten/signalControlCottbusT90_v2.0.xml");
		SignalSystemControllerData oldsscd = scd.getSignalSystemControllerDataBySystemId().get(new IdImpl("1"));
		SignalPlanData spd = oldsscd.getSignalPlanData().get(new IdImpl("1"));
		for (SignalGroupSettingsData sgsd : spd.getSignalGroupSettingsDataByGroupId().values()){
			Id ssid = CottbusT90Converter.returnProperSSID(sgsd.getSignalGroupId());
			nscd.getSignalSystemControllerDataBySystemId().get(ssid).getSignalPlanData().get(new IdImpl("1")).addSignalGroupSettings(sgsd);
			
		}
		
		
	}
	
	public static Id returnProperSSID(Id refid){
		switch (Integer.parseInt(refid.toString()))  {
		case ((1109|1101|1102)) : return new IdImpl("1");
			
		}
		return null;
	}
	
	
}
