package org.eclipse.mosaic.lib.objects.v2x.etsi.mcm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

public class Polynom {

	private List<Double> coefficients;
	private int start; // 0.001 m unit
	private int end; // 0.001 m unit
	private int xOffset; // 0.001 m unit
	
	private static final double UNIT_SCALE = 1000.0; // convert from 0.001 m to 1 m
	
	private Polynom(@Nonnull List<Double> coefficients, int start, int end, int xOffset) {
		if (coefficients.isEmpty() || coefficients.size() > 6) {
			throw new IllegalArgumentException("Coefficients must be 1 - 6 elements");
		}
		
		if (start < 0 || start > 2097150) {
			throw new IllegalArgumentException("Start must be 0 - 2097150");
		}
		
		if (end < 0 || end > 2097150) {
			throw new IllegalArgumentException("End must be 0 - 2097150");
		}
		
		if (xOffset < -800000 || xOffset > 800000) {
			throw new IllegalArgumentException("xOffset must be -800000 - 800000");
		}
		
		this.coefficients = new ArrayList<>(coefficients);
		this.start = start;
		this.end = end;
		this.xOffset = xOffset;
		
	}
	
	public static Polynom constant(double constant, double start, double end) {
		return new Polynom(Arrays.asList(constant), (int) (start * UNIT_SCALE), (int) (end * UNIT_SCALE), 0);
	}
	
	public static Polynom linear(double c0, double c1, double start, double end) {
		return new Polynom(Arrays.asList(c0, c1), (int) (start * UNIT_SCALE), (int) (end * UNIT_SCALE), 0);
	}
	
	public static Polynom quadratic(double c0, double c1, double c2, double start, double end) {
		return new Polynom(Arrays.asList(c0, c1, c2), (int) (start * UNIT_SCALE), (int) (end * UNIT_SCALE), 0);
	}
	
}