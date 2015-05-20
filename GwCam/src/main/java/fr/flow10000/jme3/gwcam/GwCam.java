package fr.flow10000.jme3.gwcam;

import java.util.HashMap;
import java.util.Map;

import com.jme3.ai.navmesh.NavMesh;
import com.jme3.ai.navmesh.NavMeshPathfinder;
import com.jme3.ai.navmesh.Path.Waypoint;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

import fr.flow10000.jme3.gwcam.camera.CustomChaseCamera;
import fr.flow10000.jme3.gwcam.entity.AbstractTargetableEntity;
import fr.flow10000.jme3.gwcam.entity.impl.Tree;
import fr.flow10000.jme3.gwcam.entity.utils.EntityIdSequence;
import fr.flow10000.jme3.gwcam.utils.NavMeshUtils;

public class GwCam extends SimpleApplication implements ActionListener {
	
	private static final float FORWARD_SPEED = 10;
	private static final float BACKWARD_SPEED = 4;
	private static final float ROTATE_SPEED = .0010f;

	private CustomChaseCamera camera;
	
	private TerrainQuad terrain;
	
	private Mesh meshForNavMesh;
	private NavMesh navMesh;
	
	private Spatial player;

	private BetterCharacterControl playerControl;
	
	private Node playerNode;
	private Node terrainNode;
	
	private BulletAppState bulletAppState;
	
	private NavMeshPathfinder pathfinder = null;
	
	private Vector3f walkDirection = new Vector3f(0, 0, 0);
	private Vector3f viewDirection = new Vector3f(0, 0, 1);
	private boolean rotateLeft = false, rotateRight = false, strafLeft = false, strafRight = false, forward = false, backward = false, walk = false, forwardUnlimited = false;
	
	
	private AbstractTargetableEntity target = null;
	private Map<Long, AbstractTargetableEntity> targetables = new HashMap<Long, AbstractTargetableEntity>();
	
	
	@Override
	public void simpleInitApp() {
		Material genericSpacialMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		genericSpacialMaterial.setColor("Color", ColorRGBA.Blue);
		
		Material materialPlayer = new Material(assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");
		
		player = assetManager.loadModel("player.j3o");
		player.setMaterial(materialPlayer);
		
		playerNode = new Node();
		playerNode.attachChild(player);
		
		terrainNode = new Node();
		initTerrain(); // load terrain (mountains512)
		
		initTrees();
		
		initNav(); // build navmesh
		
		rootNode.attachChild(playerNode);
		rootNode.attachChild(terrainNode);
		
		initPhysics();
		
		initKeys();
		
		initCamera();
		
		
//		bulletAppState.getPhysicsSpace().enableDebug(assetManager);
		
		playerControl.warp(new Vector3f(0, 50, 0));
	}
	
	private void initTrees(){
		
		EntityIdSequence sequence = EntityIdSequence.getInstance();
		
		{Tree newTree = new Tree(assetManager, terrain, new Vector2f(50, 50), sequence.next());
		targetables.put(newTree.getId(), newTree);
		terrainNode.attachChild(newTree.getSpacialItems().get(0));}
		
		{Tree newTree = new Tree(assetManager, terrain, new Vector2f(60, 50), sequence.next());
		targetables.put(newTree.getId(), newTree);
		terrainNode.attachChild(newTree.getSpacialItems().get(0));}
		
		{Tree newTree = new Tree(assetManager, terrain, new Vector2f(50, 60), sequence.next());
		targetables.put(newTree.getId(), newTree);
		terrainNode.attachChild(newTree.getSpacialItems().get(0));}
		
		{Tree newTree = new Tree(assetManager, terrain, new Vector2f(66, 66), sequence.next());
		targetables.put(newTree.getId(), newTree);
		terrainNode.attachChild(newTree.getSpacialItems().get(0));}
		
		{Tree newTree = new Tree(assetManager, terrain, new Vector2f(45, 40), sequence.next());
		targetables.put(newTree.getId(), newTree);
		terrainNode.attachChild(newTree.getSpacialItems().get(0));}
		
		{Tree newTree = new Tree(assetManager, terrain, new Vector2f(34, 44), sequence.next());
		targetables.put(newTree.getId(), newTree);
		terrainNode.attachChild(newTree.getSpacialItems().get(0));}
	}

	public void onAction(String name, boolean isPressed, float tpf) {
		
		if (name.equals("Rotate Left")) {
			stopAction();
			rotateLeft = isPressed;

		} else if (name.equals("Rotate Right")) {
			stopAction();
			rotateRight = isPressed;

		} else if (name.equals("Straf Left")) {
			stopAction();
			strafLeft = isPressed;

		} else if (name.equals("Straf Right")) {
			stopAction();
			strafRight = isPressed;

		} else if (name.equals("Forward")) {
			stopAction();
			forward = isPressed;

		} else if (name.equals("Back")) {
			stopAction();
			backward = isPressed;

		} else if(name.equals("Forward Unlimited") && isPressed){
			walk = false;
			forwardUnlimited = !forwardUnlimited;
			
		} else if (name.equals("Stop Action") && isPressed) {
			stopAction();
			
		} else if (name.equals("Generic Mouse Action") && isPressed) {
			stopAction();
			this.onGenericMouseAction();
		}
	}
	
	private void stopAction(){
		walk = false;
		forwardUnlimited = false;
	}
	
	private void onGenericMouseAction() {

		CollisionResults results = new CollisionResults();
		Vector2f click2d = inputManager.getCursorPosition();
		Vector3f click3d = cam.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 0f).clone();
		Vector3f dir = cam.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 1f).subtractLocal(click3d).normalizeLocal();
		Ray ray = new Ray(click3d, dir);
		rootNode.collideWith(ray, results);

