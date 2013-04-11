package playground.wdoering.grips.v2.evacareaselector;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.ShapeFactory;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;
import playground.wdoering.grips.scenariomanager.model.AbstractToolBox;
import playground.wdoering.grips.scenariomanager.model.Constants;
import playground.wdoering.grips.scenariomanager.model.shape.PolygonShape;
import playground.wdoering.grips.scenariomanager.model.shape.Shape;

/**
 * the evacuation area selector tool box
 * 
 * - open button: opens the grips configuration file
 * - save button: saves the shape according to the
 *   destination given in the configuration
 * 
 * 
 * @author vvvvv
 *
 */
class EvacToolBox extends AbstractToolBox
{
	private static final long serialVersionUID = 1L;
	private JButton openBtn;
	private JButton saveButton;

	EvacToolBox(AbstractModule module, Controller controller)
	{
		super(module, controller);
		
		this.setLayout(new BorderLayout());
		
		JPanel buttonPanel = new JPanel();
		
		this.openBtn = new JButton(locale.btOpen());
		this.saveButton = new JButton(locale.btSave());
		this.saveButton.setEnabled(false);
		
		this.openBtn.addActionListener(this);
		this.saveButton.addActionListener(this);
		
		buttonPanel.add(this.openBtn);
		buttonPanel.add(this.saveButton);
		
		this.add(buttonPanel, BorderLayout.SOUTH);
		
	}
	
	@Override
	public void setGoalAchieved(boolean goalAchieved)
	{
		this.saveButton.setEnabled(goalAchieved);
		super.setGoalAchieved(goalAchieved);
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();
		
		if (cmd.equals(locale.btOpen()))
		{
			if (this.controller.openGripsConfig())
			{
				this.controller.disableAllRenderLayers();
				
				//add network bounding box shape
				int shapeRendererId = controller.getVisualizer().getPrimaryShapeRenderLayer().getId();
				Rectangle2D bbRect = controller.getBoundingBox();
				controller.addShape(ShapeFactory.getNetBoxShape(shapeRendererId, bbRect, false));
				
				//deactivate circle shape
				Shape circleShape = this.controller.getShapeById(Constants.ID_EVACAREAPOLY);
				if (circleShape != null)
					circleShape.setVisible(false);
				
				this.controller.getVisualizer().getActiveMapRenderLayer().setPosition(this.controller.getCenterPosition());
				this.saveButton.setEnabled(false);
				this.controller.enableAllRenderLayers();
			}
		}
		else if (cmd.equals(locale.btSave()))
		{
			Shape shape = controller.getShapeById(Constants.ID_EVACAREAPOLY);
			
			if (shape instanceof PolygonShape)
			{
				this.goalAchieved = controller.saveShape(shape, controller.getGripsConfigModule().getEvacuationAreaFileName());
				this.controller.setGoalAchieved(this.goalAchieved);
					
				this.saveButton.setEnabled(false);
			}				
		}
		
	}
	
}