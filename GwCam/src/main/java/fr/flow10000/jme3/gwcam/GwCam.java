package fr.flow10000.jme3.gwcam;

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
	private boolean rotateLeft = false, rotateRight = false, forward = false, backward = false, walk = false;
	
	
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
		initNav(); // build navmesh
		
		rootNode.attachChild(playerNode);
		rootNode.attachChild(terrainNode);
		
		initPhysics();
		
		initKeys();
		
		initCamera();
		
//		bulletAppState.getPhysicsSpace().enableDebug(assetManager);
		
		playerControl.warp(new Vector3f(0, 50, 0));
	}

	public void onAction(String name, boolean isPressed, float tpf) {
		
		if (name.equals("Rotate Left")) {
			rotateLeft = isPressed;
			walk = false;

		} else if (name.equals("Rotate Right")) {
			rotateRight = isPressed;
			walk = false;

		} else if (name.equals("Forward")) {
			forward = isPressed;
			walk = false;

		} else if (name.equals("Back")) {
			backward = isPressed;
			walk = false;

		} else if (name.equals("Generic Mouse Action") && isPressed) {
			this.onGenericMouseAction();
		}
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
				} else {
					System.out.println("> path not found !");
				}
				
				walk = true;
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

		Vector3f modelForwardDir = cam.getDirection();
		if (forward) {
			walkDirection.addLocal(modelForwardDir.mult(FORWARD_SPEED));
			
		} else if (backward) {

			walkDirection.addLocal(modelForwardDir.mult(BACKWARD_SPEED).negate());
		}
		playerControl.setWalkDirection(walkDirection);
		if (rotateLeft) {
			Quaternion rotateL = new Quaternion().fromAngleAxis(ROTATE_SPEED, Vector3f.UNIT_Y);
			rotateL.multLocal(viewDirection);
			camera.onRotatePlayer(-ROTATE_SPEED);
		} else if (rotateRight) {
			Quaternion rotateR = new Quaternion().fromAngleAxis(-ROTATE_SPEED, Vector3f.UNIT_Y);
			rotateR.multLocal(viewDirection);
			camera.onRotatePlayer(ROTATE_SPEED);
		}

		playerControl.setViewDirection(viewDirection);
	}
	
	private void initKeys() {
		inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_Z));
		inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_S));
		inputManager.addMapping("Rotate Left", new KeyTrigger(KeyInput.KEY_Q));
		inputManager.addMapping("Rotate Right", new KeyTrigger(KeyInput.KEY_D));

		inputManager.addMapping("Generic Mouse Action", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));

		inputManager.addListener(this, "Rotate Left", "Rotate Right");
		inputManager.addListener(this, "Forward", "Back");

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
		Texture heightMapImage = assetManager.loadTexture("terrain/mountains512.png");

		heightmap = new ImageBasedHeightMap(heightMapImage.getImage());
//		heightmap = new ImageBasedHeightMap(ImageToAwt.convert(heightMapImage.getImage(), false, true, 0));//, 0.5f);
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