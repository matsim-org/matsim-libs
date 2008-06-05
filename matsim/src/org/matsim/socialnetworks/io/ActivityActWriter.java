package org.matsim.socialnetworks.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.matsim.facilities.Activity;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Knowledge;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;

/**
 * Writes the correspondence table between the facilities and the acts in the plans
 * for all plans of all agents and all iterations.
 * 
 * @author jhackney
 *
 */

public class ActivityActWriter {

	BufferedWriter out;

	public ActivityActWriter(){

	}
	public void openFile(String outFileName){
		//open file here

//		File outDir = new File(outDirName);
//		if (!outDir.mkdir() && !outDir.exists()) {
//		Gbl.errorMsg("Cannot make directory " + outDirName);
//		}
//		File outFile = new File(outFileName);
//		if (!outFile.mkdir() && !outFile.exists()) {
//		Gbl.errorMsg("Cannot make directory " + outFileName);
//		}

//		String outFileName = outDirName + "ActivityActMap.txt";
		try{
			out = new BufferedWriter(new FileWriter(outFileName));
			out.write("iter id facilityid actid");
			out.newLine();

		}catch (IOException ex ){
			ex.printStackTrace();
		}

	}
	public void write(int iter, Plans myPlans){
//		System.out.println("AAW will write to"+out.toString());
		Iterator<Person> pIt = myPlans.iterator();
		while(pIt.hasNext()){
			Person myPerson = (Person) pIt.next();
			Knowledge myKnowledge = myPerson.getKnowledge();
			List<Plan> myPersonPlans = myPerson.getPlans();

			for (int i=0;i<myPersonPlans.size();i++){
				Plan myPlan = myPersonPlans.get(i);
				ArrayList<Object> actsLegs=myPlan.getActsLegs();
				int actIndex=0;
				for (int j=0;j<actsLegs.size()+1;j=j+2){
					Act myAct= (Act) actsLegs.get(j);
					Activity myActivity= myKnowledge.map.getActivity(myAct);
//					System.out.println(" AAW DEBUG J=: "+j);
					try {
						out.write(iter+" "+myPerson.getId()+" "+myActivity.getFacility().getId()+" "+actIndex);
						out.newLine();

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					actIndex++;
				}
			}
		}
		// out.write(iter+" "+myPerson.getId()+" "+myActivity.getFacility().getRefId()+" "+myAct.getId());
	}

	public void close(){
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
