package hu.mta.sztaki.lpds.cloud.simulator.iaas.vmscheduling.iaasscheduling;

import hu.mta.sztaki.lpds.cloud.simulator.iaas.IaaSService;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.PhysicalMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VMManager;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.constraints.ResourceConstraints;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.pmscheduling.AlwaysOnMachines;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.vmscheduling.FirstFitScheduler;
import hu.mta.sztaki.lpds.cloud.simulator.io.NetworkNode;
import hu.mta.sztaki.lpds.cloud.simulator.io.Repository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Simon Csaba
 */
public abstract class IaasScheduler extends FirstFitScheduler {

	ArrayList<IaaSService> iaases;

	int vmRequestIndex = 0;
	int pmRegisterIndex = 0;
	int pmDeregisterIndex = 0;
	private int maxNumberOfPMPerIaaS = 100;
	Map<Long, Integer> PMIaaSList = new HashMap<Long, Integer>();
	

	public IaasScheduler(IaaSService parent) {
		super(parent);

		iaases = new ArrayList<IaaSService>();

		try {
			iaases.add(new IaaSService(FirstFitScheduler.class, AlwaysOnMachines.class));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void registerPM(PhysicalMachine pm) {
		pmRegisterIndex = 0;
		if(iaases.size() != 1) {
			int min = iaases.get(0).machines.size();
			for(int i=1; i<iaases.size(); i++) {
				if(iaases.get(i).machines.size() < min) {
					pmRegisterIndex = i;
					min = iaases.get(i).machines.size();
				}
			}
		}
		
		iaases.get(pmRegisterIndex).registerHost(pm);
		PMIaaSList.put(pm.id, pmRegisterIndex);

		if (iaases.get(pmRegisterIndex).machines.size() > maxNumberOfPMPerIaaS) {
			try {
				iaases.add(new IaaSService(FirstFitScheduler.class, AlwaysOnMachines.class));

				reallocatePMs();

			} catch (Exception ex) {
				ex.printStackTrace();
			}

		}
	}

	@Override
	public void deregisterPM(PhysicalMachine pm) {
		try {
			int indexOfIaaS = PMIaaSList.get(pm.id);
			iaases.get(indexOfIaaS).deregisterHost(pm);
			PMIaaSList.remove(pm.id);
			int numberOfIaases = iaases.size();
			if (iaases.get(indexOfIaaS).machines.size() < (numberOfIaases - 1) * maxNumberOfPMPerIaaS / numberOfIaases) {
				reallocatePMs(indexOfIaaS);
				iaases.remove(indexOfIaaS);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void reallocatePMs(int indexOfIaaSToDelete) throws Exception {
		int numberOfIaases = iaases.size();
		int totalNumberOfPMs = 0;

		int i, j;

		for (IaaSService iaas : iaases) {
			totalNumberOfPMs += iaas.machines.size();
		}

		//kisz�molom, hogy melyik iaas-be mennyi g�p kell at eloszt�s ut�n
		int[] numberOfPMsAfterReallocate = new int[numberOfIaases];

		//egyenl� ar�nyban elosztom a g�peket az iaas-k k�z�tt
		int baseNumberOfPMs;
		if (indexOfIaaSToDelete == -1) {
			baseNumberOfPMs = (int) Math.floor(((float) totalNumberOfPMs) / numberOfIaases);
		} else {
			baseNumberOfPMs = (int) Math.floor(((float) totalNumberOfPMs) / (numberOfIaases -1) );
		}

		for (i = 0; i < numberOfPMsAfterReallocate.length; i++) {
			if (i == indexOfIaaSToDelete) {
				numberOfPMsAfterReallocate[i] = 0;
			} else {
				numberOfPMsAfterReallocate[i] = baseNumberOfPMs;
			}
		}

		//a marad�kot sz�tosztom az els� iaas-k k�z�tt
		int remainder;
		if(indexOfIaaSToDelete == -1) {
			remainder = totalNumberOfPMs - baseNumberOfPMs * (numberOfIaases);
		} else {
			remainder = totalNumberOfPMs - baseNumberOfPMs * (numberOfIaases - 1);
		}
		
		for (i = 0; i < remainder; i++) {
			if (i != indexOfIaaSToDelete) {
				numberOfPMsAfterReallocate[i] += 1;
			}
		}

		//�tdob�lom a f�l�s PM-eket egy ideiglenes list�ba
		ArrayList<PhysicalMachine> tempPMList = new ArrayList<PhysicalMachine>(100);
		PhysicalMachine pm;
		int diff;
		for (i = 0; i < numberOfIaases; i++) {
			diff = iaases.get(i).machines.size() - numberOfPMsAfterReallocate[i];
			if (iaases.get(i).machines.size() > numberOfPMsAfterReallocate[i]) {
				for (j = 0; j < diff; j++) {
					pm = iaases.get(i).machines.get(0);
					tempPMList.add(pm);
					iaases.get(i).deregisterHost(pm);
				}
			}
		}

		//a list�b�l odaadom azoknak az IaaS-eknek akiknek kevesebb van mint kellene
		for (i = 0; i < numberOfIaases; i++) {
			if (iaases.get(i).machines.size() < numberOfPMsAfterReallocate[i]) {
				diff = numberOfPMsAfterReallocate[i] - iaases.get(i).machines.size();
				for (j = 0; j < diff; j++) {
					pm = tempPMList.get(tempPMList.size() - 1);
					iaases.get(i).registerHost(pm);
					tempPMList.remove(tempPMList.size() - 1);
					PMIaaSList.put(pm.id, i);
				}
			}
		}

	}

	private void reallocatePMs() throws Exception {
		reallocatePMs(-1);
	}

	@Override
	public void scheduleVMrequest(VirtualMachine[] vms, ResourceConstraints rc,
			Repository vaSource, HashMap<String, Object> schedulingConstraints)
			throws VMManager.VMManagementException {

		try {
			iaases.get(vmRequestIndex).requestVM(vms[0].getVa(), rc, vaSource, vms.length, schedulingConstraints);
			increaseVMRequestIndex();
		} catch (NetworkNode.NetworkException ex) {
			Logger.getLogger(IaasScheduler.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public ArrayList<IaaSService> getIaases() {
		return iaases;
	}

//	@Override
//	public void registerRepository(Repository repo) {
//		iaases[repoIndex].registerRepository(repo);
//		
//	}
	public abstract void increaseVMRequestIndex();

//	public abstract void increaseRegisterPMIndex();
//	public abstract void increaseRepoIndex();
}
