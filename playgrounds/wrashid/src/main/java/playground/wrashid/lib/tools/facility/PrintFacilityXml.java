package playground.wrashid.lib.tools.facility;

public class PrintFacilityXml {

	public static void main(String[] args) {
		int counter=0;
		for (int i=0;i<10;i++){
			for (int j=0;j<9;j++){
				double x=i*1000;
				double y=500 +j*1000;
				System.out.println("\t<facility id=\"" + counter + "\" x=\"" + x + "\" y=\""  + y + "\">");
				System.out.println("\t\t<activity type=\"parking\"/>");
				System.out.println("\t</facility>");
				System.out.println();
				counter++;
			}
		}
		
		for (int i=0;i<9;i++){
			for (int j=0;j<10;j++){
				double x=500 +i*1000;
				double y=j*1000;
				System.out.println("\t<facility id=\"" + counter + "\" x=\"" + x + "\" y=\""  + y + "\">");
				System.out.println("\t\t<activity type=\"parking\"/>");
				System.out.println("\t</facility>");
				System.out.println();
				counter++;
			}
		}
		
	}
	
}
