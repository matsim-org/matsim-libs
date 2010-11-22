package playground.fhuelsmann.emissions;
public class RoadTypeObject{

private int ObjectId;
private int Road_type_nur;
private int RoadSection;
public RoadTypeObject(int objectId, int road_type_nur, int roadSection) {
	super();
	ObjectId = objectId;
	Road_type_nur = road_type_nur;
	RoadSection = roadSection;
}

public int getObjectId() {
	return ObjectId;
}
public void setObjectId(int objectId) {
	ObjectId = objectId;
}
public int getRoad_type_nur() {
	return Road_type_nur;
}
public void setRoad_type_nur(int road_type_nur) {
	Road_type_nur = road_type_nur;
}
public int getRoadSection() {
	return RoadSection;
}
public void setRoadSection(int roadSection) {
	RoadSection = roadSection;
}



	
}
