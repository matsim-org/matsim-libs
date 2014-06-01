package playground.vbmh.einzel_klassen_tests;

import java.util.LinkedList;

import playground.vbmh.util.CSVReader;
import playground.vbmh.util.VMBoxPlot;
import playground.vbmh.util.VMCharts;

public class boxPlotTest {
	public static void main(String[] args) {
		CSVReader reader = new CSVReader();
		VMCharts charts = new VMCharts();
		charts.addChart("peter");
		charts.addSeries("peter", "walk ev");
		charts.addSeries("peter", "walk nev");
		charts.setInterval("peter", 800);
		charts.setBoxXStart("peter", 20000);
		charts.setBoxXEnd("peter", 20000+3600*15);
		charts.setBox("peter", true);
		charts.setLine("peter", true);
		LinkedList<String[]> werte = reader.readCSV("input/boxp/ev.csv", ",");
		LinkedList<String[]> werteb = reader.readCSV("input/boxp/nev.csv", ",");
		boolean firstline = true;
		for(String[] wert : werte){
			//System.out.println("read line");
			if(!firstline){
				if(Double.parseDouble(wert[1])==0.0){
					continue;
				}
				charts.addValues("peter", "walk ev", Double.parseDouble(wert[0]), Double.parseDouble(wert[1]));
			}else {
				firstline = false;
			}
			//System.out.println("read line finish");
		}
		firstline=true;
		for(String[] wert : werteb){
			//System.out.println("read line");
			if(!firstline){
				if(Double.parseDouble(wert[1])==0.0){
					continue;
				}
				charts.addValues("peter", "walk nev", Double.parseDouble(wert[0]), Double.parseDouble(wert[1]));
			}else {
				firstline = false;
			}
			//System.out.println("read line finish");
		}
		
		
		charts.printCharts("input/boxp/test", 0);
		
		
	}
}
