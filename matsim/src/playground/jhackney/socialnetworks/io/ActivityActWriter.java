package playground.jhackney.socialnetworks.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

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
		Iterator<PersonImpl> pIt = myPlans.getPersons().values().iterator();
		while(pIt.hasNext()){
			PersonImpl myPerson = (PersonImpl) pIt.next();
			List<PlanImpl> myPersonPlans = myPerson.getPlans();

			for (int i=0;i<myPersonPlans.size();i++){
				PlanImpl myPlan = myPersonPlans.get(i);
				List<? extends BasicPlanElement> actsLegs=myPlan.getPlanElements();

				for (int j=0;j<actsLegs.size()+1;j=j+2){
					ActivityImpl myAct= (ActivityImpl) actsLegs.get(j);
					ActivityOption myActivity=myAct.getFacility().getActivityOption(myAct.getType());
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
