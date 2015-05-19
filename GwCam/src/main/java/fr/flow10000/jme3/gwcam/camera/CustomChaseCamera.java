package fr.flow10000.jme3.gwcam.camera;

import com.jme3.input.ChaseCamera;
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;

public class CustomChaseCamera extends ChaseCamera {

	public CustomChaseCamera(Camera cam, Spatial target, InputManager inputManager) {
		super(cam, target, inputManager);
		
		inputManager.deleteTrigger(ChaseCamToggleRotate, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
	}
	
	public void onRotatePlayer(float value){
		
		boolean canRotateInitial = this.canRotate;
		
		this.canRotate = true;
		this.rotateCamera(value);
		
		this.canRotate = canRotateInitial;
	}

}
