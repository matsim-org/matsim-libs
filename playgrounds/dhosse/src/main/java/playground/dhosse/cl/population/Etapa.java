package playground.dhosse.cl.population;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

public class Etapa {
	
	private String comunaOrigen;
	private String comunaDestino;
	
	private Coord origin;
	private Coord destination;
	
	private String mode;
	
	public Etapa(String mode, String comunaOrigen, String comunaDestino, String originX, String originY, String destinationX, String destinationY){
		
		this.mode = mode;
		this.comunaOrigen = comunaOrigen;
		this.comunaDestino = comunaDestino;
		
		if(!originX.equals("") && !originY.equals("")){
			this.origin = new CoordImpl(originX.replace(",", "."), originY.replace(",", "."));
		}
		
		if(!destinationX.equals("") && !destinationY.equals("")){
			this.destination = new CoordImpl(destinationX.replace(",", "."), destinationY.replace(",", "."));
		}
		
	}

	public String getComunaOrigen() {
		return comunaOrigen;
	}

	public String getComunaDestino() {
		return comunaDestino;
	}

	public Coord getOrigin() {
		return origin;
	}

	public Coord getDestination() {
		return destination;
	}

	public String getMode() {
		return mode;
	}

}
