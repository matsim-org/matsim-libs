package playground.balmermi.census2000v2.models;


public class ModelTest {
	
	public static void testMobilityTools() {
		ModelMobilityTools model = new ModelMobilityTools();

		for (int i=1; i<101; i++) {
			for (int j=0; j<101; j++) {
				model.setAge(21);
				model.setSex(false);
				model.setNationality(true);
				model.setHHDimension(i);
				model.setHHKids(j);
				model.setIncome(5.8);
				model.setUrbanDegree(1);
				model.setLicenseOwnership(false);
				model.setDistanceHome2Work(1113.1);
				model.setFuelCost(1.48);
				model.setLanguage(2);

				// calc and assign mobility tools
				int mobtype = model.calcMobilityTools();
				System.out.println("("+i+","+j+":"+mobtype+")");
			}
		}
	}

	public static void main(String[] args) {
		testMobilityTools();
	}

}
