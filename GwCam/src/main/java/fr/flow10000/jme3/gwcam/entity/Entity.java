package fr.flow10000.jme3.gwcam.entity;

import java.io.Serializable;
import java.util.List;

import com.jme3.scene.Spatial;

public interface Entity {

	public Long getId();
	public List<Spatial> getSpacialItems(); // Spatial
}