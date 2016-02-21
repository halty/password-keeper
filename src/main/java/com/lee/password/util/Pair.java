package com.lee.password.util;


public final class Pair<E1, E2> {

	public final E1 first;
	public final E2 second;
	
	public Pair(E1 first, E2 second) {
		this.first = first;
		this.second = second;
	}
	
	public static <E1, E2> Pair<E1, E2> create(E1 first, E2 second) {
		return new Pair<E1, E2>(first, second);
	}
}
