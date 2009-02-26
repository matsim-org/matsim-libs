package org.matsim.socialnetworks.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.facilities.Activity;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;

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

		try{
			out = new BufferedWriter(new FileWriter(outFileName));
			out.write("iter id facilityid acttype");
			out.write("\r\n");
//			out.newLine();

		}catch (IOException ex ){
			ex.printStackTrace();
		}

	}
	public void write(int iter, Population myPlans){
//		System.out.println("AAW will write to"+out.toString());
		Iterator<Person> pIt = myPlans.iterator();
		while(pIt.hasNext()){
			Person myPerson = (Person) pIt.next();
			List<Plan> myPersonPlans = myPerson.getPlans();

			for (int i=0;i<myPersonPlans.size();i++){
				Plan myPlan = myPersonPlans.get(i);
				ArrayList<Object> actsLegs=myPlan.getActsLegs();

				for (int j=0;j<actsLegs.size()+1;j=j+2){
					Act myAct= (Act) actsLegs.get(j);
					Activity myActivity=myAct.getFacility().getActivity(myAct.getType());
//					System.out.println(" AAW DEBUG J=: "+j);
					try {
						out.write(iter+" "+myPerson.getId()+" "+myActivity.getFacility().getId()+" "+myActivity.getType());
						out.newLine();

					} catch (IOException e) {
						// TODO Auto-generated catch block
						System.out.println(myActivity.toString());
						System.out.println(myAct.toString());
						e.printStackTrace();
					}
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
