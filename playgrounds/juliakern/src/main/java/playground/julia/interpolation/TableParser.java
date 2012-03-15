package playground.julia.interpolation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Math;

public class TableParser {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String inputfile = "interpolation/no2export.txt";
		//static String emissionInputPath = "../../detailedEval/emissions/hbefaForMatsim/";
		String outputfile = new String ("interpolation/out.txt");
		int gridsizex = 120; //Zeilen was 100
		int gridsizey = 160; //Spalten was 100
		
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
					Double potMinMaxY = Double.parseDouble(line.substring(6, 20)); 
					Double potMinMaxX = Double.parseDouble(line.substring(21, 35));
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
				Double yCoordinate = Double.parseDouble(line.substring(6, 20));
				Double xCoordinate = Double.parseDouble(line.substring(21, 35));
				//arrayindizes ermitteln
				int arrayx= (int) (((xCoordinate-minx)/(maxx-minx))*gridsizex);
				int arrayy= (int) (((yCoordinate-miny)/(maxy-miny))*gridsizey);
				//wert im array eintragen
				String substring = line.substring(55);
				//mit exponentialwerten umgehen
				if ((substring.contains("e-")||substring.contains("E"))){
					mainarray[arrayx][arrayy]=getDoubleFromEString(substring);
				} else {
					mainarray[arrayx][arrayy]=Double.parseDouble(line.substring(54));
					if(mainarray[arrayx][arrayy]<0)mainarray[arrayx][arrayy]=mainarray[arrayx][arrayy]*(-1);
				}
			}
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		//TODO an dieser Stelle interpolieren
		
		//TODO ueberlegen, ob kopfzeile und kopfspalte im array stehen sollen
		//kopfzeile und kopfspalte schreiben
		for(int i=0; i<gridsizey+1;i++)mainarray[0][i]=(i)*1.0;
		for(int i=0; i<gridsizex+1;i++){
			mainarray[i][0]=(i+1)*1.0;
		}
		System.out.println(mainarray[0][1]);
		//TODO: in der ersten Zeile den letzten Eintrag loeschen
		
		//array in Textdatei schreiben
		BufferedWriter buff = new BufferedWriter (new FileWriter (outputfile));
		//inhalt schreiben
		String valueString = new String();
		for(int i = 0; i< mainarray.length; i++){
			//Tabelleninhalt
			for(int j = 0; j<mainarray[0].length; j++){
				if(i==j&&j==0){
					//valueString+="\t";
				}
				else{
					if(mainarray[i][j]==null){
					valueString+="NA";
					} else{
					//write value in table
					//TODO runden?
						if(!Double.toString(mainarray[i][j]).contains("E")){
						valueString+=Double.toString(mainarray[i][j]);
						}
						else{
							valueString+=getStringFromEValue(mainarray[i][j]);
						}
					}
				}
			
				//tabulator
				valueString+="\t";
				
			}
			//schreiben und zeilenumbruch
			buff.write(valueString);
			buff.newLine();
			valueString="";
		}
		buff.close();
	}

	private static String getStringFromEValue(Double value) {
		if(value<1){
			int i =Math.getExponent(value);
			String tore = value.toString().split("E")[0];
			tore = tore.replace(".", "");
			for (int j=-1;j>i; j--){tore= "0"+tore;}
			tore="0."+tore;
			return tore;
		}
		else System.out.println("big exponent. this case is not handled. treated as not a number");
		return "NA";
	}

	private static Double getDoubleFromEString(String substring) {
		String[] strArray =substring.split("e-");
		Double value = Double.parseDouble(strArray[0]);
		Double exponent = Math.pow(10.0, Double.parseDouble(strArray[1]));
		return (value/exponent);
	}

}
