package playground.wdoering.grips.scenariomanager.model.process;

import javax.swing.JFileChooser;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.eventlistener.AbstractListener;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;
import playground.wdoering.grips.scenariomanager.model.AbstractToolBox;
import playground.wdoering.grips.scenariomanager.model.locale.Locale;
import playground.wdoering.grips.scenariomanager.view.DefaultOpenDialog;
import playground.wdoering.grips.scenariomanager.view.renderer.JXMapRenderer;
import playground.wdoering.grips.scenariomanager.view.renderer.GridRenderer;
import playground.wdoering.grips.scenariomanager.view.renderer.ShapeRenderer;

public abstract class AbstractProcess implements ProcessInterface
{
	protected Controller controller;
	protected Locale locale;
	protected AbstractModule module;
	
	public AbstractProcess(AbstractModule module, Controller controller)
	{
		this.module = module;
		this.controller = controller;
		this.locale = controller.getLocale();
		
	}
	
	public boolean checkOrOpenGripsConfig()
	{
		
		if (controller.getGripsConfigModule() == null)
			return openGripsConfig();
		
		return true;
	}
	
	public boolean openGripsConfig()
	{
		DefaultOpenDialog openDialog = new DefaultOpenDialog(controller, "xml", locale.infoGripsFile(), true);
		int returnValue = openDialog.showOpenDialog(controller.getParentComponent());
		
		if (returnValue == JFileChooser.APPROVE_OPTION)
			return controller.openGripsConfig(openDialog.getSelectedFile());
		else
			return false;
	}
	
	/**
	 * adds slippy map and transfers slippy map event listeners to the controller
	 * 
	 */
	public void addMapViewer()
	{
		//add new jx map viewer interface
		JXMapRenderer jxMapRenderer = new JXMapRenderer(controller, null, null);
		controller.addRenderLayer(jxMapRenderer);
		controller.setSlippyMapEventListeners(jxMapRenderer.getInheritedEventListeners());
		
	}
	
	public void setListeners(AbstractListener listeners)
	{
		//set and attach listeners
		controller.setListener(listeners);
		controller.setMainPanelListeners(true);
	}
	
	public void addShapeRenderer(ShapeRenderer shapeRenderer)
	{
		controller.addRenderLayer(shapeRenderer);
	}
	
	public void addNetworkRenderer(GridRenderer renderer)
	{
		controller.addRenderLayer(renderer);
	}
	
	public void addToolBox(AbstractToolBox toolBox)
	{
		this.controller.setActiveToolBox(toolBox);
	}
	

	@Override
	public void start() {}

}
