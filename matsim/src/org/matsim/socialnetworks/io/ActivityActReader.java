package org.matsim.socialnetworks.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.socialnetworks.mentalmap.MentalMap;

public class ActivityActReader {
	private String fileName;
	private BufferedReader br = null;
	private int[] iter;
	private String[] pId;
	private int[] activityId;
	private int[] readActId;
	private int iteration=-1;
	private boolean tapeAtStartPosition=false;
	private String thisLineOfData=null;
	private String patternStr = " ";
	private Logger log = Logger.getLogger(ActivityActReader.class);

	public ActivityActReader(int iteration){
		this.iteration = iteration;
	}

	public void openFile(String fileName){

		if(fileName!=null){

			this.log.info("Opening "+fileName);
			
			try // If an error occurs, go to the "catch" block
			{   // FileInputStream fis = new FileInputStream (fileName);
				FileReader fis = new FileReader (fileName);
				br = new BufferedReader (fis);
			}
			// Handle errors in opening the file
			catch (Exception e)
			{   System.err.println(" File not found or other input error: "+fileName);
			}
		}

		int thisIter=-1;
		//		Position the Tape ;^)

		try {
			thisLineOfData=br.readLine();//		iter pid facilityid actid
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		while(iteration!=thisIter){
			try {
				thisLineOfData=br.readLine();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
//			System.out.println(thisLineOfData);

			thisIter=Integer.valueOf(thisLineOfData.split(patternStr)[0]).intValue();
		}
		tapeAtStartPosition=true;
	}

	public void read(){

		// Continue to read lines while there are still some left to read

		int i=0; // counter for the number of lines
		try {
			while ((thisLineOfData=br.readLine()) != null)
			{
//				iter pid facilityid actid
				String[] s;

				s = thisLineOfData.split(patternStr);

				iter[i] = Integer.valueOf(s[0]).intValue();
				pId[i] = s[1];
				activityId[i] =Integer.valueOf(s[2]).intValue();
				readActId[i] =Integer.valueOf(s[3]).intValue();
				i++;
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Id getNextActivityId(){

		Id myId=null;

//		readLine is positioned at the first line that interests us and this line is already read

		try{
			if(!tapeAtStartPosition){
				thisLineOfData=br.readLine();
			}

			int jjj=Integer.valueOf(thisLineOfData.split(patternStr)[2]).intValue();
			myId=new IdImpl(jjj);
			tapeAtStartPosition=false;

		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return myId;
	}

	public void close(){
		try {
			System.out.println(" Closing "+ fileName);
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public int[] getIter(){
		return iter;
	}
	public String[] getpId(){
		return pId;
	}
	public int[] getActivityId(){
		return activityId;
	}
	public int[] actId(){
		return readActId;
	}
}
