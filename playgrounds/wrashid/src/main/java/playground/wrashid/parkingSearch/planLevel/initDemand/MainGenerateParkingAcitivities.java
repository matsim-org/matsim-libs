package playground.wrashid.parkingSearch.planLevel.initDemand;


/*
 * just reads an input plans file and produces an output on the console of activity chains and their occurance in percentage.
 */

public class MainGenerateParkingAcitivities{

	public static void main(String[] args) {
		String inputPlansFilePath = "C:\\data\\workspace\\playgrounds\\wrashid\\test\\scenarios\\berlin\\plans_hwh_1pct.xml.gz";
		String outputPlansFilePath = "C:\\data\\workspace\\playgrounds\\wrashid\\test\\scenarios\\berlin\\plansWithParkingActs.xml.gz";
		String networkFilePath = "C:\\data\\workspace\\playgrounds\\wrashid\\test\\scenarios\\berlin\\network.xml.gz";
		String facilitiesFilePath = "C:\\data\\workspace\\playgrounds\\wrashid\\test\\scenarios\\berlin\\facilities.xml";
		
		PlanGeneratorWithParkingActivities pghc=new PlanGeneratorWithParkingActivities(inputPlansFilePath, networkFilePath, facilitiesFilePath);
		
		pghc.processPlans();

		pghc.writePlans(outputPlansFilePath);
		
	}

	

	
}
