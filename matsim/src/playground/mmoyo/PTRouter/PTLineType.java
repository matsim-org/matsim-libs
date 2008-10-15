package playground.mmoyo.PTRouter;

/**
 * Describes the type of Public Transport Line in a scenario
 * The abbreviation of values is
 *
 * B= "Bus"
 * T= "Trolley"  also "Tramway", "Tram", "Street car" ("Straﬂenbahn" in German speaking countries)
 * S= "Subway"     
 * C= "Commuter Rail"  also Suburban Metro ("S-Bahn" in German speaking countries)
 *
 * @param chrType The abbreviation of the name of the type
 * @param withDedicatedTracks  If has dedicated rails or interacts with normal vehicles 
 */

public class PTLineType {
	boolean hasDedicatedTracks;
	private char chrType;
	
	public PTLineType(char chrType){
		this.chrType=chrType;
		setWithDedicatedTracks();
	}

	private void setWithDedicatedTracks() {
		if (this.chrType =='S' || this.chrType =='C')
			this.hasDedicatedTracks=false;
		else if (this.chrType =='B' || this.chrType =='T')
			this.hasDedicatedTracks = true;
	}

	public boolean isWithDedicatedTracks() {
		return this.hasDedicatedTracks;
	}

	public char getChrType() {
		return chrType;
	}

	public void setChrType(char chrType) {
		this.chrType = chrType;
	}

	public String getType() {
		String[] type ={null, "Bus","Tram", "Subway", "Commuter Rail"};
		int i=0;
		if (this.chrType =='B')
			i=1;
		else if(this.chrType =='T')
			i=2;
		else if(this.chrType =='S')
			i=3;
		else if(this.chrType =='C')
			i=4;
		return type[i];
	}	 
}