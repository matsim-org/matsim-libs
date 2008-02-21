package playground.david.vis.data;

public interface SceneLayer {
	public void init(SceneGraph graph);
	public void finish();
	public void addItem(OTFData.Receiver item);
	public void draw();
	public Object newInstance(Class clazz) throws InstantiationException, IllegalAccessException;
	public int getDrawOrder();
	
}

