package playground.wrashid.lib.obj.geoGrid;

import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;

import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class GeoGridItems<T> {

	private LinkedListValueHashMap<String, T> gridItems;
	private int sideLengthInMeters;
	
	public GeoGridItems(int sideLengthInMeters){
		this.sideLengthInMeters=sideLengthInMeters;
		gridItems=new LinkedListValueHashMap<String, T>();
	}
	
	public LinkedList<T> getElementsWithinDistance(Coord coord, int numberOfGridUnits){
		LinkedList<T> resultList=new LinkedList<T>();
		
		int xGridCompLeftBottomCorner=convertToGridCoordinateComponent(coord.getX())-numberOfGridUnits;
		int yGridCompLeftBottomCorner=convertToGridCoordinateComponent(coord.getY())-numberOfGridUnits;
		
		for (int i=0;i<numberOfGridUnits*2+1;i++){
			for (int j=0;j<numberOfGridUnits*2+1;j++){
				LinkedList<T> tmpList=gridItems.get(getGridKeyFromComponents(xGridCompLeftBottomCorner+i,yGridCompLeftBottomCorner+j));
				resultList.addAll(tmpList);
			}
		}
	
		return resultList;
	}
	
	public void addElement(T element,Coord coord){
		gridItems.put(getGridKeyFromCoordinate(coord), element);
	}
	
	private String getGridKeyFromComponents(int xGridComp, int yGridComp) {
		return xGridComp + "," + yGridComp;
	}
	
	private String getGridKeyFromCoordinate(Coord coord) {
		return getGridKeyFromComponents (convertToGridCoordinateComponent(coord.getX()),convertToGridCoordinateComponent(coord.getY()));
	}
	
	private int convertToGridCoordinateComponent(double component){
		return (int) Math.round(component) / sideLengthInMeters;
	}
	
}
