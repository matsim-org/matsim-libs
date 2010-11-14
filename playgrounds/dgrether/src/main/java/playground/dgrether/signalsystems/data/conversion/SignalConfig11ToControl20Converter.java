/* *********************************************************************** *
 * project: org.matsim.*
 * SignalConfig11ToControl20Converter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.data.conversion;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.jaxb.amberTimes10.XMLAmberTimes;
import org.matsim.jaxb.amberTimes10.XMLAmberTimes.XMLSignalSystem;
import org.matsim.jaxb.amberTimes10.XMLAmberTimes.XMLSignalSystem.XMLSignal;
import org.matsim.jaxb.amberTimes10.XMLAmberTimesType.XMLAmber;
import org.matsim.jaxb.amberTimes10.XMLAmberTimesType.XMLRedAmber;
import org.matsim.jaxb.signalcontrol20.ObjectFactory;
import org.matsim.jaxb.signalcontrol20.XMLSignalControl;
import org.matsim.jaxb.signalcontrol20.XMLSignalGroupSettingsType;
import org.matsim.jaxb.signalcontrol20.XMLSignalPlanType;
import org.matsim.jaxb.signalcontrol20.XMLSignalSystemControllerType;
import org.matsim.jaxb.signalcontrol20.XMLSignalSystemType;
import org.matsim.jaxb.signalcontrol20.XMLSignalGroupSettingsType.XMLDropping;
import org.matsim.jaxb.signalcontrol20.XMLSignalGroupSettingsType.XMLOnset;
import org.matsim.jaxb.signalcontrol20.XMLSignalPlanType.XMLCycleTime;
import org.matsim.jaxb.signalcontrol20.XMLSignalPlanType.XMLOffset;
import org.matsim.jaxb.signalcontrol20.XMLSignalPlanType.XMLStart;
import org.matsim.jaxb.signalcontrol20.XMLSignalPlanType.XMLStop;
import org.matsim.jaxb.signalsystemsconfig11.XMLAdaptivePlanbasedSignalSystemControlInfoType;
import org.matsim.jaxb.signalsystemsconfig11.XMLAdaptiveSignalSystemControlInfoType;
import org.matsim.jaxb.signalsystemsconfig11.XMLPlanbasedSignalSystemControlInfoType;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemConfig;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemConfigurationType;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemControlInfoType;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemPlanType;
import org.matsim.signalsystems.MatsimSignalSystemConfigurationsReader;
import org.matsim.signalsystems.SignalSystemConfigurationsReader11;
import org.matsim.signalsystems.data.ambertimes.v10.AmberTimesWriter10;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.signalsystems.model.DefaultPlanbasedSignalSystemController;
import org.xml.sax.SAXException;


/**
 * @author dgrether
 *
 */
public class SignalConfig11ToControl20Converter {

	private static final Logger log = Logger.getLogger(SignalConfig11ToControl20Converter.class);
	
	public void convert(String signalConfigs11, String signalControl20,
			String amberTimes10) {
		log.info("Starting to convert " + signalConfigs11  + " to " + signalControl20 + " and " + amberTimes10 + " file...");
		XMLSignalSystemConfig xmlSignalConfigs11 = this.readSignalConfigs11(signalConfigs11);
		Tuple<XMLSignalControl, XMLAmberTimes> signalControl20AmberTimes10 = this.createSignalControl20AndAmberTimes10(xmlSignalConfigs11);
		this.writeSignalControl20(signalControl20AmberTimes10.getFirst(), signalControl20);
		this.writeAmberTimes10(signalControl20AmberTimes10.getSecond(), amberTimes10);
		log.info("conversion done!");
	}

	
	private void writeAmberTimes10(XMLAmberTimes amberTimes10, String filename) {
		AmberTimesWriter10 writer = new AmberTimesWriter10(null);
		writer.write(filename, amberTimes10);
	}


	private void writeSignalControl20(XMLSignalControl xmlSignalControl, String filename) {
		SignalControlWriter20 writer = new SignalControlWriter20(null);
		writer.write(filename, xmlSignalControl);
	}

