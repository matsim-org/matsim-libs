package playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.incomeImputation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.acmarmol.matsim2030.microcensus2010.MZConstants;
import playground.acmarmol.matsim2030.microcensus2010.objectAttributesConverters.CoordConverter;

public class UpdateHouseholdIncome {
	
	private String fromFile;
	private ObjectAttributes householdAttributes;
	
	
	public UpdateHouseholdIncome(String fromFile, String householdAttributesInputFile){
	
		this.fromFile = fromFile;
		this.householdAttributes = new ObjectAttributes();
		ObjectAttributesXmlReader readerHH = new ObjectAttributesXmlReader(householdAttributes);
		readerHH.putAttributeConverter(CoordImpl.class, new CoordConverter());
		readerHH.parse(householdAttributesInputFile);		
	}
	
	
	
	public static void main(String[] args) throws IOException {
		
		String inputBase = "C:/local/marmolea/input/Activity Chains Forecast/";
		
		UpdateHouseholdIncome mz2000 = new UpdateHouseholdIncome(inputBase + "incomeImputationMZ2000.dat", inputBase + "householdAttributes.04.MZ2000.xml" ); 
		UpdateHouseholdIncome mz2005 = new UpdateHouseholdIncome(inputBase + "incomeImputationMZ2005.dat", inputBase + "householdAttributes.04.MZ2005.xml" ); 
		UpdateHouseholdIncome mz2010 = new UpdateHouseholdIncome(inputBase + "incomeImputationMZ2010.dat", inputBase + "householdAttributes.04.MZ2010.xml" ); 
		
		mz2000.update();
		mz2000.write(inputBase + "householdAttributes.04.imputed.MZ2000.xml");
		mz2005.update();
		mz2005.write(inputBase + "householdAttributes.04.imputed.MZ2005.xml");
		mz2010.update();
		mz2010.write(inputBase + "householdAttributes.04.imputed.MZ2010.xml");
		
	}
	
	public void update() throws IOException{
		
		double counter1 =0;
		
		FileReader fr = new FileReader(this.fromFile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
		
		while ((curr_line = br.readLine()) != null) {
			
		String[] entries = curr_line.split("\t", -1);
		
		String hhnr = entries[2];
		String income = entries[36];
		
		if(this.householdAttributes.getAttribute(hhnr, MZConstants.HOUSEHOLD_INCOME_MIDDLE_OF_INTERVAL).equals(MZConstants.UNSPECIFIED)){
			counter1++;
			this.householdAttributes.putAttribute(hhnr, MZConstants.HOUSEHOLD_INCOME_MIDDLE_OF_INTERVAL, income);
		}
//		else{
//			if(!this.householdAttributes.getAttribute(hhnr, "household_income_2").equals(income)){
//				System.out.println(hhnr + "\t" + income + "\t" +  this.householdAttributes.getAttribute(hhnr, "household_income_2"));
//			}
//		}	
		
		}
		
//		System.out.println("Total of Households with UNSPECIFIED INCOME: " + counter1);
	}
	
	
	
	public void write(String outputFile){
		
		ObjectAttributesXmlWriter households_axmlw = new ObjectAttributesXmlWriter(this.householdAttributes);
		households_axmlw.putAttributeConverter(CoordImpl.class, new CoordConverter());
		households_axmlw.writeFile(outputFile);		
	}

}
