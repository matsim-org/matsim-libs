package playground.artemc.scenarioTools;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Random;

import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;



public class CarOwnershipAssignment {

	/**
	 * @param args
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoConnectionException 
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException, NoConnectionException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/SiouxFalls_Dempo.properties"));
		ResultSet persons = dba.executeQuery("SELECT * FROM u_artemc.sf_population");
		ResultSet facilities = dba.executeQuery("SELECT * FROM u_artemc.sf_facilities");
		ResultSet homes = dba.executeQuery("SELECT * FROM u_artemc.sf_homes_ext");

		Random generator = new Random();	

		HashMap<String, Household> hh_Map = new HashMap<String, Household>();
		HashMap<String, Double> distances = new HashMap<String, Double>();
		HashMap<String, String> hh_homes = new HashMap<String, String>();
		HashMap<String, Integer> units = new HashMap<String, Integer>();

		Integer totalHouseholds = 0;
		Integer totalPersons = 0;
		Integer totalAdults=0;
		Integer totalChildren=0;;
		Integer totalPensioners=0;
		Integer noCar=0;

		while(facilities.next()){
			distances.put(facilities.getString("id"), facilities.getDouble("transit_distance"));	
		}

		while(homes.next()){
			hh_homes.put(homes.getString("synth_hh_id"), homes.getString("home"));	
			if(!units.containsKey(homes.getString("home")))
				units.put(homes.getString("home"), homes.getInt("units"));
		}


		while(persons.next()){
			String synth_hh_id = persons.getString("synth_hh_id");

			if(!hh_Map.containsKey(synth_hh_id)){
				Household newHousehold = new Household(synth_hh_id);
				Integer income = persons.getInt("HHINCOME");
				String home_facility = hh_homes.get(synth_hh_id);
				Double stopDistance = distances.get(home_facility);

				System.out.println(home_facility+","+stopDistance);

				newHousehold.setIncome(income);
				newHousehold.setStopDistance(stopDistance);
				newHousehold.setUnitsInBuilding(units.get(home_facility));
				hh_Map.put(synth_hh_id, newHousehold);
				totalHouseholds++;

			}

			Integer age = persons.getInt("AGE");
			if(age<18){
				hh_Map.get(synth_hh_id).setChildren(hh_Map.get(synth_hh_id).getChildren()+1);
				totalChildren++;
			}else if(age >=18 && age<65){
				hh_Map.get(synth_hh_id).setAdults(hh_Map.get(synth_hh_id).getAdults()+1);
				totalAdults++;
			}
			else{
				hh_Map.get(synth_hh_id).setPensioners(hh_Map.get(synth_hh_id).getPensioners()+1);
				totalPensioners++;
			}
			totalPersons++;
		}



		/*Car ownership model according to Giuliano, Dargay (2006)*/
		for(String hh:hh_Map.keySet()){

			Boolean oneAdult = false;
			Boolean threeAdults = false;
			Boolean children = false;
			Boolean pensioner = false;

			Boolean income20 = false;
			Boolean income40_55 = false;
			Boolean income55=false;

			Boolean transit = false;
			
			Boolean rowHouse = false;
			Boolean appartment = false;

			if((hh_Map.get(hh).getAdults()+hh_Map.get(hh).getPensioners())==1)
				oneAdult = true;

			if((hh_Map.get(hh).getAdults()+hh_Map.get(hh).getPensioners())>3)
				threeAdults = true;

			if(hh_Map.get(hh).getChildren()>0)
				children = true;

			if(hh_Map.get(hh).getPensioners()>0 && hh_Map.get(hh).getAdults()==0)
				pensioner = true;

			if(hh_Map.get(hh).getIncome()<20000)
				income20 = true;

			if(hh_Map.get(hh).getIncome()>=40000 && hh_Map.get(hh).getIncome()<=55000)
				income40_55=true;

			if(hh_Map.get(hh).getIncome()>55000)
				income55=true;

			if(hh_Map.get(hh).getStopDistance()<805.00)
				transit=true;
			
			if(hh_Map.get(hh).getUnitsInBuilding()>1 && hh_Map.get(hh).getUnitsInBuilding()<7)
				rowHouse = true;
			
			if(hh_Map.get(hh).getUnitsInBuilding()>=7)
				appartment = true;
			

			System.out.println(hh+","+hh_Map.get(hh).getIncome());
			System.out.println(oneAdult+","+threeAdults+"   "+children+","+pensioner+"   "+income20+","+income40_55+","+income55);

			Double cStar = - 1.21 * (oneAdult ? 1.0 : 0.0) 
					+ 0.39 * (threeAdults ? 1.0 : 0.0)
					+ 0.11 * (children ? 1.0 : 0.0)
					- 0.37 * (pensioner ? 1.0 : 0.0)
					- 0.51 * (income20 ? 1.0 : 0.0)
					+ 0.44 * (income40_55 ? 1.0 : 0.0)
					+ 0.76 * (income55 ? 1.0 : 0.0)
					+ 0.17 * 1.0
					- 0.24 * (transit ? 1.0 : 0.0)
					- 0.27 * (rowHouse ? 1.0 : 0.0)
					- 0.57 * (appartment ? 1.0 : 0.0)
					+ 2.69;

			Double random = generator.nextGaussian();
			Double c = cStar + random;

			Integer cars = 0;

			if(c>0 && c<=1.79)
				cars = 1;

			if(c>1.79)
				cars = 2;

			
			dba.executeStatement(String.format("UPDATE %s SET cars = %s WHERE synth_hh_id = '%s';",
					"u_artemc.sf_cars ", cars, hh_Map.get(hh).getHh_number()));	
			
			System.out.println(cStar+" - "+random+" = "+c+"      CARS:"+cars);

			if(cars == 0) 
				noCar++;
		}


		System.out.println("Total persons: "+totalPersons);
		System.out.println("Total households: "+totalHouseholds);
		System.out.println("  Total adults:"+totalAdults);
		System.out.println("  Total children:"+totalChildren);
		System.out.println("  Total pernsioners:"+totalPensioners);
		System.out.println();
		System.out.println("  Average adults:"+(double)totalAdults/(double)totalHouseholds);
		System.out.println("  Average children:"+(double)totalChildren/(double)totalHouseholds);
		System.out.println("  Average pernsioners:"+(double)totalPensioners/(double)totalHouseholds);	
		System.out.println();

		System.out.println("No car households: "+noCar+"   %:"+(double)noCar/(double)totalHouseholds);

		dba.close();
	}

}

