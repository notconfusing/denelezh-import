package net.lehir.denelezh;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Occupation {
	
	private static Map<Long, Occupation> occupations = new HashMap<>();
	
	public static Occupation getOccupation(Long id) {
		if (occupations.containsKey(id)) {
			return occupations.get(id);
		}
		Occupation occupation = new Occupation();
		occupations.put(id, occupation);
		return occupation;
	}
	
	public static Map<Long, Occupation> getOccupations() {
		return occupations;
	}
	
	public boolean isTrueOccupation = false;
	private Map<Long, Occupation> parents = new HashMap<>();
	
	private Occupation() {
	}
	
	public void addParent(Long parentId) {
		parents.put(parentId, getOccupation(parentId));
	}
	
	public void getAllParents(Set<Long> allParents) {
		for (Long parentId : parents.keySet()) {
			if (!allParents.contains(parentId)) {
				allParents.add(parentId);
				parents.get(parentId).getAllParents(allParents);
			}
		}
	}
	
	public Map<Long, Occupation> getParents() {
		return parents;
	}
	
}
