package playground.balac.utils.mobilitydataanalysis;

public class Station {
	
	private double CoordX = 0.0;
	private double CoordY = 0.0;
	
	private int cars = 0;
	
	private int id;
	
	public Station(double CoordX, double CoordY, int cars, int id) {
		
		this.CoordX = CoordX;
		this.CoordY = CoordY;
		this.cars = cars;
		this.setId(id);
	}

	public double getCoordX() {
		return CoordX;
	}

	public void setCoordX(double coordX) {
		CoordX = coordX;
	}

	public double getCoordY() {
		return CoordY;
	}

	public void setCoordY(double coordY) {
		CoordY = coordY;
	}

	public int getCars() {
		return cars;
	}

	public void setCars(int cars) {
		this.cars = cars;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	

}
