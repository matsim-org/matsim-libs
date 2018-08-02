package org.matsim.contrib.matsim4urbansim.run;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.contrib.matsim4urbansim.constants.InternalConstants;
import org.matsim.core.utils.io.IOUtils;

public class CreateUrbansimPopulation {
	
//	@Test // I don' think this is a test ... since it actually does not "test" anything.  Rather, it is a helper method for
	// other test classes. kai
	static void createUrbanSimTestPopulation(String path, int nPersons){
		
		createPersons(path, nPersons);
		
		createJobs(path, nPersons);
		
		createParcels(path);
		
	}

	private static void createPersons(String path, int nPersons) {
		
		String fileLocation = path + "/" + InternalConstants.URBANSIM_PERSON_DATASET_TABLE + "2000" + InternalConstants.FILE_TYPE_TAB;
		
		BufferedWriter bw = IOUtils.getBufferedWriter(fileLocation);
		
		try {
			
			bw.write("person_id\tparcel_id_home\tparcel_id_work\n");
			
			for(int i=1;i<=nPersons;i++){
				bw.write(i+"\t0\t1\n");
			}
			
			bw.flush();
			bw.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}

	private static void createParcels(String path) {
		String fileLocation = path + "/" + InternalConstants.URBANSIM_PARCEL_DATASET_TABLE + "2000" + InternalConstants.FILE_TYPE_TAB;

		BufferedWriter bw = IOUtils.getBufferedWriter(fileLocation);
		
		try{
			
			bw.write("parcel_id\tx_coord_sp\ty_coord_sp\tzone_id\n");
			
			bw.write("0\t0\t100\t1\n");
			bw.write("1\t200\t100\t1\n");
			
			bw.flush();
			bw.close();
			
		} catch(IOException e){
			e.printStackTrace();
		}
	}

	private static void createJobs(String path, int nPersons) {
		
		String fileLocation = path + "/" + InternalConstants.URBANSIM_JOB_DATASET_TABLE + "2000" + InternalConstants.FILE_TYPE_TAB;

		BufferedWriter bw = IOUtils.getBufferedWriter(fileLocation);
		
		try {
			
			bw.write("job_id\tparcel_id_work\tzone_id_work\n");
			
			for(int i=1;i<=nPersons;i++){
				bw.write(i+"\t1\t1\n");	
			}
			
			
			bw.flush();
			
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
