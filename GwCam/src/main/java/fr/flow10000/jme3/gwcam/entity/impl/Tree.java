package fr.flow10000.jme3.gwcam.entity.impl;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;

import fr.flow10000.jme3.gwcam.entity.AbstractTargetableEntity;

public class Tree extends AbstractTargetableEntity {

	public Tree(AssetManager assetManager, TerrainQuad terrain, Vector2f position, Long id) {
		super(id, 1);
		this.load(assetManager, terrain, position);
	}
	
	private void load(AssetManager assetManager, TerrainQuad terrain, Vector2f position2d){
		
		Material mat = new Material(assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");
		
		Spatial spacial = assetManager.loadModel("tree.j3o"); 
		
		spacial.setMaterial(mat);
		spacial.setName(getId().toString());
		spacial.scale(5.f);
		
		Vector3f position = new Vector3f(position2d.x, 0, position2d.y);
		position.setY(terrain.getHeight(new Vector2f(position.x, position.z)));
		
		spacial.setLocalTranslation(position);
		
		spacialItems.add(spacial);
	}
	
}
