package playground.jjoubert.CommercialTraffic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.TreeSet;

public class AnalyseThroughTraffic {
	// Mac
	final static String ROOT = "/Users/johanwjoubert/MATSim/workspace/MATSimData/";
	// IVT-Sim0
//	final static String ROOT = "/home/jjoubert/";
	// Derived string values:
	final static String GT_FILE = ROOT + "Gauteng/Activities/WithinThrough/GautengThroughVehicleStats.txt";
	final static String KZN_FILE = ROOT + "KZN/Activities/WithinThrough/KZNThroughVehicleStats.txt";
	final static String WC_FILE = ROOT + "WesternCape/Activities/WithinThrough/WesternCapeThroughVehicleStats.txt";
	
	private static TreeSet<Integer> throughVehicles;
	
	public static void main( String [] args){
		throughVehicles = new TreeSet<Integer>();
		ArrayList<Integer> result = new ArrayList<Integer>();
		for(int i = 0; i < 8; i++ ){
			result.add( 0 );
		}
		
		
		// Build the tree sets
		TreeSet<Integer> inGauteng = buildTreeSet( GT_FILE );
		TreeSet<Integer> inKZN = buildTreeSet( KZN_FILE );
		TreeSet<Integer> inWesternCape = buildTreeSet( WC_FILE );
		
		// Analyse each vehicle
		for (Integer vehicle : throughVehicles) {
			String g = null;
			String k = null;
			String w = null;
			String where = null;
			
			g = inGauteng.contains(vehicle) ? "1" : "0";
			k = inKZN.contains(vehicle) ? "1" : "0";
			w = inWesternCape.contains(vehicle) ? "1" : "0";
			
			where = g + k + w;
			if(where.equals( "000") ){
				int dummy = result.get(0);
				result.set(0, dummy + 1 );
			} else if(where.equals( "001" ) ){
				int dummy = result.get(1);
				result.set(1, dummy + 1);
			} else if(where.equals( "010" ) ){
				int dummy = result.get(2);
				result.set(2, dummy + 1);
			} else if(where.equals( "011" ) ){
				int dummy = result.get(3);
				result.set(3, dummy + 1);
			} else if(where.equals( "100" ) ){
				int dummy = result.get(4);
				result.set(4, dummy + 1);
			} else if(where.equals( "101" ) ){
				int dummy = result.get(5);
				result.set(5, dummy + 1);
			} else if(where.equals( "110" ) ){
				int dummy = result.get(6);
				result.set(6, dummy + 1);
			} else if(where.equals( "111" ) ){
				int dummy = result.get(7);
				result.set(7, dummy + 1);
			} else{
				System.out.printf("Vehicle %d is not in any province...", vehicle );
			}
		}	
		int total = 0;
		for (int i = 0; i < result.size(); i++ ) {
			total += result.get(i);
			System.out.printf("Option %1d has %4d vehicles\n", i, result.get(i) );
		}
		System.out.printf("\nTotal number of vehicles considered 'through' traffic: %4d\n", total);
		
	}

	private static TreeSet<Integer> buildTreeSet(String theFile ) {
		TreeSet<Integer> tree = new TreeSet<Integer>();
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File( theFile ) ) ) );
			input.nextLine();
			
			while(input.hasNextLine() ){
				String [] lineSplit = input.nextLine().split( "," );
				int vehicle = Integer.parseInt( lineSplit[0] );
				tree.add( vehicle );
				if( !throughVehicles.contains( vehicle ) ){
					throughVehicles.add( vehicle );
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return tree;
	}

}
