package com.lee.password.util;

public class Triple<E1, E2, E3> {

	public final E1 first;
	public final E2 second;
	public final E3 third;
	
	public Triple(E1 first, E2 second, E3 third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}
	
	public static <E1, E2, E3> Triple<E1, E2, E3> create(E1 first, E2 second, E3 third) {
		return new Triple<E1, E2, E3>(first, second, third);
	}
}
