package playground.dhosse.cl.population;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Persona {
	
	private String id;
	private int age;
	private int sex;
	private boolean drivingLicence;
	
	private LinkedHashMap<String, Viaje> viajes = new LinkedHashMap<>();
//	private Viaje[] viajes;

	private int currentIdx = 0;
	
	public Persona(String id, int age, String  sex, String drivingLicence, String nViajes){
		
		this.id = id;
		this.age = age;
		this.sex = Integer.valueOf(sex);
		this.drivingLicence = setHasDrivingLicence(drivingLicence);
		
	}

	private boolean setHasDrivingLicence(String drivingLicence) {
		
		if(drivingLicence.equals("1")){
			
			return false;
			
		} else{
			
			return true;
			
		}
		
	}
	
	public void addViaje(Viaje viaje){
		
		this.viajes.put(viaje.getId(), viaje);
		
	}
	
	public String getId() {
		return id;
	}

	public double getAge() {
		return age;
	}

	public int getSex() {
		return sex;
	}

	public boolean isDrivingLicence() {
		return drivingLicence;
	}

	public Map<String, Viaje> getViajes() {
		return viajes;
	}

}
