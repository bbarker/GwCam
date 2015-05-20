package fr.flow10000.jme3.gwcam.entity;

import java.util.ArrayList;
import java.util.List;

import com.jme3.scene.Spatial;

public abstract class AbstractTargetableEntity implements Entity {

	private final Long id;
	
	protected List<Spatial> spacialItems;  
	
	public AbstractTargetableEntity(Long id, int spacialSize){
		this.id = id;
		this.spacialItems = new ArrayList<Spatial>(spacialSize);
	}
	
	public Long getId() {
		return id;
	}

	public List<Spatial> getSpacialItems(){
		return spacialItems;
	}

}
