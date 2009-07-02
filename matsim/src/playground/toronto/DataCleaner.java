//Data Cleaner
//Cleans modechoice output file
//Generate three lists of trip chains: one for pure auto chains, one for pure non-drive tours, and one for anything else
//
//
//Assumptions:
//	Chains are defined as a chain of trips that starts at home (mode H or L) and ends at home (mode H or L)
//	In the case where there are non-drive trips within an otherwise drive chain, a pure drive chain can be obtained by simply removing those non-drive trips

package playground.toronto;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.Math;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.utils.io.IOUtils;

public class DataCleaner {

	/**
	 * @param args
	 */
	public static int i=0;
	public static String output;
	public static int countG = 0;
	public static int countO = 0;
	public static int countB = 0;
	public static int removed = 0;
	public static int chainS =1;
	public static int chainE =1;
	public static int chainB =1; //backup
	private int sum=0;
	private int product=-1;
	private String tmpPersonId = "";
	private String tmpTripId = "";
	private String tmpPurp = "H";
	private String start = "H";
	private String[] tmpTabs = {"","","1"};
	public static Map<String,String> storage = new HashMap<String,String>();
	
	
	public void readLine(final String line) {
		if(line!=null){
			String[] tabs = line.split(",");
			String tripId = tabs[0] + "," + tabs[1] + "," + tabs[2];
			
			if(!this.tmpTripId.equals(tripId)){ //ignore duplicates
				String temp = ","+tabs[3];
				for (int i = 4; i<tabs.length;i=i+1){
					temp = temp+","+tabs[i];
				}
				//System.out.println(temp);
		        storage.put(tripId, temp);
				String personId = tabs[0] + "," + tabs[1];
				int mode = Integer.parseInt(tabs[16]); 
		
				String purp = tabs[7];
				
				try{
					
					if (this.tmpPersonId.equals(personId)&&this.tmpTabs[10].equals(tabs[10])) { //same person, same chain
						if(mode != 1 && mode !=2 && mode != 7){
							sum = sum + Math.abs(mode);
							product = product * Math.abs(mode);
						}//do nothing in the case where there are non-drive trips within an otherwise drive chain 
						i=0;
					}else{
						output = this.tmpPersonId;
						chainS = chainB;
						chainE = Integer.parseInt(tmpTabs[2]);
						
						
						//System.out.print(personId + ",!"+ sum + ","+ product + "\n");
						if(sum==0&&product==0&&(this.tmpPurp.equals("H")||this.tmpPurp.equals("L"))&&(start.equals("H")||start.equals("L"))){//all drive home chain
							i = 1;
							countG++;
						}else if(sum>0&&product==0){//at least one drive trip
							i = 2;
							countO++;
						}else if(product>0||(!this.tmpPurp.equals("H")&&!this.tmpPurp.equals("L"))||(!start.equals("H")&&!start.equals("L"))){//all non-drive
							i = 3;	
							countB++;
						} 
						sum = Math.abs(mode);
						product = Math.abs(mode);
						chainB = Integer.parseInt(tabs[2]);
						start = tabs[4];
						this.tmpPersonId = personId;

					}
					
					this.tmpTripId = tripId;
					this.tmpPurp = purp;
					this.tmpTabs = tabs;
					//System.out.print(tabs[0]+","+ tabs[1] + "," + tabs[2]+ ",\n"+chainS + ","+chainE + ", i="+ i + "\n");
					
				} catch (Exception e) {
				e.printStackTrace();
				}
				//System.out.print(personId + ","+ sum + ","+ i + "\n");
			}
		}else{
			output = this.tmpPersonId;
			chainS = chainB;
			chainE = Integer.parseInt(tmpTabs[2]);
			//System.out.print(personId + ",!"+ sum + ","+ product + "\n");
			if(sum==0&&product==0&&(this.tmpPurp.equals("H")||this.tmpPurp.equals("L"))&&(start.equals("H")||start.equals("L"))){//all drive home chain
				i = 1;
				countG++;
			}else if(sum>0&&product==0){//at least one drive trip
				i = 2;
				countO++;
			}else if(product>0||(!this.tmpPurp.equals("H")&&!this.tmpPurp.equals("L"))||(!start.equals("H")&&!start.equals("L"))){//all non-drive
				i = 3;	
				countB++;
			} 
		}
	}

	public static void main(final String[] args) {
		String InputFilename = "C:\\Thesis_HJY\\matsim\\input\\DataCleaner\\fout_modechoices.csv";
		String GoodOutputFilename = "C:\\Thesis_HJY\\matsim\\output\\Datacleaner\\fout_modechoices_good.txt";
		String OkOutputFilename = "C:\\Thesis_HJY\\matsim\\output\\DataCleaner\\fout_modechoices_salvage.txt";
		String BadOutputFilename = "C:\\Thesis_HJY\\matsim\\output\\DataCleaner\\fout_modechoices_delete.txt";
//		String InputFilename = "C:\\Thesis_HJY\\matsim\\input\\DataCleaner\\InputForRecleaning.txt";
//		String GoodOutputFilename = "C:\\Thesis_HJY\\matsim\\output\\Datacleaner\\fout_modechoices_recleaned.txt";
//		String OkOutputFilename = "C:\\Thesis_HJY\\matsim\\output\\DataCleaner\\fout_modechoices_salvage2.txt";
//		String BadOutputFilename = "C:\\Thesis_HJY\\matsim\\output\\DataCleaner\\fout_modechoices_delete2.txt";
		DataCleaner d = new DataCleaner();
		
		try {
			BufferedReader reader = IOUtils.getBufferedReader(InputFilename);
			BufferedWriter writerG = IOUtils.getBufferedWriter(GoodOutputFilename);
			BufferedWriter writerO = IOUtils.getBufferedWriter(OkOutputFilename);
			BufferedWriter writerB = IOUtils.getBufferedWriter(BadOutputFilename);

			String line = reader.readLine();
			do {
				line = reader.readLine();
				if (line != null) {
					d.readLine(line);
					if(i==1){
						for (int i = chainS; i < chainE+1; i = i + 1){
							writerG.write(output+","+i+storage.get(output+","+i));
							writerG.newLine();
						}
					}else if (i==2){
						for (int i = chainS; i < chainE+1; i = i + 1){
							writerO.write(output+","+i+storage.get(output+","+i));
							writerO.newLine();
						}
					}else if (i==3){
						for (int i = chainS; i < chainE+1; i = i + 1){
							writerB.write(output+","+i+storage.get(output+","+i));
							writerB.newLine();
						}
					}
				}
				i=0; //reset i 
			} while (line != null);
			d.readLine(line);
			if(i==1){
				for (int i = chainS; i < chainE+1; i = i + 1){
					writerG.write(output+","+i+storage.get(output+","+i));
					writerG.newLine();
				}
			}else if (i==2){
				for (int i = chainS; i < chainE+1; i = i + 1){
					writerO.write(output+","+i+storage.get(output+","+i));
					writerO.newLine();
				}
			}else if (i==3){
				for (int i = chainS; i < chainE+1; i = i + 1){
					writerB.write(output+","+i+storage.get(output+","+i));
					writerB.newLine();
				}
			}
			
			reader.close();
			writerG.close();
			writerO.close();
			writerB.close();
			System.out.print(countG + " chain(s) written to " + GoodOutputFilename +".\n");
			System.out.print(countO + " chain(s) written to " + OkOutputFilename +".\n");
			System.out.print(countB + " chain(s) written to " + BadOutputFilename +".\n");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("done.");
	}
}
