package com.gpergrossi.util.data;

import java.util.ArrayList;
import java.util.List;

public class Lists {

	@SafeVarargs
	public static <T> List<T> of(T... elements) {
		List<T> result = new ArrayList<T>(elements.length);
		
		for (T element : elements) {
			result.add(element);
		}
		
		return result;
	}
	
}
