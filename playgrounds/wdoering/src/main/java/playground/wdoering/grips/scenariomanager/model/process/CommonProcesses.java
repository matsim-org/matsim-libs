package playground.wdoering.grips.scenariomanager.model.process;

import java.util.ArrayList;
import java.util.HashMap;

import org.dom4j.tree.AbstractProcessingInstruction;

import playground.wdoering.grips.scenariomanager.control.Controller;

public class CommonProcesses
{
	public enum ProcessType { LAYERS_ENABLE, LAYERS_DISABLE, CONFIG_GRIPS_INIT, CONFIG_MATSIM_INIT, LAYER_SHAPE_INIT, LAYER_MAP_INIT }
	
	public static final HashMap<ProcessType, BasicProcess> processes = new HashMap<CommonProcesses.ProcessType, BasicProcess>();
	public Controller controller;
	
	public CommonProcesses(Controller controller)
	{
		this.controller = controller;
	}
	
	

//	public static BasicProcess get(ProcessType type)
//	{
//		if (!processes.containsKey(type))
//		{
//			switch (type)
//			{
//				case LAYERS_ENABLE:
//					processes.put(type, new EnableLayersProcess(controller));
//				break;
//				case LAYERS_DISABLE:
//					processes.put(type, new DisableLayersProcess(controller));
//				break;
//				case LAYER_MAP_INIT:
//					processes.put(type, new InitMapLayerProcess(controller));
//				break;
//				case LAYER_SHAPE_INIT:
//					processes.put(type, new InitShapeLayerProcess(controller));
//				break;
//				case CONFIG_GRIPS_INIT:
//					
//				break;
//				case CONFIG_MATSIM_INIT:
//					
//				break;
//				
//			
//			}
//		}
//		
//		return this.processes.get(type);
//	}

	public static BasicProcess disableLayers()
	{
//		if (!processes.containsKey(type))
//			processes.add(new )
		return null;
	}
	

}
