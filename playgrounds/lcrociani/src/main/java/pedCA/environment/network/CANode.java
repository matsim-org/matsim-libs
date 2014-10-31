package pedCA.environment.network;

public class CANode {
	private int id;
	private int destinationId;
	private Coordinates coordinates;
	private double width;
	
	
	public CANode(int id, Coordinates coordinates, double width){
		this.id = id;
		this.coordinates = coordinates;
		this.width = width;
	}

	public int getId() {
		return id;
	}
	
	public int getDestinationId() {
		return destinationId;
	}

	public void setDestinationId(int destinationId) {
		this.destinationId = destinationId;
	}

	public Coordinates getCoordinates() {
		return coordinates;
	}
	
	public double getWidth() {
		return width;
	}
	
	public String toString(){
		String result = "Coordinates: "+coordinates.toString()+"\n";
		result += "WIDTH: "+width;
		return result;
	}
	
	
}
