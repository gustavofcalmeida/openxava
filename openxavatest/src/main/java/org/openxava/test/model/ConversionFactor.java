package org.openxava.test.model;

import java.io.*;
import java.math.*;

import javax.persistence.*;
import javax.validation.constraints.*;

@Entity
public class ConversionFactor implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	private Long id;
	
	@Column(length = 30)
	private String fromUnit;
	
	@Column(length = 30)
	private String toUnit;
	
	@Column(scale = 6)
	private BigDecimal factor;
	
	
	@Digits(integer=10, fraction=6) 
	private BigDecimal reverseFactor;
	
	@Digits(integer=6, fraction=0) 
	@Max(200) // To test @Max combined with @Digits
	private BigDecimal storedFactorIndex; // tmr 
	
	@Column(scale = 6) 
	@DecimalMax("0.999999")
	public BigDecimal getMirrorFactor() {
		return factor;
	}
	
	// With no @Column, for a test
	public BigDecimal getShortFactor() { 
		return factor;
	}
	
	@Column(length=6, scale=0)
	public BigDecimal getFactorIndex() { 
		return factor.multiply(new BigDecimal("1000000"));
	}
	
	@Column(precision=6, scale=0)
	public BigDecimal getFactorGrade() { 
		return factor.multiply(new BigDecimal("1000000"));
	}	
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFromUnit() {
		return fromUnit;
	}

	public void setFromUnit(String fromUnit) {
		this.fromUnit = fromUnit;
	}

	public String getToUnit() {
		return toUnit;
	}

	public void setToUnit(String toUnit) {
		this.toUnit = toUnit;
	}

	public BigDecimal getFactor() {
		return factor;
	}

	public void setFactor(BigDecimal factor) {
		this.factor = factor;
	}

	public BigDecimal getReverseFactor() {
		return reverseFactor;
	}

	public void setReverseFactor(BigDecimal reverseFactor) {
		this.reverseFactor = reverseFactor;
	}

	public BigDecimal getStoredFactorIndex() {
		return storedFactorIndex;
	}

	public void setStoredFactorIndex(BigDecimal storedFactorIndex) {
		this.storedFactorIndex = storedFactorIndex;
	}

}
