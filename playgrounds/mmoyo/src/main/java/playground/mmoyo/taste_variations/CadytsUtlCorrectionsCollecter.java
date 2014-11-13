/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mmoyo.taste_variations;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import playground.mmoyo.io.TextFileWriter;

import java.util.Map.Entry;


/**
  * Calculates and stores svd values from plans at the end of a iteration
  */
public class CadytsUtlCorrectionsCollecter implements IterationEndsListener{
	static final String TB = "\t";
	static final String NL = "\n";

	final String STR_UTLCORR0 = "UtlCorr0";
	final String STR_UTLCORR1 = "UtlCorr1";
	final String STR_UTLCORR2 = "UtlCorr2";
	final String STR_UTLCORR3 = "UtlCorr3";
	final String STR_SEL_INX = "SEL_INX";
	final String STR_CAD_CORR = "cadytsCorrection";
	
	final String STR_wWALK = "wWalk" ;
	final String STR_wTIME = "wTime" ;
	final String STR_wDISTA = "wDista" ;
	final String STR_wCHNG = "wChng" ;
	
	final Network net; 
	final TransitSchedule schedule;
	
	public CadytsUtlCorrectionsCollecter(final Network net, final TransitSchedule schedule){
		this.net = net; 
		this.schedule = schedule;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
	
		if (   ((event.getIteration() > 0) && (event.getIteration() % 100 == 0) ) || (event.getIteration() ==10)) {
			System.err.println("calculating svd values.........");

			ObjectAttributes attrs = new ObjectAttributes();
            Population pop = event.getControler().getScenario().getPopulation();
			
			String STR_UTLCORR = "UtlCorr";
			
			MyLeastSquareSolutionCalculator svd;
			int numPlansMax=0;
			for (Person person : pop.getPersons().values()) {
				String strId = person.getId().toString();
				int numPlans = person.getPlans().size();
				if (numPlans>numPlansMax){numPlansMax = numPlans;}
				
				double[] utCorrec = new double [numPlans]; 
				int i = 0;
				for (Plan plan : person.getPlans()) {
					Double cadytsCorrection = Double.valueOf(0.0);   //initialize with value zero 
					if (plan.getCustomAttributes() != null) {
						cadytsCorrection = (Double)plan.getCustomAttributes().get(STR_CAD_CORR);
						cadytsCorrection = Double.valueOf(cadytsCorrection == null ? 0.0 : cadytsCorrection.doubleValue());  //convert null to zero
					}
					utCorrec[i] = cadytsCorrection.doubleValue();
					attrs.putAttribute(strId , (STR_UTLCORR + i) , cadytsCorrection);
					i++;
				}

				//invoke svd calculator and put resulting values in attrs
				svd = new MyLeastSquareSolutionCalculator(net , schedule, MyLeastSquareSolutionCalculator.SVD );
				IndividualPreferences values = svd.getSVDvalues(person, utCorrec);
				attrs.putAttribute( strId , STR_SEL_INX, Integer.valueOf(person.getPlans().indexOf(person.getSelectedPlan())));
				attrs.putAttribute( strId , STR_wWALK , Double.valueOf(values.getWeight_trWalkTime()));
				attrs.putAttribute( strId , STR_wTIME, Double.valueOf(values.getWeight_trTime()));
				attrs.putAttribute( strId , STR_wDISTA, Double.valueOf(values.getWeight_trDistance()));
				attrs.putAttribute( strId , STR_wCHNG, Double.valueOf(values.getWeight_changes()));
			}

			OutputDirectoryHierarchy outDirhchy = event.getControler().getControlerIO();
			String outfile = "cadytsCorrectionsNsvdValues.xml.gz";
			new ObjectAttributesXmlWriter(attrs).writeFile(outDirhchy.getIterationFilename(event.getIteration(), outfile));
			outfile = null;
			
			// create header for text file
			StringBuffer sBuff = new StringBuffer("ID\tSEL_INX");			
			String strCorr = "\tCORR";
			for(int i=1; i<=numPlansMax; i++){
				sBuff.append(strCorr + i);
			}
			sBuff.append("\twWALK\twTIME\twDIST\twCHNG");
			
			//fill data data
			for(Entry<Id<Person>, ? extends Person> entry: pop.getPersons().entrySet()) {
				String strId = entry.getKey().toString() ;
				Person person = entry.getValue();				
				
				sBuff.append(NL + strId);
				sBuff.append(TB + attrs.getAttribute(strId, STR_SEL_INX));
				for (int i=0; i< person.getPlans().size(); i++){      // instead of //sBuff.append(TB + attrs.getAttribute(strId, STRUtilCorr[0]) + TB + attrs.getAttribute(strId, STRUtilCorr[1]) + TB + attrs.getAttribute(strId, STRUtilCorr[2]) + TB + attrs.getAttribute(strId, STRUtilCorr[3]));
					sBuff.append(TB + attrs.getAttribute(strId, (  STR_UTLCORR + i ) )  );    
				} 
				sBuff.append(TB + attrs.getAttribute(strId, STR_wWALK) + TB + attrs.getAttribute(strId, STR_wTIME) + TB + attrs.getAttribute(strId, STR_wDISTA) + TB + attrs.getAttribute(strId, STR_wCHNG));
			}
			outfile = "cadytsCorrectionsNsvdValues.txt";
			new TextFileWriter().write(sBuff.toString(), outDirhchy.getIterationFilename(event.getIteration(), outfile), false);
			strCorr= null;
			sBuff = null;
			outfile=  null;
			STR_UTLCORR = null;
		}
	}
	

	
}
