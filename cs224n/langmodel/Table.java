package cs224n.langmodel;

import java.util.HashMap;
import java.util.Set;

public class Table<Key, Value> {
	
	HashMap hash = new HashMap();
	Integer childCount = 0;
	
	void put(Key k, Value v){
		hash.put(k, v);
	}
	
	Value get(Key k){
		return (Value)hash.get(k);
	}
	
	boolean containsKey(Key k)	{
		return hash.containsKey(k);
	}
	
	Set keySet()	{
		return hash.keySet();
	}
}
