package playground.dhosse.qgis;

import java.awt.Color;

public class ColorRamp {
	
	private String type = "gradient";
	private String name = "[source]";
	private Color color1 = new Color(215,25,28,255);
	private Color color2 = new Color(26,150,65,255);
	private int discrete = 0;
	private int inverted = 1;
	private String mode = "equal";
	
	private String[] stops = {"0.25;253,174,97,255:","0.5;255,255,192,255:","0.75;166,217,106,255"};
	
	public ColorRamp(Color color1, Color color2, int inverted, String mode){
		
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public Color getColor1() {
		return color1;
	}

	public Color getColor2() {
		return color2;
	}

	public int getDiscrete() {
		return discrete;
	}

	public String[] getStops() {
		return stops;
	}
	
	public int isInverted(){
		return this.inverted;
	}
	
	public String getMode(){
		return this.mode;
	}

}
