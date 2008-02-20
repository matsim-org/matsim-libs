package playground.andreas;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.io.tabularFileParser.TabularFileHandlerI;
import org.matsim.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.utils.io.tabularFileParser.TabularFileParserConfig;

public class CountsTransform implements TabularFileHandlerI{
	
	HashMap<Integer, Coord> xYCoords = new HashMap<Integer, Coord>();

	/**
	 * coordFile has to be structered as follows:<br>
	 *  - row 0: LinkID<br>
	 *  - row 1: x-Coord<br>
	 *  - row 2: y-Coord<br>
	 * each row separated by semicolon
	 * 
	 * @param args 0: countsInFile, 1: coordFile, 2: countsOutFile
	 */
	public static void main(String[] args) {
		
		if(args.length < 3){
			System.err.println("Too few arguments: args 0: countsInFile, 1: coordFile, 2: CountsOutFile");
		} else {
			CountsTransform myCountsTransform = new CountsTransform();	
			
			myCountsTransform.readXYFile(args[1]);		
			myCountsTransform.merge(args[0], args[2]);
				
			System.out.println("... Finished");
		}
				
	}

	private void readXYFile(String coordFile) {
		
		TabularFileParserConfig tabConfig = new TabularFileParserConfig();
		tabConfig.setFileName(coordFile);
//		tabConfig.setCommentTags(new String[]{"\""});		
		tabConfig.setDelimiterTags(new String[]{";"});
		TabularFileParser myFileReader = new TabularFileParser();
		
		try {
			myFileReader.parse(tabConfig, this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
	}

	private void merge(String countInFile, String countOutFile) {

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(countInFile)));
			
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(countOutFile)));
			
			Boolean moveOn = true;
			
			while(moveOn){
				
				String line = reader.readLine();
				
				if (line != null){
					
					if(line.contains("loc_id")){
												
						int firstEqual = line.indexOf('"');
						int secondEqual = line.indexOf('"', firstEqual + 1);
						
						String linkIDSubString = line.substring(firstEqual + 1, secondEqual);						
						Integer linkID = Integer.parseInt(linkIDSubString);
						
						if (xYCoords.get(linkID) != null){
												
							String tempNewLine = line.substring(0, line.length() - 1);						
							tempNewLine = tempNewLine.concat(" xCoord=\"" + xYCoords.get(linkID).getX() + "\" yCoord=\"" + xYCoords.get(linkID).getY() + "\">");
						
							writer.write(tempNewLine + "\n");
						
						} else {
							System.err.println("No or corrupted XY-Coords for link " + linkID + " > Copied line");
							writer.write(line + "\n");
						}
						
					} else {
						writer.write(line + "\n");
					}				
					
				} else {
					moveOn = false;
				}					
			}
			
			reader.close();
			writer.flush();
			writer.close();
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void startRow(String[] row) {
				
		if(row.length != 3){
			System.err.println("Line with too few or many arguments found");
		} else {
			
			if(row[0].length() != 0 && !row[0].startsWith("\"")){
				Coord coord = new Coord(Double.parseDouble(row[1]), Double.parseDouble(row[2]));
				xYCoords.put(Integer.parseInt(row[0]), coord);		

			} else System.err.println("Could not read: " + row[0] + row[1] + row[2]);

		}		
	}
}