		if (results.size() > 0) {
			
			CollisionResult result = results.getClosestCollision();
			Geometry target = result.getGeometry();
			
			if (target.getName().startsWith("TERRAIN")) {

				Vector3f startPoint = new Vector3f(playerNode.getLocalTranslation());
				Vector3f targetPoint = new Vector3f(result.getContactPoint());

				navMesh.loadFromMesh(meshForNavMesh); // reload navmesh
				pathfinder = new NavMeshPathfinder(navMesh);
				pathfinder.setPosition(startPoint);
				pathfinder.computePath(targetPoint);

				if(pathfinder.getPath() != null && pathfinder.getPath().getWaypoints() != null && pathfinder.getPath().getWaypoints().size() > 0){
					System.out.println("> path found !");
					walk = true;
				} else {
					System.out.println("> path not found !");
				}
				
			} else {
				
				Long asEntityId = Long.parseLong(target.getName());
				if(targetables.containsKey(asEntityId)){
					this.target = targetables.get(asEntityId);
					
					// try to walk
					Vector3f startPoint = new Vector3f(playerNode.getLocalTranslation());
					Vector3f targetPoint = new Vector3f(this.target.getSpacialItems().get(0).getLocalTranslation());

					navMesh.loadFromMesh(meshForNavMesh); // reload navmesh
					pathfinder = new NavMeshPathfinder(navMesh);
					pathfinder.setPosition(startPoint);
					pathfinder.computePath(targetPoint);

					if(pathfinder.getPath() != null && pathfinder.getPath().getWaypoints() != null && pathfinder.getPath().getWaypoints().size() > 0){
						System.out.println("> path found !");
						walk = true;
					} else {
						System.out.println("> path not found !");
					}
				}
			}

		} else {
			System.out.println(">>> NOTHING");
		}
	}
	
	@Override
	public void simpleUpdate(float tpf) {

		walkDirection.set(0, 0, 0);
		
		if (walk) {

			Waypoint waypoint = pathfinder.getNextWaypoint();

			if (waypoint == null) {
				walk = false;
				return;
			}

			Vector3f vector = waypoint.getPosition().subtract(playerNode.getWorldTranslation());

			if (!(vector.length() < 1)) {

				walkDirection.addLocal(vector.normalize().mult(FORWARD_SPEED));
				playerControl.setWalkDirection(walkDirection);

			} else if (!pathfinder.isAtGoalWaypoint()) {
				pathfinder.goToNextWaypoint();
			} else {
				walk = false;
			}
			
			return; 
		}

//		Vector3f modelForwardDir = cam.getDirection();
		Vector3f modelForwardDir = new Vector3f(0, 0, 0);
		float speed = 0;
		
		if (forward || forwardUnlimited) {
			
			speed = FORWARD_SPEED;
			modelForwardDir.addLocal(cam.getDirection());
			
//			walkDirection.addLocal(modelForwardDir.mult(FORWARD_SPEED));
			
		} else if (backward) {
			
//			speed = BACKWARD_SPEED;
			
			
//			Vector3f local = new Vector3f(modelForwardDir.mult(BACKWARD_SPEED));
//			local.y = 0;
//			walkDirection.subtractLocal(local);
		}
		
		if(strafLeft) {
			
			if(speed == 0){
				speed = BACKWARD_SPEED;
			}
			
			Quaternion quat = new Quaternion();
			quat.fromAngleAxis(3 * FastMath.PI / 4, Vector3f.UNIT_Y);
			
			Vector3f lol = quat.mult(cam.getDirection());
			
			modelForwardDir.addLocal(lol);
			
//			modelForwardDir = quat.mult(viewDirection);
			
//			walkDirection = quat.mult(viewDirection);
//			walkDirection.addLocal(modelForwardDir.mult(FORWARD_SPEED));
//			walkDirection = quat.mult(walkDirection, walkDirection);
//			Vector3f local = new Vector3f(modelForwardDir.mult(BACKWARD_SPEED));
//			walkDirection.addLocal(local);
			
		} else if(strafRight) {
			
			Quaternion quat = new Quaternion();
			quat.fromAngleAxis(3 * FastMath.PI / 2, Vector3f.UNIT_Y);
			walkDirection = quat.mult(walkDirection);
			Vector3f local = new Vector3f(modelForwardDir.mult(BACKWARD_SPEED));
			walkDirection.addLocal(local);
			
		}
		
		if(speed > 0){
			walkDirection.addLocal(modelForwardDir.mult(speed));
		}
		
		
		
		if (rotateLeft) {
			Quaternion rotateL = new Quaternion().fromAngleAxis(ROTATE_SPEED, Vector3f.UNIT_Y);
			rotateL.multLocal(viewDirection);
			camera.onRotatePlayer(-ROTATE_SPEED);
		} else if (rotateRight) {
			Quaternion rotateR = new Quaternion().fromAngleAxis(-ROTATE_SPEED, Vector3f.UNIT_Y);
			rotateR.multLocal(viewDirection);
			camera.onRotatePlayer(ROTATE_SPEED);
		}
		
		walkDirection.y = 0;
		playerControl.setWalkDirection(walkDirection);
		playerControl.setViewDirection(viewDirection);
	}
	
	private void initKeys() {
		
		inputManager.addMapping("Forward Unlimited", new KeyTrigger(KeyInput.KEY_R));
		inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_Z));
		inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_S));
		inputManager.addMapping("Rotate Left", new KeyTrigger(KeyInput.KEY_Q));
		inputManager.addMapping("Rotate Right", new KeyTrigger(KeyInput.KEY_D));
		inputManager.addMapping("Straf Left", new KeyTrigger(KeyInput.KEY_A));
		inputManager.addMapping("Straf Right", new KeyTrigger(KeyInput.KEY_E));
		
		inputManager.addMapping("Stop Action", new KeyTrigger(KeyInput.KEY_ESCAPE));	// todo virer celui de la simpleApp

		inputManager.addMapping("Generic Mouse Action", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));

		inputManager.addListener(this, "Rotate Left", "Rotate Right");
		inputManager.addListener(this, "Straf Left", "Straf Right");
		inputManager.addListener(this, "Forward", "Back");
		inputManager.addListener(this, "Forward Unlimited");
		
		inputManager.addListener(this, "Stop Action");

		inputManager.addListener(this, "Generic Mouse Action");
	}
	
	
	private void initTerrain() {

		Material mat = new Material(assetManager, "Common/MatDefs/Terrain/Terrain.j3md");
		mat.setTexture("Alpha", assetManager.loadTexture("terrain/alphamap.png"));

		Texture grass = assetManager.loadTexture("terrain/grass.jpg");
		grass.setWrap(WrapMode.Repeat);
		mat.setTexture("Tex1", grass);
		mat.setFloat("Tex1Scale", 64f);

		Texture dirt = assetManager.loadTexture("terrain/dirt.jpg");
		dirt.setWrap(WrapMode.Repeat);
		mat.setTexture("Tex2", dirt);
		mat.setFloat("Tex2Scale", 32f);

		Texture rock = assetManager.loadTexture("terrain/road.jpg");
		rock.setWrap(WrapMode.Repeat);
		mat.setTexture("Tex3", rock);
		mat.setFloat("Tex3Scale", 128f);

		AbstractHeightMap heightmap = null;
		Texture heightMapImage = assetManager.loadTexture("terrain/mountains512_4.png");

		heightmap = new ImageBasedHeightMap(heightMapImage.getImage());
		heightmap.load();
		heightmap.smooth(0.8f, 1);

		int patchSize = 65;
		terrain = new TerrainQuad("TERRAIN_", patchSize, 513, heightmap.getHeightMap());

		terrain.setMaterial(mat);

		terrainNode.attachChild(terrain);
	}
	
	private void initNav(){
		NavMeshUtils nmUtils = new NavMeshUtils(assetManager);
		meshForNavMesh = nmUtils.buildMeshForNavMesh(terrainNode);
		navMesh = new NavMesh();
	}
	
	private void initCamera() {

		flyCam.setEnabled(false);
		camera = new CustomChaseCamera(cam, player, inputManager);
		camera.setSmoothMotion(false);
		camera.setInvertVerticalAxis(true);
	}
	
	private void initPhysics(){
		bulletAppState = new BulletAppState();
		bulletAppState.setDebugEnabled(true);
		stateManager.attach(bulletAppState);

		RigidBodyControl floorControl = new RigidBodyControl(0f);
		playerControl = new BetterCharacterControl(1f, 2.5f, 80f);

		playerNode.addControl(playerControl);
		terrainNode.addControl(floorControl);

		bulletAppState.getPhysicsSpace().add(playerNode);
		bulletAppState.getPhysicsSpace().add(terrainNode);
	}
}