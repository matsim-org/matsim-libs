package playground.jhackney.socialnetworks.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

public class ActivityActReader {
	private String fileName;
	private BufferedReader br = null;
	private int[] iter;
	private String[] pId;
	private String[] fId;
	private String[] acttype;
//	private int[] readActId;
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
			{
//				System.err.println(" Input error. Probably the mental map file of Acts <-> Facilities was not found for this iteration. Check paths and file locations: "+fileName);
				this.log.info(" The mental map file of Acts <-> Facilities was not found for this iteration. If you wanted to use one, check paths and file locations: "+fileName);
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


	public TreeMap<Id, String> getNextPoint(){

		TreeMap<Id,String> newPoint=new TreeMap<Id,String>();
		Id myFacId=null;
		String myType=null;

//		readLine is positioned at the first line that interests us and this line is already read

		try{
			if(!tapeAtStartPosition){
				thisLineOfData=br.readLine();
			}

			int jjj=Integer.valueOf(thisLineOfData.split(patternStr)[2]).intValue();
			myFacId=new IdImpl(jjj);
			myType=thisLineOfData.split(patternStr)[3];
			tapeAtStartPosition=false;

		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		newPoint.put(myFacId, myType);
		return newPoint;
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
	public String[] getActivityType(){
		return acttype;
	}
	public String[] fId(){
		return fId;
	}
}
