package playground.florian.GTFSConverter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class GtfsSource {
	
	private String filename;
	
	private List<String> header;
	
	private List<String[]> content;
	

	static GtfsSource parseGtfsFile(String filename){
		GtfsSource newFile = new GtfsSource();
		newFile.filename = filename;
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			newFile.header = new ArrayList<String>(Arrays.asList(splitRow(br.readLine(),true)));
			newFile.content = new ArrayList<String[]>();
			String row = br.readLine();
			while(row!= null){
				newFile.content.add(splitRow(row,false));
				row = br.readLine();
			};
		} catch (FileNotFoundException e) {
			System.out.println(filename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}
		int pI = -1;
		int sI = -1;
		// in stopTimes und shapes the contents need to be in the right order - that's why they will be sorted now
		for(String s: newFile.header){		
			if((s.contains("trip_id")) || (s.contains("shape_id"))){
				pI = newFile.header.indexOf(s);			
			}else if(s.contains("sequence")){
				sI = newFile.header.indexOf(s);
			}
			if((pI >= 0) && (sI >= 0)){
				newFile.sort(pI, sI);
				break;
			}
		}
		return newFile;
	}

	static String[] splitRow(String row, boolean header){
		List<String> entries = new ArrayList<String>();
		boolean quotes = false;
		StringBuilder sb = new StringBuilder();
		for(int i =0; i<row.length(); i++){
			if(row.charAt(i) == '"'){
				quotes = !(quotes);
			}else if((row.charAt(i) == ',') && !(quotes)){
				entries.add(sb.toString().trim());	
				sb = new StringBuilder();
			}else{
				if(sb.length() == 0){
					if(Character.isLetterOrDigit(row.charAt(i))){
						sb.append(row.charAt(i));
					}else if(!header){
						sb.append(row.charAt(i));
					}
				}else if(Character.isDefined(row.charAt(i))){
					sb.append(row.charAt(i));
				}				
			}
		}
		entries.add(sb.toString().trim());	
		return entries.toArray(new String[entries.size()]);
	}
	
	int getContentIndex(String headerInformation){
		return header.indexOf(headerInformation);
	}
	
	List<String[]> getContent() {
		return content;
	}

	String getFilename() {
		return filename;
	}
	
	void sort(int primarySortIndex, int secondarySortIndex){
		Collections.sort(this.content, new GtfsComparator(primarySortIndex, secondarySortIndex));
	}
	
	private class GtfsComparator implements Comparator<String[]>{
		
		int primarySortIndex;
		int secondarySortIndex;
		
		public GtfsComparator(int primarySortIndex, int secondarySortIndex){
			this.primarySortIndex = primarySortIndex;
			this.secondarySortIndex = secondarySortIndex;
		}

		@Override
		public int compare(String[] o1, String[] o2) {
			String s1 = o1[primarySortIndex];
			String s2 = o2[primarySortIndex];
			int result = s1.compareTo(s2);
			if(result == 0){
				Double d1 = Double.parseDouble(o1[secondarySortIndex]);
				Double d2 = Double.parseDouble(o2[secondarySortIndex]);
				return d1.compareTo(d2);
			}else{
				return result;
			}
		}
		
	}

}
