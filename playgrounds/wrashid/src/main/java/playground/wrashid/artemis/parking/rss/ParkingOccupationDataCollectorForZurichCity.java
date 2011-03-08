package playground.wrashid.artemis.parking.rss;

import java.util.Date;

public class ParkingOccupationDataCollectorForZurichCity {

	public static void main(String[] args) {
		
		printParkingNames();
		
		System.out.println();
		
		while (true){
			printParkingOccupancies();
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		
		
	}

	private static void printParkingOccupancies() {
		RSSParser rssParser=new RSSParser("http://www.pls-zh.ch/plsFeed/rss");
		
		System.out.print(new Date().toString() + "\t");
		
		for (int i=0;i<rssParser.itemCount();i++){
			System.out.print(rssParser.getItem(i).getDescription()+"\t");
			rssParser.getItem(i).getTitle();
		}
		
		System.out.println();
	}

	private static void printParkingNames() {
		RSSParser rp=new RSSParser("http://www.pls-zh.ch/plsFeed/rss");
		
		System.out.print("[time]\t");
		
		for (int i=0;i<rp.itemCount();i++){
			System.out.print(rp.getItem(i).getTitle()+"\t");
		}
	}
	
}
