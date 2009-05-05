package playground.anhorni.locationchoice.valid.counts;

public class LinkInfo {
	
	private String direction;
	private String linkidTeleatlas;
	private String linkidNavteq;
	private String linkidAre;
	private String linkidIVTCH;
	
	
	
	public LinkInfo(String direction, String linkidTeleatlas,
			String linkidNavteq, String linkidAre, String linkidIVTCH) {
		this.direction = direction;
		this.linkidTeleatlas = linkidTeleatlas;
		this.linkidNavteq = linkidNavteq;
		this.linkidAre = linkidAre;
		this.linkidIVTCH = linkidIVTCH;
	}
	
	
	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}
	public String getLinkidTeleatlas() {
		return linkidTeleatlas;
	}
	public void setLinkidTeleatlas(String linkidTeleatlas) {
		this.linkidTeleatlas = linkidTeleatlas;
	}
	public String getLinkidNavteq() {
		return linkidNavteq;
	}
	public void setLinkidNavteq(String linkidNavteq) {
		this.linkidNavteq = linkidNavteq;
	}
	public String getLinkidAre() {
		return linkidAre;
	}
	public void setLinkidAre(String linkidAre) {
		this.linkidAre = linkidAre;
	}
	public String getLinkidIVTCH() {
		return linkidIVTCH;
	}
	public void setLinkidIVTCH(String linkidIVTCH) {
		this.linkidIVTCH = linkidIVTCH;
	}

}
