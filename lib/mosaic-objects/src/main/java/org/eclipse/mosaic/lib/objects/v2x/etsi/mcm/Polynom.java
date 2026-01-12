package org.eclipse.mosaic.lib.objects.v2x.etsi.mcm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.eclipse.mosaic.lib.objects.ToDataOutput;

public class Polynom implements ToDataOutput {

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
	
	public Polynom(DataInput in) throws IOException {
		this.coefficients = new ArrayList<Double>();
		int size = in.readInt();
		for(int i = 0; i < size; i++) {
			in.readDouble();
		}
		start = in.readInt();
		end = in.readInt();
		xOffset = in.readInt();
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
	
	public double evaluate(double x) {
        double xAdjusted = x - getXOffsetValue();
        
        // Check if x is in valid range
        if (xAdjusted < getStartValue() || xAdjusted > getEndValue()) {
            throw new IllegalArgumentException(
                String.format("x=%.3f outside valid range [%.3f, %.3f]", 
                            x, getStartValue() + getXOffsetValue(), 
                            getEndValue() + getXOffsetValue()));
        }
        
        // Horner's method for polynomial evaluation
        double result = 0.0;
        for (int i = coefficients.size() - 1; i >= 0; i--) {
            result = result * xAdjusted + coefficients.get(i);
        }
        
        return result;
    }
	
	public Polynom derivative() {
        if (coefficients.size() == 1) {
            // Derivative of constant is 0
            return constant(0.0, getStartValue(), getEndValue());
        }
        
        List<Double> derivCoeffs = new ArrayList<>();
        for (int i = 1; i < coefficients.size(); i++) {
            derivCoeffs.add(coefficients.get(i) * i);
        }
        
        return new Polynom(derivCoeffs, start, end, xOffset);
    }
	
	public boolean isInRange(double x) {
        double xAdjusted = x - getXOffsetValue();
        return xAdjusted >= getStartValue() && xAdjusted <= getEndValue();
    }
	
	public int getDegree() {
        return coefficients.size() - 1;
    }
    
    public double getStartValue() {
        return start / UNIT_SCALE;
    }
    
    public double getEndValue() {
        return end / UNIT_SCALE;
    }
    
    public double getXOffsetValue() {
        return xOffset / UNIT_SCALE;
    }
    
    public List<Double> getCoefficients() {
        return Collections.unmodifiableList(coefficients);
    }
    
    public int getStart() {
        return start;
    }
    
    public int getEnd() {
        return end;
    }
    
    public int getXOffset() {
        return xOffset;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Poly[");
        for (int i = 0; i < coefficients.size(); i++) {
            if (i > 0) sb.append(" + ");
            sb.append(String.format("%.3f", coefficients.get(i)));
            if (i > 0) sb.append("*x^").append(i);
        }
        sb.append(String.format("] valid=[%.3f, %.3f]", 
                               getStartValue() + getXOffsetValue(),
                               getEndValue() + getXOffsetValue()));
        return sb.toString();
    }

	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		dataOutput.writeInt(coefficients.size());
		for(Double d : coefficients) {
			dataOutput.writeDouble(d);
		}
		dataOutput.writeInt(start);
		dataOutput.writeInt(end);
		dataOutput.writeInt(xOffset);
	}
	
}