	private XMLSignalSystemConfig readSignalConfigs11(String signalConfigs11) {
		SignalSystemConfigurationsReader11 reader = new SignalSystemConfigurationsReader11(null, MatsimSignalSystemConfigurationsReader.SIGNALSYSTEMSCONFIG11);
		XMLSignalSystemConfig xmlSignalControl = null;
		try {
			xmlSignalControl = reader.readSignalSystemConfig11File(signalConfigs11);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return xmlSignalControl;
	}

	private Tuple<XMLSignalControl, XMLAmberTimes> createSignalControl20AndAmberTimes10(
			XMLSignalSystemConfig signalConfigs11) {
		ObjectFactory signalControl20Fac = new ObjectFactory();
		XMLSignalControl signalControl20 = signalControl20Fac.createXMLSignalControl();
		
		org.matsim.jaxb.amberTimes10.ObjectFactory amber10Fac = new org.matsim.jaxb.amberTimes10.ObjectFactory();
		XMLAmberTimes amberTimes10 = amber10Fac.createXMLAmberTimes();
		
		for (XMLSignalSystemConfigurationType signalConfig11 : signalConfigs11.getSignalSystemConfiguration()){
			XMLSignalSystemType signalSystem20 = signalControl20Fac.createXMLSignalSystemType();
			signalControl20.getSignalSystem().add(signalSystem20);
			signalSystem20.setRefId(signalConfig11.getRefId());
			
			XMLSignalSystemControlInfoType controlInfo11 = signalConfig11.getSignalSystemControlInfo();
			XMLSignalSystemControllerType signalControler20 = signalControl20Fac.createXMLSignalSystemControllerType();
			signalSystem20.setSignalSystemController(signalControler20);

			if (controlInfo11 instanceof XMLPlanbasedSignalSystemControlInfoType){
				XMLPlanbasedSignalSystemControlInfoType plancontrolInfo11 = (XMLPlanbasedSignalSystemControlInfoType) controlInfo11;
				signalControler20.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
				this.process11Plans(plancontrolInfo11.getSignalSystemPlan(), signalControl20Fac, signalControler20, amber10Fac, amberTimes10, signalSystem20);
			}
			else if (controlInfo11 instanceof XMLAdaptivePlanbasedSignalSystemControlInfoType){
				XMLAdaptivePlanbasedSignalSystemControlInfoType aplanControlInfo11 = (XMLAdaptivePlanbasedSignalSystemControlInfoType) controlInfo11;
				signalControler20.setControllerIdentifier(aplanControlInfo11.getAdaptiveControler());
				this.process11Plans(aplanControlInfo11.getSignalSystemPlan(), signalControl20Fac, signalControler20, amber10Fac, amberTimes10, signalSystem20);
			}
			else if (controlInfo11 instanceof XMLAdaptiveSignalSystemControlInfoType){
				XMLAdaptiveSignalSystemControlInfoType adaptivecontrolInfo11 = (XMLAdaptiveSignalSystemControlInfoType) controlInfo11;
				signalControler20.setControllerIdentifier(adaptivecontrolInfo11.getAdaptiveControler());
			}
		}
		return new Tuple<XMLSignalControl, XMLAmberTimes>(signalControl20, amberTimes10);
	}
	
	private XMLSignalSystem searchAmberSignalSystem(List<XMLSignalSystem> amberSystems, String id){
		for (XMLSignalSystem ss : amberSystems){
			if (ss.getRefId().equals(id)){
				return ss;
			}
		}
		return null;
	}
	
	private void process11Plans(List<XMLSignalSystemPlanType> plans, ObjectFactory signalControl20Fac, 
			XMLSignalSystemControllerType signalControler20, org.matsim.jaxb.amberTimes10.ObjectFactory amber10Fac, 
			XMLAmberTimes amberTimes10, XMLSignalSystemType signalSystem20){
		for (XMLSignalSystemPlanType plan11 : plans){
			Tuple<XMLSignalPlanType, List<XMLSignal>> planAmber20 = this.convertPlan11ToPlan20(plan11, signalControl20Fac, amber10Fac);
			XMLSignalPlanType signalPlan20 = planAmber20.getFirst();
			signalControler20.getSignalPlan().add(signalPlan20);
			if (!planAmber20.getSecond().isEmpty()){
				XMLSignalSystem amberSignalSystem = this.searchAmberSignalSystem(amberTimes10.getSignalSystem(), signalSystem20.getRefId());
				if (amberSignalSystem ==  null){
					amberSignalSystem = amber10Fac.createXMLAmberTimesXMLSignalSystem();
					amberTimes10.getSignalSystem().add(amberSignalSystem);
					amberSignalSystem.setRefId(signalSystem20.getRefId());
				}
				amberSignalSystem.getSignal().addAll(planAmber20.getSecond());
			}
		}
	}
	
	private Tuple<XMLSignalPlanType, List<XMLSignal>> convertPlan11ToPlan20(XMLSignalSystemPlanType plan11, ObjectFactory fac20, 
			org.matsim.jaxb.amberTimes10.ObjectFactory amber10Fac){
		XMLSignalPlanType plan20 = fac20.createXMLSignalPlanType();
		plan20.setId(plan11.getId());
		XMLStart xmlStart = fac20.createXMLSignalPlanTypeXMLStart();
		plan20.setStart(xmlStart);
		xmlStart.setDaytime(plan11.getStart().getDaytime());
		XMLStop xmlStop = fac20.createXMLSignalPlanTypeXMLStop();
		plan20.setStop(xmlStop);
		xmlStop.setDaytime(plan11.getStop().getDaytime());
		XMLCycleTime xmlCycleTime = fac20.createXMLSignalPlanTypeXMLCycleTime();
		plan20.setCycleTime(xmlCycleTime);
		xmlCycleTime.setSec(plan11.getCycleTime().getSec());
		XMLOffset xmlOffset = fac20.createXMLSignalPlanTypeXMLOffset();
		plan20.setOffset(xmlOffset);
		xmlOffset.setSec(plan11.getSynchronizationOffset().getSec());
		
		List<XMLSignal> amberSignals = new ArrayList<XMLSignal>();
		//process signalGroupSettings
		for (org.matsim.jaxb.signalsystemsconfig11.XMLSignalGroupSettingsType settings11 : plan11.getSignalGroupSettings()){
			XMLSignalGroupSettingsType xmlSgSettings = fac20.createXMLSignalGroupSettingsType();
			plan20.getSignalGroupSettings().add(xmlSgSettings);
			xmlSgSettings.setRefId(settings11.getRefId());
			
			XMLOnset xmlOnset = fac20.createXMLSignalGroupSettingsTypeXMLOnset();
			xmlSgSettings.setOnset(xmlOnset); 
			xmlOnset.setSec(settings11.getRoughcast().getSec());
			
			XMLDropping xmlDropping = fac20.createXMLSignalGroupSettingsTypeXMLDropping();
			xmlSgSettings.setDropping(xmlDropping);
			xmlDropping.setSec(settings11.getDropping().getSec());
			
			//create the amberTimes 
			XMLSignal amberSignal = null;
			if (settings11.getInterGreenTimeRoughcast() != null){
				amberSignal = amber10Fac.createXMLAmberTimesXMLSignalSystemXMLSignal();
				amberSignal.setRefId(settings11.getRefId());
				XMLRedAmber redAmber = amber10Fac.createXMLAmberTimesTypeXMLRedAmber();
				amberSignal.setRedAmber(redAmber);
				BigInteger sec = BigInteger.valueOf(settings11.getInterGreenTimeRoughcast().getSec());
				redAmber.setSeconds(sec);
			}
			if (settings11.getInterGreenTimeDropping() != null){
				if (amberSignal == null){
					amberSignal = amber10Fac.createXMLAmberTimesXMLSignalSystemXMLSignal();
					amberSignal.setRefId(settings11.getRefId());
				}
				XMLAmber amber = amber10Fac.createXMLAmberTimesTypeXMLAmber();
				amberSignal.setAmber(amber);
				BigInteger sec = BigInteger.valueOf(settings11.getInterGreenTimeDropping().getSec());
				amber.setSeconds(sec);
			}
			if (amberSignal != null){
				amberSignals.add(amberSignal);
			}
		}
		return new Tuple<XMLSignalPlanType, List<XMLSignal>>(plan20, amberSignals);
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String base = "./test/input/org/matsim/";
		//one agent test
		String inputDir = base + "signalsystems/SignalSystemsOneAgentTest/";
		String signalSystemConfigurations = inputDir + "testSignalSystemConfigurations_v1.1.xml";
		String signalControl20 = inputDir + "testSignalControl_v2.0.xml";
		String amberTimes10 = inputDir + "testAmberTimes_v1.0.xml";
		new SignalConfig11ToControl20Converter().convert(signalSystemConfigurations, signalControl20, amberTimes10);

		//travel time one way test
		inputDir = base + "signalsystems/TravelTimeOneWayTest/";
		signalSystemConfigurations = inputDir + "testSignalSystemConfigurations_v1.1.xml";
		signalControl20 = inputDir + "testSignalControl_v2.0.xml";
		amberTimes10 = inputDir + "testAmberTimes_v1.0.xml";
		new SignalConfig11ToControl20Converter().convert(signalSystemConfigurations, signalControl20, amberTimes10);

		//travel time four ways test
		inputDir = base + "signalsystems/TravelTimeFourWaysTest/";
		signalSystemConfigurations = inputDir + "testSignalSystemConfigurations_v1.1.xml";
		signalControl20 = inputDir + "testSignalControl_v2.0.xml";
		amberTimes10 = inputDir + "testAmberTimes_v1.0.xml";
		new SignalConfig11ToControl20Converter().convert(signalSystemConfigurations, signalControl20, amberTimes10);

		//signalsystems integration test
		inputDir = base + "integration/signalsystems/SignalSystemsIntegrationTest/";
		signalSystemConfigurations = inputDir + "testSignalSystemConfigurations_v1.1.xml";
		signalControl20 = inputDir + "testSignalControl_v2.0.xml";
		amberTimes10 = inputDir + "testAmberTimes_v1.0.xml";
		new SignalConfig11ToControl20Converter().convert(signalSystemConfigurations, signalControl20, amberTimes10);

		
		
	}
}
