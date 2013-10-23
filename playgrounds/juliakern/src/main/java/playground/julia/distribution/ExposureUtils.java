package playground.julia.distribution;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class ExposureUtils {

	public void printTimeTables(List<PersonalExposure> popExposure, String outPathForTimeTables) {
		BufferedWriter buffW;
		try {
			buffW = new BufferedWriter(new FileWriter(outPathForTimeTables));

			// header: Person base case compare case difference
			buffW.write("Timetable");
			buffW.newLine();

			for (PersonalExposure perEx : popExposure) {
				buffW.write("Person id: " + perEx.getPersonalId().toString());
				buffW.newLine();
				buffW.write("Average exposure:" + perEx.getAverageExposure());
				buffW.newLine();
				buffW.write("activity type \t start time \t end time \t duration \t exposure value \t exposure x duration");
				buffW.newLine();

				TimeDependendExposure next = perEx.getNextTimeDependendExposure(null);

				while (next != null) {
					buffW.write(perEx.getStringForInterval(next));
					buffW.newLine();
					next = perEx.getNextTimeDependendExposure(next);
				}

				buffW.newLine();
			}

			buffW.close();

		} catch (IOException e) {
			System.err.println("Error creating " + outPathForTimeTables + ".");
		}
	}


	
	public void printPersonalExposureTimeTables(ArrayList<ExposureEvent> exposure,
			String outPathForTimeTables) {
		// TODO Auto-generated method stub
		
	}

	public void printPersonalResponibility(ArrayList<ResponsibilityEvent> responsibility,
			String outPathForTimeTables) {
		// TODO Auto-generated method stub
		
	}



	public void printExposureInformation(ArrayList<ExposureEvent> exposure,
			String string) {
		// TODO Auto-generated method stub
		
	}



	public void printResponsibilityInformation(
			ArrayList<ResponsibilityEvent> responsibility, String string) {
		// TODO Auto-generated method stub
		
	}



}
