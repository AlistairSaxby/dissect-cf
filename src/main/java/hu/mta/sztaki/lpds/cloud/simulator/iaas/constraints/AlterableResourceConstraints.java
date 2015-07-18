/*
 *  ========================================================================
 *  DIScrete event baSed Energy Consumption simulaTor 
 *    					             for Clouds and Federations (DISSECT-CF)
 *  ========================================================================
 *  
 *  This file is part of DISSECT-CF.
 *  
 *  DISSECT-CF is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at
 *  your option) any later version.
 *  
 *  DISSECT-CF is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 *  General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with DISSECT-CF.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  (C) Copyright 2014, Gabor Kecskemeti (gkecskem@dps.uibk.ac.at,
 *   									  kecskemeti.gabor@sztaki.mta.hu)
 */
package hu.mta.sztaki.lpds.cloud.simulator.iaas.constraints;

public class AlterableResourceConstraints extends ResourceConstraints {
	private double requiredCPUs;
	private double requiredProcessingPower;
	private boolean requiredProcessingIsMinimum;
	private long requiredMemory;
	private double totalProcessingPower;

	public AlterableResourceConstraints(final double cpu,
			final double processing, final long memory) {
		this(cpu, processing, false, memory);
	}

	public AlterableResourceConstraints(final double cpu,
			final double processing, boolean isMinimum, final long memory) {
		requiredCPUs = cpu;
		requiredMemory = memory;
		requiredProcessingPower = processing;
		updateTotal();
		requiredProcessingIsMinimum = isMinimum;
	}

	public AlterableResourceConstraints(final ResourceConstraints toCopy) {
		this(toCopy.getRequiredCPUs(), toCopy.getRequiredProcessingPower(),
				toCopy.isRequiredProcessingIsMinimum(), toCopy
						.getRequiredMemory());
	}

	public static AlterableResourceConstraints getNoResources() {
		return new AlterableResourceConstraints(ConstantConstraints.noResources);
	}

	public void multiply(final double times) {
		if (times != 1) {
			requiredCPUs *= times;
			requiredMemory *= times;
			updateTotal();
		}
	}

	public void add(final ResourceConstraints... toAdd) {
		for (int i = 0; i < toAdd.length; i++) {
			requiredCPUs += toAdd[i].getRequiredCPUs();
			requiredProcessingPower = toAdd[i].getRequiredProcessingPower() < requiredProcessingPower ? requiredProcessingPower
					: toAdd[i].getRequiredProcessingPower();
			requiredMemory += toAdd[i].getRequiredMemory();
		}
		requiredProcessingPower = requiredCPUs == 0 ? 0
				: requiredProcessingPower;
		updateTotal();
	}

	public void subtract(final ResourceConstraints what) {
		requiredCPUs -= what.getRequiredCPUs();
		requiredProcessingPower = requiredCPUs == 0 ? 0
				: requiredProcessingPower < what.getRequiredProcessingPower() ? requiredProcessingPower
						: what.getRequiredProcessingPower();
		requiredMemory -= what.getRequiredMemory();
		updateTotal();
	}

	private void updateTotal() {
		totalProcessingPower = requiredCPUs * requiredProcessingPower;
	}

	@Override
	public double getRequiredCPUs() {
		return requiredCPUs;
	}

	@Override
	public double getRequiredProcessingPower() {
		return requiredProcessingPower;
	}

	@Override
	public boolean isRequiredProcessingIsMinimum() {
		return requiredProcessingIsMinimum;
	}

	@Override
	public long getRequiredMemory() {
		return requiredMemory;
	}

	@Override
	public double getTotalProcessingPower() {
		return totalProcessingPower;
	}
}
