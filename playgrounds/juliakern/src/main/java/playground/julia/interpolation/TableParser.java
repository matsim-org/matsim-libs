package playground.julia.interpolation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class TableParser {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String inputfile = "interpolation/no2export.txt";
		//static String emissionInputPath = "../../detailedEval/emissions/hbefaForMatsim/";
		String outputfile = new String ("interpolation/outputWithoutMinus.txt");
		int gridsizex = 135; //Zeilen was 135
		int gridsizey = 135; //Spalten was 135
		
		//initiate variables to find min and max values on both axes
		Double minx = new Double(Double.MAX_VALUE);
		Double maxx = new Double(Double.MIN_VALUE);
		Double miny = new Double(Double.MAX_VALUE);
		Double maxy = new Double(Double.MIN_VALUE);
		
		//initiate array
		//TODO muss das mit null initiert werden? muessen hinterher erkennen, wo wir nicht eingetragen haben, um dort NA eintragen zu koennen
		Double [][] mainarray = new Double [gridsizex+1][gridsizey+1];
		System.out.println("test size"+"x"+(gridsizex+1)+"y"+(gridsizey+1)+"sollte sein"+mainarray.length+"und"+mainarray[12].length);
		
		//Datei einlesen -> header?
		//mins und maxs finden + merken
		try {
			File file = new File(inputfile);
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line= new String("");
			//header
			in.readLine();
			while ((line = in.readLine())!= null){
				//TODO numberformatexception
				try {
					//TODO ordentlich trennen
					Double potMinMaxX = Double.parseDouble(line.substring(6, 20)); 
					Double potMinMaxY = Double.parseDouble(line.substring(21, 35));
					if(potMinMaxX<minx)minx=potMinMaxX;
					if(potMinMaxX>maxx)maxx=potMinMaxX;
					if(potMinMaxY<miny)miny=potMinMaxY;
					if(potMinMaxY>maxy)maxy=potMinMaxY;
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			System.out.println("minx+maxx+miny+maxy"+minx+" "+maxx+" "+miny+" "+maxy);
			
			//erneut zeilenweise lesen, werte ins array schreiben
			BufferedReader in2 = new BufferedReader(new FileReader(file));
			//header
			line=in2.readLine();
			while((line=in2.readLine())!= null){
				//Koordinaten rausfinden
				Double xCoordinate = Double.parseDouble(line.substring(6, 20));
				Double yCoordinate = Double.parseDouble(line.substring(21, 35));
				//arrayindizes ermitteln
				int arrayx= (int) (((xCoordinate-minx)/(maxx-minx))*gridsizex);
				int arrayy= (int) (((yCoordinate-miny)/(maxy-miny))*gridsizey);
				//wert im array eintragen
				String substring = line.substring(55);
				//TODO mit exponentialwerten umgehen
				if (!(substring.contains("e-"))){
					if(!(substring.contains("E"))){
						mainarray[arrayx][arrayy]=Double.parseDouble(line.substring(54));
						if(mainarray[arrayx][arrayy]<0)mainarray[arrayx][arrayy]=mainarray[arrayx][arrayy]*(-1);
					}
				}
			}
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		//TODO an dieser Stelle ggf interpolieren
		
		//kopfzeile und kopfspalte schreiben
		for(int i=0; i<mainarray.length;i++)mainarray[0][i]=(i+1)*1.0;
		for(int i=0; i<mainarray[0].length;i++)mainarray[i][0]=(i+1)*1.0;
		
		//TODO: in der ersten Zeile den letzten Eintrag loeschen
		
		//array in Textdatei schreiben
		BufferedWriter buff = new BufferedWriter (new FileWriter (outputfile));
		//inhalt schreiben
		String valueString = "\t";
		System.out.println(mainarray.length+" "+mainarray[20].length);
		for(int i = 0; i< mainarray.length; i++){
			
			//Tabelleninhalt
			for(int j = 0; j<mainarray[0].length; j++){
				if(mainarray[i][j]==null){
					valueString+="NA";
				} else{
					//write value in table
					if(!Double.toString(mainarray[i][j]).contains("E")){
						valueString+=Double.toString(mainarray[i][j]);
					}
					else valueString+="NA";
				}
				//tabulator
				//TODO pruefen, ob tab richtig ist
				valueString+="\t";
				
			}
			//schreiben und zeilenumbruch
			buff.write(valueString);
			buff.newLine();
			valueString="";
		}
		buff.close();
	}

}
