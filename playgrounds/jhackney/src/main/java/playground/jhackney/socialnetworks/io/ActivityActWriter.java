package playground.jhackney.socialnetworks.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.population.ActivityImpl;

/**
 * Writes the correspondence table between the facilities and the acts in the plans
 * for all plans of all agents and all iterations.
 *
 * @author jhackney
 *
 */

public class ActivityActWriter {

	BufferedWriter out;
	ActivityFacilities facilities;

	public ActivityActWriter(ActivityFacilities facilities){
		this.facilities = facilities;
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
	public void write(int iter, Population myPlans) {
//		System.out.println("AAW will write to"+out.toString());
		Iterator<? extends Person> pIt = myPlans.getPersons().values().iterator();
		while(pIt.hasNext()){
			Person myPerson = pIt.next();
			List<? extends Plan> myPersonPlans = myPerson.getPlans();

			for (int i=0;i<myPersonPlans.size();i++){
				Plan myPlan = myPersonPlans.get(i);
				List<? extends PlanElement> actsLegs=myPlan.getPlanElements();

				for (int j=0;j<actsLegs.size()+1;j=j+2){
					ActivityImpl myAct= (ActivityImpl) actsLegs.get(j);
					ActivityOption myActivity=this.facilities.getFacilities().get(myAct.getFacilityId()).getActivityOptions().get(myAct.getType());
//					System.out.println(" AAW DEBUG J=: "+j);
					try {
						out.write(iter+" "+myPerson.getId()+" "+myAct.getFacilityId()+" "+myActivity.getType());
						out.newLine();

					} catch (IOException e) {
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
