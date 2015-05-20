package fr.flow10000.jme3.gwcam.entity.utils;

import java.util.LinkedList;
import java.util.Queue;

public class EntityIdSequence {

	private static EntityIdSequence instance = null;
	private EntityIdSequence(){}
	
	public static EntityIdSequence getInstance(){
		if(instance == null){
			instance = new EntityIdSequence();
		}
		return instance;
	}
	
	private Long next = 0L;
	private Queue<Long> freeIds = new LinkedList<Long>();
	
	public synchronized Long next(){
		
		if(!freeIds.isEmpty()){
			return freeIds.remove();
		}
		
		return next++;
	}
	
	public synchronized void release(Long idToRelease){
		freeIds.add(idToRelease);
	}
}