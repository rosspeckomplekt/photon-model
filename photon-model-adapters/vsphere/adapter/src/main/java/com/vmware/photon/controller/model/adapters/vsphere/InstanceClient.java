/*
 * Copyright (c) 2015-2016 VMware, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, without warranties or
 * conditions of any kind, EITHER EXPRESS OR IMPLIED.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.vmware.photon.controller.model.adapters.vsphere;

import static com.vmware.photon.controller.model.ComputeProperties.RESOURCE_GROUP_NAME;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.VM_PATH_FORMAT;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.createCdrom;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.createFloppy;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.createHdd;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.fillInControllerUnitNumber;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.findFreeScsiUnit;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.findFreeUnit;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.getDatastoreFromStoragePolicy;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.getDatastorePathForDisk;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.getDiskMode;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.getDiskProvisioningType;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.getFirstIdeController;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.getFirstScsiController;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.getFirstSioController;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.getPbmProfileSpec;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.getStorageIOAllocationInfo;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.insertCdrom;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.insertFloppy;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.makePathToVmdkFile;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.nextUnitNumber;
import static com.vmware.photon.controller.model.adapters.vsphere.ClientUtils.toKb;
import static com.vmware.photon.controller.model.adapters.vsphere.CustomProperties.DISK_DATASTORE_NAME;
import static com.vmware.photon.controller.model.constants.PhotonModelConstants.STORAGE_REFERENCE;

import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.vmware.photon.controller.model.adapters.vsphere.ProvisionContext.NetworkInterfaceStateWithDetails;
import com.vmware.photon.controller.model.adapters.vsphere.network.NetworkDeviceBackingFactory;
import com.vmware.photon.controller.model.adapters.vsphere.ovf.OvfDeployer;
import com.vmware.photon.controller.model.adapters.vsphere.ovf.OvfParser;
import com.vmware.photon.controller.model.adapters.vsphere.ovf.OvfRetriever;
import com.vmware.photon.controller.model.adapters.vsphere.util.VimNames;
import com.vmware.photon.controller.model.adapters.vsphere.util.VimPath;
import com.vmware.photon.controller.model.adapters.vsphere.util.connection.BaseHelper;
import com.vmware.photon.controller.model.adapters.vsphere.util.connection.Connection;
import com.vmware.photon.controller.model.adapters.vsphere.util.connection.GetMoRef;
import com.vmware.photon.controller.model.adapters.vsphere.util.finders.Element;
import com.vmware.photon.controller.model.adapters.vsphere.util.finders.Finder;
import com.vmware.photon.controller.model.adapters.vsphere.util.finders.FinderException;
import com.vmware.photon.controller.model.adapters.vsphere.vapi.LibraryClient;
import com.vmware.photon.controller.model.adapters.vsphere.vapi.VapiClient;
import com.vmware.photon.controller.model.adapters.vsphere.vapi.VapiConnection;
import com.vmware.photon.controller.model.resources.ComputeService.ComputeState;
import com.vmware.photon.controller.model.resources.DiskService.DiskState.BootConfig.FileEntry;
import com.vmware.photon.controller.model.resources.DiskService.DiskStateExpanded;
import com.vmware.photon.controller.model.resources.DiskService.DiskStatus;
import com.vmware.photon.controller.model.resources.DiskService.DiskType;
import com.vmware.photon.controller.model.resources.ImageService.ImageState;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.ArrayOfVAppPropertyInfo;
import com.vmware.vim25.ArrayOfVirtualDevice;
import com.vmware.vim25.ArrayUpdateOperation;
import com.vmware.vim25.DuplicateName;
import com.vmware.vim25.DuplicateNameFaultMsg;
import com.vmware.vim25.FileAlreadyExists;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidNameFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.MethodFault;
import com.vmware.vim25.OptionValue;
import com.vmware.vim25.OvfNetworkMapping;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.VAppPropertyInfo;
import com.vmware.vim25.VAppPropertySpec;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceBackingInfo;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDeviceConnectInfo;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualDiskSpec;
import com.vmware.vim25.VirtualDiskType;
import com.vmware.vim25.VirtualE1000;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardMacType;
import com.vmware.vim25.VirtualFloppy;
import com.vmware.vim25.VirtualLsiLogicController;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineDefinedProfileSpec;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualMachineGuestOsIdentifier;
import com.vmware.vim25.VirtualMachineRelocateDiskMoveOptions;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.VirtualMachineRelocateSpecDiskLocator;
import com.vmware.vim25.VirtualMachineSnapshotInfo;
import com.vmware.vim25.VirtualPCIController;
import com.vmware.vim25.VirtualSCSIController;
import com.vmware.vim25.VirtualSCSISharing;
import com.vmware.vim25.VmConfigSpec;
import com.vmware.xenon.common.Utils;

/**
 * A simple client for vsphere. Consist of a valid connection and some context. This class does
 * blocking IO but doesn't talk back to xenon. A client operates in the context of a datacenter. If
 * the datacenter cannot be determined at construction time a ClientException is thrown.
 */
public class InstanceClient extends BaseHelper {
    private static final Logger logger = LoggerFactory.getLogger(InstanceClient.class.getName());

    private static final String CLOUD_CONFIG_PROPERTY_USER_DATA = "user-data";
    private static final String COREOS_CLOUD_CONFIG_PROPERTY_USER_DATA = "guestinfo.coreos.config.data";
    private static final String COREOS_CLOUD_CONFIG_PROPERTY_USER_DATA_ENCODING = "guestinfo.coreos.config.data.encoding";
    private static final String CLOUD_CONFIG_BASE64_ENCODING = "base64";

    private static final String CLOUD_CONFIG_PROPERTY_HOSTNAME = "hostname";
    private static final String COREOS_CLOUD_CONFIG_PROPERTY_HOSTNAME = "guestinfo.guestinfo.hostname";

    private static final String CLOUD_CONFIG_PROPERTY_PUBLIC_KEYS = "public-keys";
    private static final String OVF_PROPERTY_ENV = "ovf-env";

    public static final String CLONE_STRATEGY_FULL = "FULL";

    public static final String CLONE_STRATEGY_LINKED = "LINKED";

    private static final Map<String, Lock> lockPerUri = new ConcurrentHashMap<>();
    private static final VirtualMachineGuestOsIdentifier DEFAULT_GUEST_ID = VirtualMachineGuestOsIdentifier.OTHER_GUEST_64;
    private static final String EXTRA_CONFIG_CREATED = "photon-model-created-millis";
    private static final String SNAPSHOT_LIMIT_CONFIG_STRING = "snapshot.maxSnapshots";

    private final GetMoRef get;
    private final Finder finder;
    private final ProvisionContext ctx;
    private DiskStateExpanded bootDisk;
    private List<DiskStateExpanded> imageDisks;
    private List<DiskStateExpanded> dataDisks;
    private List<DiskStateExpanded> externalDisks;
    private ManagedObjectReference vm;
    private ManagedObjectReference datastore;
    private ManagedObjectReference resourcePool;
    private ManagedObjectReference host;

    public InstanceClient(Connection connection, ProvisionContext ctx) throws FinderException {
        super(connection);

        this.ctx = ctx;

        if (ctx.disks != null) {
            this.dataDisks = new ArrayList<>(ctx.disks.size());
            this.imageDisks = new ArrayList<>(ctx.disks.size());
            this.externalDisks = new ArrayList<>(ctx.disks.size());

            ctx.disks.stream().forEach(ds -> {
                if (ds.bootOrder != null) {
                    this.imageDisks.add(ds);
                } else {
                    if (ds.status == DiskStatus.AVAILABLE) {
                        this.externalDisks.add(ds);
                    } else {
                        this.dataDisks.add(ds);
                    }
                }
            });
            this.bootDisk = findBootDisk();
        }

        this.finder = new Finder(connection, this.ctx.datacenterMoRef);

        this.get = new GetMoRef(this.connection);
    }

    public ComputeState createInstanceFromTemplate(ManagedObjectReference template)
            throws Exception {
        ManagedObjectReference vm = cloneVm(template);

        if (vm == null) {
            // vm was created by someone else
            return null;
        }
        // store reference to created vm for further processing
        this.vm = vm;

        customizeAfterClone();

        ComputeState state = new ComputeState();
        state.resourcePoolLink = VimUtils
                .firstNonNull(this.ctx.child.resourcePoolLink, this.ctx.parent.resourcePoolLink);

        return state;
    }

    public ComputeState createInstanceFromSnapshot() throws Exception {
        String message = "";
        if (this.ctx.snapshotMoRef == null) {
            message = String.format("No MoRef found for the specified snapshot %s",
                  this.ctx.child.documentSelfLink);

            logger.error(message);

            this.ctx.fail(new IllegalStateException(message));
        }

        if (this.ctx.referenceComputeMoRef == null) {
            if (this.ctx.snapshotMoRef == null) {
                message = String.format("No MoRef found for the reference compute for linkedclone creation for %s.",
                      this.ctx.child.documentSelfLink);

                logger.error(message);

                this.ctx.fail(new IllegalStateException(message));
            }
        }

        VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
        relocateSpec.setDiskMoveType(VirtualMachineRelocateDiskMoveOptions
                .CREATE_NEW_CHILD_DISK_BACKING.value());

        VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
        cloneSpec.setPowerOn(false);
        cloneSpec.setLocation(relocateSpec);
        cloneSpec.setSnapshot(this.ctx.snapshotMoRef);
        cloneSpec.setTemplate(false);

        ManagedObjectReference folder = getVmFolder();
        String displayName = this.ctx.child.name;

        ManagedObjectReference linkedCloneTask = getVimPort()
                .cloneVMTask(this.ctx.referenceComputeMoRef, folder, displayName, cloneSpec);

        TaskInfo info = waitTaskEnd(linkedCloneTask);

        if (info.getState() == TaskInfoState.ERROR) {
            MethodFault fault = info.getError().getFault();
            if (fault instanceof FileAlreadyExists) {
                // a .vmx file already exists, assume someone won the race to create the vm
                return null;
            } else {
                return VimUtils.rethrow(info.getError());
            }
        }

        ManagedObjectReference clonedVM = (ManagedObjectReference) info.getResult();
        if (clonedVM == null) {
            // vm was created by someone else
            return null;
        }
        // store reference to created vm for further processing
        this.vm = clonedVM;

        customizeAfterClone();

        ComputeState state = new ComputeState();
        state.resourcePoolLink = VimUtils
              .firstNonNull(this.ctx.child.resourcePoolLink, this.ctx.parent.resourcePoolLink);

        return state;
    }

    private void customizeAfterClone() throws Exception {
        VirtualMachineConfigSpec spec = new VirtualMachineConfigSpec();

        // even though this is a clone, hw config from the compute resource
        // is takes precedence
        if (this.ctx.child.description.cpuCount > 0) {
            spec.setNumCPUs((int) this.ctx.child.description.cpuCount);
        }

        String gt = CustomProperties.of(this.ctx.child).getString(CustomProperties.GUEST_ID, null);
        if (gt != null) {
            spec.setGuestId(gt);
        }

        if (this.ctx.child.description.totalMemoryBytes > 0) {
            spec.setMemoryMB(toMemoryMb(this.ctx.child.description.totalMemoryBytes));
        }
        recordTimestamp(spec.getExtraConfig());

        // set ovf environment
        ArrayOfVAppPropertyInfo infos = this.get.entityProp(this.vm,
                VimPath.vm_config_vAppConfig_property);
        populateCloudConfig(spec, infos);

        // remove nics and attach to proper networks if nics are configured
        ArrayOfVirtualDevice devices = null;
        if (this.ctx.nics != null && this.ctx.nics.size() > 0) {
            devices = this.get.entityProp(this.vm, VimPath.vm_config_hardware_device);
            devices.getVirtualDevice().stream()
                    .filter(d -> d instanceof VirtualEthernetCard)
                    .forEach(nic -> {
                        VirtualDeviceConfigSpec removeNicChange = new VirtualDeviceConfigSpec();
                        removeNicChange.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);
                        removeNicChange.setDevice(nic);
                        spec.getDeviceChange().add(removeNicChange);
                    });

            for (NetworkInterfaceStateWithDetails niState : this.ctx.nics) {
                VirtualDevice nic = createNic(niState, null);
                addDeviceToVm(spec, nic);
            }
        }

        // Find whether it has HDD disk
        if (this.imageDisks != null && !this.imageDisks.isEmpty()) {
            // If there are nics, then devices would have already retrieved, so that it
            // can avoid one more network call.
            // Iterate over each of the VirtualDisk and reconfigure if needed
            if (devices == null) {
                devices = this.get.entityProp(this.vm, VimPath.vm_config_hardware_device);
            }
            spec.getDeviceChange().addAll(getCustomizationConfigSpecs(devices, this.imageDisks));
        }

        // set the maximum snapshot limit if specified
        final String snapshotLimit = CustomProperties.of(this.ctx.child).getString(CustomProperties.SNAPSHOT_MAXIMUM_LIMIT);
        recordSnapshotLimit(spec.getExtraConfig(), snapshotLimit);

        ManagedObjectReference task = getVimPort().reconfigVMTask(this.vm, spec);
        TaskInfo info = waitTaskEnd(task);

        if (info.getState() == TaskInfoState.ERROR) {
            VimUtils.rethrow(info.getError());
        }

        // If there are any data disks then attach then to the VM
        if (this.dataDisks != null && !this.dataDisks.isEmpty()) {
            attachDisks(this.dataDisks, false);
        }

        // If there are any external disks, then attach then to the VM
        attachExternalDisks();
    }

    /**
     * Get customization config spec for all the image disks if any
     */
    private List<VirtualDeviceConfigSpec> getCustomizationConfigSpecs(ArrayOfVirtualDevice devices,
            List<DiskStateExpanded> diskStates)
            throws FinderException, InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        List<VirtualDeviceConfigSpec> specs = new ArrayList<>();
        List<VirtualDisk> virtualDisks = devices.getVirtualDevice().stream()
                .filter(d -> d instanceof VirtualDisk)
                .map(d -> (VirtualDisk) d)
                .collect(Collectors.toList());

        for (VirtualDisk vd : virtualDisks) {
            VirtualDeviceConfigSpec diskSpec = getBootDiskCustomizeConfigSpec(
                    findMatchingImageDiskState(vd, diskStates), vd);
            if (diskSpec != null) {
                specs.add(diskSpec);
            }
        }
        return specs;
    }

    /**
     * Find a matching DiskState object for the virtual disk
     */
    private DiskStateExpanded findMatchingImageDiskState(VirtualDisk virtualDisk,
            List<DiskStateExpanded> diskStates) {
        if (diskStates == null || diskStates.isEmpty()) {
            return null;
        }
        // Filter the disk state that matches the virtual disk scsi number. If not then default
        // it to the properties of the boot disk
        return diskStates.stream().filter(ds -> ds.bootOrder != null &&
                (ds.bootOrder - 1) == virtualDisk.getUnitNumber()).findFirst().orElse(this.bootDisk);
    }

    /**
     * Stores a timestamp into a VM's extraConfig on provisioning.
     * Currently used for resource cleanup only.
     */
    private void recordTimestamp(List<OptionValue> extraConfig) {
        if (extraConfig == null) {
            return;
        }

        OptionValue ov = new OptionValue();
        ov.setKey(EXTRA_CONFIG_CREATED);
        ov.setValue(Long.toString(System.currentTimeMillis()));
        extraConfig.add(ov);
    }

    private void recordSnapshotLimit(List<OptionValue> extraConfig, String snapshotLimitValue) {
        if (snapshotLimitValue != null && !snapshotLimitValue.isEmpty()) {
            if (extraConfig == null) {
                extraConfig = new ArrayList<>();
            }
            extraConfig.add(populateSnapshotLimitValue(snapshotLimitValue));
        }
    }

    private OptionValue populateSnapshotLimitValue(String snapshotLimitValue) {
        OptionValue ov = new OptionValue();
        ov.setKey(SNAPSHOT_LIMIT_CONFIG_STRING);
        ov.setValue(snapshotLimitValue);
        return ov;
    }

    private ManagedObjectReference cloneVm(ManagedObjectReference template) throws Exception {
        ManagedObjectReference folder = getVmFolder();
        List<VirtualMachineDefinedProfileSpec> pbmSpec = getPbmProfileSpec(this.bootDisk);
        ManagedObjectReference datastore = getDataStoreForDisk(this.bootDisk, pbmSpec);
        ManagedObjectReference resourcePool = getResourcePool();

        Map<String, Object> props = this.get.entityProps(template, VimPath.vm_config_hardware_device);

        ArrayOfVirtualDevice devices = (ArrayOfVirtualDevice) props
                .get(VimPath.vm_config_hardware_device);

        VirtualDisk vd = devices.getVirtualDevice().stream()
                .filter(d -> d instanceof VirtualDisk)
                .map(d -> (VirtualDisk) d).findFirst().orElse(null);

        VirtualMachineRelocateSpec relocSpec = new VirtualMachineRelocateSpec();
        relocSpec.setDatastore(datastore);
        if (pbmSpec != null) {
            pbmSpec.stream().forEach(spec -> {
                relocSpec.getProfile().add(spec);
            });
        }
        relocSpec.setFolder(folder);
        relocSpec.setPool(resourcePool);
        relocSpec.setDiskMoveType(computeDiskMoveType().value());

        VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
        cloneSpec.setLocation(relocSpec);

        //Set the provisioning type of the parent disk.
        VirtualMachineRelocateSpecDiskLocator diskProvisionTypeLocator = setProvisioningType(vd, datastore,
                pbmSpec);
        if (diskProvisionTypeLocator != null) {
            cloneSpec.getLocation().getDisk().add(diskProvisionTypeLocator);
        }

        cloneSpec.setPowerOn(false);
        cloneSpec.setTemplate(false);

        String displayName = this.ctx.child.name;

        ManagedObjectReference cloneTask = getVimPort()
                .cloneVMTask(template, folder, displayName, cloneSpec);

        TaskInfo info = waitTaskEnd(cloneTask);

        if (info.getState() == TaskInfoState.ERROR) {
            MethodFault fault = info.getError().getFault();
            if (fault instanceof FileAlreadyExists) {
                // a .vmx file already exists, assume someone won the race to create the vm
                return null;
            } else {
                return VimUtils.rethrow(info.getError());
            }
        }

        return (ManagedObjectReference) info.getResult();
    }

    private VirtualMachineRelocateDiskMoveOptions computeDiskMoveType() {
        String strategy = CustomProperties.of(this.ctx.child).getString(CustomProperties.CLONE_STRATEGY);

        if (CLONE_STRATEGY_FULL.equals(strategy)) {
            return VirtualMachineRelocateDiskMoveOptions.MOVE_ALL_DISK_BACKINGS_AND_DISALLOW_SHARING;
        } else if (CLONE_STRATEGY_LINKED.equals(strategy)) {
            return VirtualMachineRelocateDiskMoveOptions.CREATE_NEW_CHILD_DISK_BACKING;
        } else {
            logger.warn("Unknown clone strategy {}, defaulting to MOVE_CHILD_MOST_DISK_BACKING", strategy);
            return VirtualMachineRelocateDiskMoveOptions.MOVE_CHILD_MOST_DISK_BACKING;
        }
    }

    public void deleteInstance() throws Exception {
        ManagedObjectReference vm = CustomProperties.of(this.ctx.child)
                .getMoRef(CustomProperties.MOREF);
        if (vm == null) {
            logger.info("No moref associated with the given instance, skipping delete.");
            return;
        }

        TaskInfo info;
        // power off
        ManagedObjectReference task = getVimPort().powerOffVMTask(vm);
        info = waitTaskEnd(task);
        ignoreError("Ignore error powering off VM", info);

        // delete vm
        task = getVimPort().destroyTask(vm);
        info = waitTaskEnd(task);
        ignoreError("Ignore error deleting VM", info);
    }

    private void ignoreError(String s, TaskInfo info) {
        if (info.getState() == TaskInfoState.ERROR) {
            logger.info(s + ": " + info.getError().getLocalizedMessage());
        }
    }

    /**
     * Does provisioning and return a patchable state to patch the resource.
     *
     * @return
     */
    public ComputeState createInstance() throws Exception {
        ManagedObjectReference vm;

        URI ovfUri = getOvfUri();
        if (ovfUri != null) {
            vm = deployOvf(ovfUri);
            this.vm = vm;
        } else {
            vm = createVm();
            if (vm == null) {
                // vm was created by someone else
                return null;
            }
            // store reference to created vm for further processing
            this.vm = vm;

            attachDisks(this.imageDisks, true);
        }
        // If there are any data disks then attach then to the VM
        if (this.dataDisks != null && !this.dataDisks.isEmpty()) {
            attachDisks(this.dataDisks, false);
        }

        // If there are any external disks, then attach then to the VM
        attachExternalDisks();

        ComputeState state = new ComputeState();
        state.resourcePoolLink = VimUtils
                .firstNonNull(this.ctx.child.resourcePoolLink, this.ctx.parent.resourcePoolLink);

        return state;
    }

    private URI getOvfUri() {
        CustomProperties cp = CustomProperties.of(this.ctx.child.description);

        String result = cp.getString(OvfParser.PROP_OVF_URI);
        if (result == null) {
            result = cp.getString(OvfParser.PROP_OVF_ARCHIVE_URI);
        }

        if (result == null) {
            if (this.bootDisk != null && this.bootDisk.sourceImageReference != null
                    && this.bootDisk.sourceImageReference.getScheme() != null) {
                if (this.bootDisk.sourceImageReference.getScheme().startsWith("http")) {
                    result = this.bootDisk.sourceImageReference.toString();
                }
            }
        }

        if (result == null) {
            return null;
        }

        return URI.create(result);
    }

    private ManagedObjectReference deployOvf(URI ovfUri) throws Exception {
        OvfDeployer deployer = new OvfDeployer(this.connection);
        CustomProperties cust = CustomProperties.of(this.ctx.child.description);

        URI archiveUri = cust.getUri(OvfParser.PROP_OVF_ARCHIVE_URI);
        if (archiveUri != null) {
            logger.info("Prefer ova {} uri to ovf {}", archiveUri, ovfUri);
            OvfRetriever retriever = deployer.getRetriever();
            ovfUri = retriever.downloadIfOva(archiveUri);
        }

        ManagedObjectReference folder = getVmFolder();
        List<VirtualMachineDefinedProfileSpec> pbmSpec = getPbmProfileSpec(this.bootDisk);
        ManagedObjectReference ds = getDataStoreForDisk(this.bootDisk, pbmSpec);
        ManagedObjectReference resourcePool = getResourcePool();

        String vmName = "pmt-" + deployer.getRetriever().hash(ovfUri);

        GetMoRef get = new GetMoRef(this.connection);

        ManagedObjectReference vm = findTemplateByName(vmName, get);
        if (vm == null) {
            String config = cust.getString(OvfParser.PROP_OVF_CONFIGURATION);
            Lock lock = getLock(vmName);
            lock.lock();
            try {
                vm = findTemplateByName(vmName, get);
                if (vm == null) {
                    OvfParser parser = new OvfParser();
                    Document ovfDoc = parser.retrieveDescriptor(ovfUri);
                    List<OvfNetworkMapping> networks = mapNetworks(parser.extractNetworks(ovfDoc),
                            ovfDoc, this.ctx.nics);
                    vm = deployer.deployOvf(ovfUri, getHost(), folder, vmName, networks,
                            ds, Collections.emptyList(), config, resourcePool);

                    logger.info("Removing NICs from deployed template: {} ({})", vmName,
                            vm.getValue());
                    ArrayOfVirtualDevice devices = get.entityProp(vm,
                            VimPath.vm_config_hardware_device);
                    if (devices != null) {
                        VirtualMachineConfigSpec reconfig = new VirtualMachineConfigSpec();

                        for (VirtualDevice device : devices.getVirtualDevice()) {
                            if (device instanceof VirtualEthernetCard) {
                                VirtualDeviceConfigSpec spec = new VirtualDeviceConfigSpec();
                                spec.setDevice(device);
                                spec.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);
                                reconfig.getDeviceChange().add(spec);
                            }
                        }
                        ManagedObjectReference reconfigTask = getVimPort()
                                .reconfigVMTask(vm, reconfig);
                        VimUtils.waitTaskEnd(this.connection, reconfigTask);
                    }
                    ManagedObjectReference snapshotTask = getVimPort()
                            .createSnapshotTask(vm, "initial",
                                    null, false, false);
                    VimUtils.waitTaskEnd(this.connection, snapshotTask);
                }
            } catch (Exception e) {
                logger.warn("Error deploying Ovf for template [" + vmName + "],reason:", e);
                vm = awaitVM(vmName, folder, get);
            } finally {
                lock.unlock();
            }
        }

        if (!isSameDatastore(ds, vm, get)) {
            // make sure the original VM template is ready
            Object snapshot = get.entityProp(vm, VimPath.vm_snapshot);
            if (snapshot == null) {
                vm = awaitVM(vmName, folder, get);
            }
            vm = replicateVMTemplate(resourcePool, ds, pbmSpec, folder, vmName, vm, get);
        }

        return cloneOvfBasedTemplate(vm, ds, folder, resourcePool, pbmSpec);
    }

    private List<OvfNetworkMapping> mapNetworks(List<String> ovfNetworkNames, Document ovfDoc,
            List<NetworkInterfaceStateWithDetails> nics)
            throws FinderException, InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        List<OvfNetworkMapping> networks = new ArrayList<>();

        if (ovfNetworkNames.isEmpty() || nics.isEmpty()) {
            return networks;
        }

        CustomProperties custProp;
        ManagedObjectReference moRef;
        NetworkInterfaceStateWithDetails nic = nics.iterator().next();
        if (nic.subnet != null) {
            custProp = CustomProperties.of(nic.subnet);
        } else {
            custProp = CustomProperties.of(nic.network);
        }
        moRef = custProp.getMoRef(CustomProperties.MOREF);

        if (moRef == null) {
            moRef = this.finder.networkList("*").iterator().next().object;
        }

        final ManagedObjectReference finalMoRef = moRef;
        ovfNetworkNames.forEach(n -> {
            OvfNetworkMapping nm = new OvfNetworkMapping();
            nm.setName(n);
            nm.setNetwork(finalMoRef);
            networks.add(nm);
        });
        return networks;
    }

    private ManagedObjectReference replicateVMTemplate(ManagedObjectReference resourcePool,
            ManagedObjectReference datastore, List<VirtualMachineDefinedProfileSpec> pbmSpec,
            ManagedObjectReference vmFolder, String vmName,
            ManagedObjectReference vm, GetMoRef get) throws Exception {
        logger.info("Template lives on a different datastore, looking for a local copy of: {}.",
                vmName);

        String replicatedName = vmName;
        if (datastore != null) {
            replicatedName = replicatedName + "_" + datastore.getValue();
        }
        ManagedObjectReference repVm = findTemplateByName(replicatedName, get);
        if (repVm != null) {
            return repVm;
        }

        logger.info("Replicating {} ({}) to {}", vmName, vm.getValue(), replicatedName);
        Lock lock = getLock(replicatedName);
        lock.lock();
        try {
            VirtualMachineRelocateSpec spec = new VirtualMachineRelocateSpec();
            spec.setPool(resourcePool);
            if (datastore != null) {
                spec.setDatastore(datastore);
            }
            if (pbmSpec != null) {
                pbmSpec.stream().forEach(sp -> {
                    spec.getProfile().add(sp);
                });
            }
            spec.setFolder(vmFolder);
            spec.setDiskMoveType(
                    VirtualMachineRelocateDiskMoveOptions.MOVE_ALL_DISK_BACKINGS_AND_DISALLOW_SHARING
                            .value());

            VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
            cloneSpec.setLocation(spec);
            cloneSpec.setTemplate(false);
            cloneSpec.setPowerOn(false);
            ManagedObjectReference cloneTask = getVimPort()
                    .cloneVMTask(vm, vmFolder, replicatedName, cloneSpec);

            TaskInfo info = VimUtils.waitTaskEnd(this.connection, cloneTask);

            if (info.getState() == TaskInfoState.ERROR) {
                MethodFault fault = info.getError().getFault();
                if (fault instanceof DuplicateName) {
                    logger.info(
                            "Template is being replicated by another thread, waiting for {} to be ready",
                            replicatedName);
                    return awaitVM(replicatedName, vmFolder, get);
                } else {
                    return VimUtils.rethrow(info.getError());
                }
            }

            ManagedObjectReference rvm = (ManagedObjectReference) info.getResult();
            logger.info("Replicated {} ({}) to {} ({})", vmName, vm.getValue(), replicatedName,
                    rvm.getValue());
            logger.info("Creating initial snapshot for linked clones on {}", rvm.getValue());
            ManagedObjectReference snapshotTask = getVimPort().createSnapshotTask(rvm, "initial",
                    null, false, false);
            VimUtils.waitTaskEnd(this.connection, snapshotTask);
            logger.info("Created initial snapshot for linked clones on {}", rvm.getValue());
            return rvm;
        } finally {
            lock.unlock();
        }
    }

    private ManagedObjectReference awaitVM(String replicatedName, ManagedObjectReference vmFolder,
            GetMoRef get)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, FinderException {

        Element element = this.finder.fullPath(vmFolder);
        String path = element.path;

        if (path.endsWith("/")) {
            path = path + replicatedName;
        } else {
            path = path + "/" + replicatedName;
        }

        // remove the datacenters folder from path, as findByInventoryPath is using relative to it
        // paths
        if (path.startsWith("/Datacenters")) {
            path = path.substring("/Datacenters".length());
        }

        logger.info("Searching for vm using InventoryPath {}", path);
        ManagedObjectReference reference = getVimPort()
                .findByInventoryPath(getServiceContent().getSearchIndex(), path);

        Object snapshot = get.entityProp(reference, VimPath.vm_snapshot);
        if (snapshot == null) {
            int retryCount = 30;
            while (retryCount > 0) {
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    return null;
                }
                snapshot = get.entityProp(reference, VimPath.vm_snapshot);
                if (snapshot != null) {
                    return reference;
                }
            }
        }

        return reference;
    }

    private ManagedObjectReference findTemplateByName(String vmName, GetMoRef get) {
        try {
            return get.vmByVMname(vmName,
                    this.connection.getServiceContent().getPropertyCollector());
        } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e) {
            logger.debug("Error finding template vm[" + vmName + "]", e);
            return null;
        }
    }

    private boolean isSameDatastore(ManagedObjectReference datastore, ManagedObjectReference vm,
            GetMoRef get) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        if (datastore == null) {
            return false;
        }
        ArrayOfManagedObjectReference datastores = get.entityProp(vm,
                VimPath.vm_datastore);
        if (null != datastores) {
            for (ManagedObjectReference p : datastores.getManagedObjectReference()) {
                if (p.getValue().equals(datastore.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    private ManagedObjectReference cloneOvfBasedTemplate(ManagedObjectReference vmTempl,
            ManagedObjectReference datastore, ManagedObjectReference folder,
            ManagedObjectReference resourcePool, List<VirtualMachineDefinedProfileSpec> pbmSpec)
            throws Exception {

        String vmName = this.ctx.child.name;

        Map<String, Object> props = this.get.entityProps(vmTempl, VimPath.vm_summary_config_numCpu,
                VimPath.vm_summary_config_memorySizeMB, VimPath.vm_snapshot,
                VimPath.vm_config_hardware_device, VimPath.vm_config_vAppConfig_property);

        VirtualMachineSnapshotInfo snapshot = (VirtualMachineSnapshotInfo) props
                .get(VimPath.vm_snapshot);
        ArrayOfVirtualDevice devices = (ArrayOfVirtualDevice) props
                .get(VimPath.vm_config_hardware_device);

        VirtualDisk vd = devices.getVirtualDevice().stream()
                .filter(d -> d instanceof VirtualDisk)
                .map(d -> (VirtualDisk) d).findFirst().orElse(null);

        VirtualSCSIController scsiController = getFirstScsiController(devices);
        Integer[] scsiUnit = findFreeScsiUnit(scsiController, devices.getVirtualDevice());

        VirtualMachineRelocateDiskMoveOptions diskMoveOption = computeDiskMoveType();
        boolean customizeImageDisk = false;
        List<VirtualDeviceConfigSpec> newDisks = new ArrayList<>();
        VirtualMachineRelocateSpecDiskLocator bootDiskLocator = null;

        List<VirtualDisk> vDisks = null;
        if (this.bootDisk != null) {
            if (vd == null) {
                String datastoreName = this.get.entityProp(datastore, VimPath.ds_summary_name);
                String path = makePathToVmdkFile("ephemeral_disk", vmName);
                String diskName = String.format(VM_PATH_FORMAT, datastoreName, path);
                VirtualDeviceConfigSpec hdd = createHdd(scsiController.getKey(), scsiUnit[0], this.bootDisk,
                        diskName, datastore, pbmSpec);
                newDisks.add(hdd);
            } else {
                // if any one of the image disk is requesting for resize, then change the clone
                // strategy
                if (this.imageDisks != null && !this.imageDisks.isEmpty()) {
                    vDisks = devices.getVirtualDevice().stream()
                            .filter(d -> d instanceof VirtualDisk)
                            .map(d -> (VirtualDisk) d)
                            .filter(d -> {
                                DiskStateExpanded ds = findMatchingImageDiskState(d, this.imageDisks);
                                return toKb(ds.capacityMBytes) > d.getCapacityInKB() || ds.customProperties != null;
                            }).collect(Collectors.toList());
                    if (vDisks.size() > 0) {
                        diskMoveOption = VirtualMachineRelocateDiskMoveOptions.MOVE_ALL_DISK_BACKINGS_AND_DISALLOW_SHARING;
                        logger.warn(
                                "Changing clone strategy to MOVE_ALL_DISK_BACKINGS_AND_DISALLOW_SHARING, as there is disk resize requested");
                        customizeImageDisk = true;
                        bootDiskLocator = setProvisioningType(vDisks.get(0), datastore, pbmSpec);
                    }
                }
            }
        }

        VirtualCdrom vcd = devices.getVirtualDevice().stream()
                .filter(d -> d instanceof VirtualCdrom)
                .map(d -> (VirtualCdrom) d).findFirst().orElse(null);

        // add a cdrom so that ovf transport works
        if (vcd == null) {
            VirtualDevice ideController = getFirstIdeController(devices);
            int ideUnit = findFreeUnit(ideController, devices.getVirtualDevice());
            VirtualDeviceConfigSpec cdrom = createCdrom(ideController, ideUnit);
            newDisks.add(cdrom);
        } else {
            VirtualDeviceConfigSpec cdrom = reconfigureCdrom(vcd);
            newDisks.add(cdrom);
        }

        VirtualMachineConfigSpec spec = new VirtualMachineConfigSpec();

        // even though this is a clone, hw config from the compute resource
        // is takes precedence
        spec.setNumCPUs((int) this.ctx.child.description.cpuCount);
        spec.setMemoryMB(toMemoryMb(this.ctx.child.description.totalMemoryBytes));
        String gt = CustomProperties.of(this.ctx.child).getString(CustomProperties.GUEST_ID, null);
        if (gt != null) {
            spec.setGuestId(gt);
        }

        // set ovf environment
        ArrayOfVAppPropertyInfo infos = (ArrayOfVAppPropertyInfo) props
                .get(VimPath.vm_config_vAppConfig_property);// this.get.entityProp(vmTempl,
        // VimPath.vm_config_vAppConfig_property);
        populateVAppProperties(spec, infos);
        populateCloudConfig(spec, infos);
        recordTimestamp(spec.getExtraConfig());
        // set the maximum snapshot limit if specified
        final String snapshotLimit = CustomProperties.of(this.ctx.child).getString(CustomProperties.SNAPSHOT_MAXIMUM_LIMIT);
        recordSnapshotLimit(spec.getExtraConfig(), snapshotLimit);
        // add disks one at a time
        for (VirtualDeviceConfigSpec newDisk : newDisks) {
            spec.getDeviceChange().add(newDisk);
        }

        // configure network
        VirtualPCIController pci = getFirstPciController(devices);
        for (NetworkInterfaceStateWithDetails nicWithDetails : this.ctx.nics) {
            VirtualDevice nic = createNic(nicWithDetails, pci.getControllerKey());
            addDeviceToVm(spec, nic);
        }

        // remove any networks from the template
        devices.getVirtualDevice().stream()
                .filter(d -> VirtualEthernetCard.class.isAssignableFrom(d.getClass()))
                .forEach(d -> addRemoveDeviceFromVm(spec, d));

        VirtualMachineRelocateSpec relocSpec = new VirtualMachineRelocateSpec();
        if (pbmSpec != null) {
            pbmSpec.stream().forEach(sp -> {
                relocSpec.getProfile().add(sp);
            });
        }
        relocSpec.setDatastore(datastore);
        relocSpec.setFolder(folder);
        relocSpec.setPool(resourcePool);
        relocSpec.setDiskMoveType(diskMoveOption.value());

        VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
        cloneSpec.setLocation(relocSpec);
        cloneSpec.setPowerOn(false);
        cloneSpec.setTemplate(false);
        cloneSpec.setSnapshot(snapshot.getCurrentSnapshot());
        cloneSpec.setConfig(spec);

        if (bootDiskLocator != null) {
            cloneSpec.getLocation().getDisk().add(bootDiskLocator);
        }

        ManagedObjectReference cloneTask = getVimPort().cloneVMTask(vmTempl, folder, vmName,
                cloneSpec);
        TaskInfo info = waitTaskEnd(cloneTask);

        if (info.getState() == TaskInfoState.ERROR) {
            return VimUtils.rethrow(info.getError());
        }

        ManagedObjectReference vmMoref = (ManagedObjectReference) info.getResult();
        // Apply boot disk customization if any, if done through full clone.
        if (customizeImageDisk) {
            ArrayOfVirtualDevice virtualDevices = this.get.entityProp(vmMoref, VimPath
                    .vm_config_hardware_device);
            reconfigureBootDisk(vmMoref, getCustomizationConfigSpecs(virtualDevices, this.imageDisks));
        }

        return vmMoref;
    }

    /**
     * The first HDD disk is considered the boot disk.
     *
     * @return
     */
    private DiskStateExpanded findBootDisk() {
        if (this.ctx.disks == null || this.ctx.disks.isEmpty()) {
            return null;
        }

        return this.ctx.disks.stream()
                .filter(d -> d.type == DiskType.HDD && d.bootOrder != null && d.bootOrder == 1)
                .findFirst()
                .orElse(null);
    }

    /**
     * Attach any externally created disks to the VM if any.
     */
    public void attachExternalDisks() throws Exception {
        if (this.externalDisks == null || this.externalDisks.isEmpty()) {
            return;
        }

        if (this.vm == null) {
            throw new IllegalStateException("Cannot attach diskStates if VM is not created");
        }

        for (DiskStateExpanded diskState: this.externalDisks) {
            ArrayOfVirtualDevice devices = this.get
                    .entityProp(this.vm, VimPath.vm_config_hardware_device);
            String diskDatastoreName = CustomProperties.of(diskState).getString(DISK_DATASTORE_NAME);

            ClientUtils.attachDiskToVM(devices, this.vm, diskState,
                    this.finder.datastore(diskDatastoreName).object, this.connection,
                    this.getVimPort());
        }
    }

    /**
     * Creates disks and attaches them to the vm created by {@link #createInstance()}. The given
     * diskStates are enriched with data from vSphere and can be patched back to xenon.
     */
    public void attachDisks(List<DiskStateExpanded> diskStates, boolean isImageDisks) throws
            Exception {
        if (this.vm == null) {
            throw new IllegalStateException("Cannot attach diskStates if VM is not created");
        }

        EnumSet<DiskType> notSupportedTypes = EnumSet.of(DiskType.SSD, DiskType.NETWORK);
        List<DiskStateExpanded> unsupportedDisks = diskStates.stream()
                .filter(d -> notSupportedTypes.contains(d.type))
                .collect(Collectors.toList());
        if (!unsupportedDisks.isEmpty()) {
            throw new IllegalStateException(
                    "Some diskStates cannot be created: " + unsupportedDisks.stream()
                            .map(d -> d.documentSelfLink).collect(Collectors.toList()));
        }

        // the path to folder holding all vm files
        String dir = this.get.entityProp(this.vm, VimPath.vm_config_files_vmPathName);
        dir = Paths.get(dir).getParent().toString();

        ArrayOfVirtualDevice devices = this.get
                .entityProp(this.vm, VimPath.vm_config_hardware_device);

        VirtualSCSIController scsiController = getFirstScsiController(devices);
        // Get available free unit numbers for the given scsi controller.
        Integer[] scsiUnits = findFreeScsiUnit(scsiController, devices.getVirtualDevice());

        VirtualDevice ideController = getFirstIdeController(devices);
        int ideUnit = findFreeUnit(ideController, devices.getVirtualDevice());

        VirtualDevice sioController = getFirstSioController(devices);
        int sioUnit = findFreeUnit(sioController, devices.getVirtualDevice());

        List<VirtualDeviceConfigSpec> newDisks = new ArrayList<>();

        boolean cdromAdded = false;
        List<DiskStateExpanded> disksToBeCustomized = null;

        int scsiUnitIndex = 0;
        for (DiskStateExpanded ds : diskStates) {
            String diskPath = VimUtils.uriToDatastorePath(ds.sourceImageReference);

            if (ds.type == DiskType.HDD) {
                // Find if there is a storage policy defined for this disk
                List<VirtualMachineDefinedProfileSpec> pbmSpec = getPbmProfileSpec(ds);
                VirtualDeviceConfigSpec hdd;
                if (diskPath != null) {
                    // create full clone of given disk
                    hdd = createFullCloneAndAttach(diskPath, ds, dir, scsiController,
                            scsiUnits[scsiUnitIndex], pbmSpec);
                    newDisks.add(hdd);

                    // When it is through clone, customize after the clone is complete.
                    if (disksToBeCustomized == null) {
                        disksToBeCustomized = new ArrayList<>(diskStates.size());
                    }
                    if (isImageDisks) {
                        disksToBeCustomized.add(ds);
                    }
                } else {
                    String dsDirForDisk = getDatastorePathForDisk(ds, dir);
                    String diskName = makePathToVmdkFile(ds.name, dsDirForDisk);
                    hdd = createHdd(scsiController.getKey(), scsiUnits[scsiUnitIndex], ds, diskName,
                            getDataStoreForDisk(ds, pbmSpec), pbmSpec);
                    newDisks.add(hdd);
                }
                scsiUnitIndex++;
            }
            if (ds.type == DiskType.CDROM) {
                VirtualDeviceConfigSpec cdrom = createCdrom(ideController, ideUnit);
                fillInControllerUnitNumber(ds, ideUnit);
                ideUnit = nextUnitNumber(ideUnit);
                if (diskPath != null) {
                    // mount iso image
                    insertCdrom((VirtualCdrom) cdrom.getDevice(), diskPath);
                }
                newDisks.add(cdrom);
                cdromAdded = true;
            }
            if (ds.type == DiskType.FLOPPY) {
                VirtualDeviceConfigSpec floppy = createFloppy(sioController, sioUnit);
                fillInControllerUnitNumber(ds, sioUnit);
                sioUnit = nextUnitNumber(sioUnit);
                if (diskPath != null) {
                    // mount iso image
                    insertFloppy((VirtualFloppy) floppy.getDevice(), diskPath);
                }
                newDisks.add(floppy);
            }

            // mark disk as attached
            ds.status = DiskStatus.ATTACHED;
        }

        // add a cdrom so that ovf transport works
        if (!cdromAdded && isImageDisks) {
            VirtualDeviceConfigSpec cdrom = createCdrom(ideController, ideUnit);
            newDisks.add(cdrom);
        }

        // add disks one at a time
        for (VirtualDeviceConfigSpec newDisk : newDisks) {
            VirtualMachineConfigSpec spec = new VirtualMachineConfigSpec();
            spec.getDeviceChange().add(newDisk);

            ManagedObjectReference reconfigureTask = getVimPort().reconfigVMTask(this.vm, spec);
            TaskInfo info = waitTaskEnd(reconfigureTask);
            if (info.getState() == TaskInfoState.ERROR) {
                VimUtils.rethrow(info.getError());
            }
        }

        // If disks are created through full clone, then reconfigure
        if (disksToBeCustomized != null && !disksToBeCustomized.isEmpty()) {
            // Get the hardware devices once again as they are reconfigured
            devices = this.get.entityProp(this.vm, VimPath.vm_config_hardware_device);
            reconfigureBootDisk(this.vm, getCustomizationConfigSpecs(devices, disksToBeCustomized));
        }
    }

    /**
     * Reconfigure image disk with the customizations
     */
    private void reconfigureBootDisk(ManagedObjectReference vmMoref,
            List<VirtualDeviceConfigSpec> deviceConfigSpecs) throws Exception {
        if (deviceConfigSpecs != null && !deviceConfigSpecs.isEmpty()) {
            VirtualMachineConfigSpec bootDiskSpec = new VirtualMachineConfigSpec();
            bootDiskSpec.getDeviceChange().addAll(deviceConfigSpecs);
            ManagedObjectReference task = getVimPort().reconfigVMTask(vmMoref, bootDiskSpec);
            TaskInfo info = waitTaskEnd(task);

            if (info.getState() == TaskInfoState.ERROR) {
                VimUtils.rethrow(info.getError());
            }
        }
    }

    /**
     * Construct VM config spec for boot disk size customization, if the user defined value is
     * greater then the existing size of the disk
     */
    private VirtualDeviceConfigSpec getBootDiskCustomizeConfigSpec(DiskStateExpanded disk, VirtualDisk virtualDisk)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, FinderException {
        if (disk != null && virtualDisk != null) {
            // Resize happens if the new size is more than the existing disk size, other storage
            // related attributes will be applied the disk.
            VirtualDeviceConfigSpec hdd = resizeHdd(virtualDisk, disk);
            return hdd;
        }
        return null;
    }

    private TaskInfo waitTaskEnd(ManagedObjectReference task)
            throws InvalidCollectorVersionFaultMsg, InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        return VimUtils.waitTaskEnd(this.connection, task);
    }

    private VirtualDeviceConfigSpec createFullCloneAndAttach(String sourcePath, DiskStateExpanded ds,
            String dir, VirtualDevice scsiController, int unitNumber,
            List<VirtualMachineDefinedProfileSpec> pbmSpec)
            throws Exception {

        ManagedObjectReference diskManager = this.connection.getServiceContent()
                .getVirtualDiskManager();

        String dsDirForDisk = getDatastorePathForDisk(ds, dir);
        // put full clone in the vm folder
        String destName = makePathToVmdkFile(ds.name, dsDirForDisk);

        // all ops are within a datacenter
        ManagedObjectReference sourceDc = this.ctx.datacenterMoRef;
        ManagedObjectReference destDc = sourceDc;

        Boolean force = true;

        // spec is not supported, should use null for now
        VirtualDiskSpec spec = null;

        ManagedObjectReference task = getVimPort()
                .copyVirtualDiskTask(diskManager, sourcePath, sourceDc, destName, destDc, spec,
                        force);

        // wait for the disk to be copied
        TaskInfo taskInfo = waitTaskEnd(task);
        if (taskInfo.getState() == TaskInfoState.ERROR) {
            return VimUtils.rethrow(taskInfo.getError());
        }

        VirtualDiskFlatVer2BackingInfo backing = new VirtualDiskFlatVer2BackingInfo();
        backing.setDiskMode(getDiskMode(ds));
        VirtualDiskType provisionType = getDiskProvisioningType(ds);
        if (provisionType != null) {
            backing.setThinProvisioned(provisionType == VirtualDiskType.THIN);
            backing.setEagerlyScrub(provisionType == VirtualDiskType.EAGER_ZEROED_THICK);
        }
        backing.setFileName(destName);
        backing.setDatastore(getDataStoreForDisk(ds, pbmSpec));

        VirtualDisk disk = new VirtualDisk();
        disk.setBacking(backing);
        disk.setStorageIOAllocation(getStorageIOAllocationInfo(ds));
        disk.setControllerKey(scsiController.getKey());
        disk.setUnitNumber(unitNumber);
        fillInControllerUnitNumber(ds, unitNumber);
        disk.setKey(-1);

        VirtualDeviceConfigSpec change = new VirtualDeviceConfigSpec();
        change.setDevice(disk);
        // Add storage policy spec
        if (pbmSpec != null) {
            pbmSpec.stream().forEach(sp -> {
                change.getProfile().add(sp);
            });
        }
        change.setOperation(VirtualDeviceConfigSpecOperation.ADD);

        return change;
    }

    private VirtualMachineRelocateSpecDiskLocator setProvisioningType(VirtualDisk vDisk,
            ManagedObjectReference datastore, List<VirtualMachineDefinedProfileSpec> pbmSpec)
          throws InvalidPropertyFaultMsg, FinderException, RuntimeFaultFaultMsg {

        if (vDisk == null) {
            return null;
        }

        // If datastore for disk is null, if storage policy is configured pick the compatible
        // datastore from that.
        if (datastore == null) {
            ManagedObjectReference dsFromSp = getDatastoreFromStoragePolicy(this.connection,
                    pbmSpec);
            datastore = dsFromSp == null ? getDatastore() : dsFromSp;
        }

        VirtualDiskFlatVer2BackingInfo flatBacking = (VirtualDiskFlatVer2BackingInfo) vDisk.getBacking();

        VirtualDiskType provisioningType = getDiskProvisioningType(this.bootDisk);

        boolean wasThinProvision = flatBacking.isThinProvisioned();
        Boolean wasEagerScrubbed = flatBacking.isEagerlyScrub() != null ?
                flatBacking.isEagerlyScrub() : false;

        if (provisioningType != null) {
            flatBacking.setThinProvisioned(provisioningType == VirtualDiskType.THIN);
            flatBacking.setEagerlyScrub(provisioningType == VirtualDiskType.EAGER_ZEROED_THICK);
        }

        VirtualMachineRelocateSpecDiskLocator diskLocator = new VirtualMachineRelocateSpecDiskLocator();
        diskLocator.setDiskId(vDisk.getKey());
        diskLocator.setDiskBackingInfo(flatBacking);
        diskLocator.setDatastore(datastore);

        Boolean isEagerScrub = flatBacking.isEagerlyScrub() != null ?
                flatBacking.isEagerlyScrub() : false;

        //If there is a change from thin to thick or vice-versa then we need to change the DiskMoveType
        //to MOVE_ALL_DISK_BACKINGS_AND_DISALLOW_SHARING
        if (wasThinProvision != flatBacking.isThinProvisioned() || !wasEagerScrubbed.equals(isEagerScrub)) {
            diskLocator.setDiskMoveType(VirtualMachineRelocateDiskMoveOptions
                    .MOVE_ALL_DISK_BACKINGS_AND_DISALLOW_SHARING.value());
        }

        return diskLocator;
    }

    private VirtualDeviceConfigSpec reconfigureCdrom(VirtualCdrom vcd) {
        VirtualCdrom cdrom = new VirtualCdrom();

        cdrom.setControllerKey(vcd.getControllerKey());
        cdrom.setKey(vcd.getKey());
        cdrom.setUnitNumber(vcd.getUnitNumber());

        VirtualDeviceConnectInfo info = new VirtualDeviceConnectInfo();
        info.setAllowGuestControl(true);
        info.setConnected(true);
        info.setStartConnected(true);
        cdrom.setConnectable(info);

        cdrom.setBacking(vcd.getBacking());

        VirtualDeviceConfigSpec spec = new VirtualDeviceConfigSpec();
        spec.setDevice(cdrom);
        spec.setOperation(VirtualDeviceConfigSpecOperation.EDIT);

        return spec;
    }

    private VirtualDeviceConfigSpec resizeHdd(VirtualDisk sysdisk, DiskStateExpanded ds)
            throws FinderException, InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        VirtualDiskFlatVer2BackingInfo oldbacking = (VirtualDiskFlatVer2BackingInfo) sysdisk
                .getBacking();
        VirtualDiskFlatVer2BackingInfo backing = new VirtualDiskFlatVer2BackingInfo();
        backing.setDiskMode(getDiskMode(ds));
        backing.setThinProvisioned(oldbacking.isThinProvisioned());
        backing.setEagerlyScrub(oldbacking.isEagerlyScrub());
        backing.setFileName(oldbacking.getFileName());

        VirtualDisk disk = new VirtualDisk();
        if (toKb(ds.capacityMBytes) > sysdisk.getCapacityInKB()) {
            disk.setCapacityInKB(toKb(ds.capacityMBytes));
        } else {
            disk.setCapacityInKB(sysdisk.getCapacityInKB());
        }
        disk.setBacking(backing);
        disk.setStorageIOAllocation(getStorageIOAllocationInfo(ds));
        disk.setControllerKey(sysdisk.getControllerKey());
        disk.setUnitNumber(sysdisk.getUnitNumber());
        fillInControllerUnitNumber(ds, sysdisk.getUnitNumber());
        disk.setKey(sysdisk.getKey());

        VirtualDeviceConfigSpec change = new VirtualDeviceConfigSpec();
        change.setDevice(disk);
        change.setOperation(VirtualDeviceConfigSpecOperation.EDIT);

        return change;
    }

    private VirtualPCIController getFirstPciController(ArrayOfVirtualDevice devices) {
        for (VirtualDevice dev : devices.getVirtualDevice()) {
            if (dev instanceof VirtualPCIController) {
                return (VirtualPCIController) dev;
            }
        }

        return null;
    }

    /**
     * Once a vm is provisioned this method collects vsphere-assigned properties and stores them in
     * the {@link ComputeState#customProperties}
     *
     * @param state
     * @throws InvalidPropertyFaultMsg
     * @throws RuntimeFaultFaultMsg
     */
    public VmOverlay enrichStateFromVm(ComputeState state)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        Map<String, Object> props = this.get.entityProps(this.vm,
                VimPath.vm_config_instanceUuid,
                VimPath.vm_config_name,
                VimPath.vm_config_hardware_device,
                VimPath.vm_runtime_powerState,
                VimPath.vm_runtime_host,
                VimPath.vm_config_guestId,
                VimPath.vm_guest_net,
                VimPath.vm_summary_guest_ipAddress,
                VimPath.vm_summary_guest_hostName,
                VimPath.vm_datastore);

        VmOverlay overlay = new VmOverlay(this.vm, props);
        state.id = overlay.getInstanceUuid();
        state.primaryMAC = overlay.getPrimaryMac();
        state.powerState = overlay.getPowerState();
        state.address = overlay.guessPublicIpV4Address();
        state.name = overlay.getName();

        CustomProperties.of(state)
                .put(CustomProperties.MOREF, this.vm)
                .put(CustomProperties.TYPE, VimNames.TYPE_VM)
                .put(STORAGE_REFERENCE, overlay.getDatastoreMorefsAsString());

        return overlay;
    }

    /**
     * Creates a VM in vsphere. This method will block until the CreateVM_Task completes. The path
     * to the .vmx file is explicitly set and its existence is iterpreted as if the VM has been
     * successfully created and returns null.
     *
     * @return
     * @throws FinderException
     * @throws Exception
     */
    private ManagedObjectReference createVm() throws Exception {
        ManagedObjectReference folder = getVmFolder();
        List<VirtualMachineDefinedProfileSpec> pbmSpec = getPbmProfileSpec(this.bootDisk);
        ManagedObjectReference datastore = getDataStoreForDisk(this.bootDisk, pbmSpec);
        ManagedObjectReference resourcePool = getResourcePool();
        ManagedObjectReference host = getHost();

        // If datastore for disk is null, if storage policy is configured pick the compatible
        // datastore from that.
        if (datastore == null) {
            ManagedObjectReference dsFromSp = getDatastoreFromStoragePolicy(this.connection,
                    pbmSpec);
            datastore = dsFromSp == null ? getDatastore() : dsFromSp;
        }
        String datastoreName = this.get.entityProp(datastore, "name");
        VirtualMachineConfigSpec spec = buildVirtualMachineConfigSpec(datastoreName);

        String gt = CustomProperties.of(this.ctx.child).getString(CustomProperties.GUEST_ID, null);
        if (gt != null) {
            try {
                gt = VirtualMachineGuestOsIdentifier.valueOf(gt).value();
            } catch (IllegalArgumentException e) {
                // silently default to generic 64 bit guest.
                gt = DEFAULT_GUEST_ID.value();
            }

            spec.setGuestId(gt);
        }

        populateCloudConfig(spec, null);
        recordTimestamp(spec.getExtraConfig());
        // set the maximum snapshot limit if specified
        final String snapshotLimit = CustomProperties.of(this.ctx.child).getString(CustomProperties.SNAPSHOT_MAXIMUM_LIMIT);
        recordSnapshotLimit(spec.getExtraConfig(), snapshotLimit);

        ManagedObjectReference vmTask = getVimPort().createVMTask(folder, spec, resourcePool,
                host);

        TaskInfo info = waitTaskEnd(vmTask);

        if (info.getState() == TaskInfoState.ERROR) {
            MethodFault fault = info.getError().getFault();
            if (fault instanceof FileAlreadyExists) {
                // a .vmx file already exists, assume someone won the race to create the vm
                return null;
            } else {
                return VimUtils.rethrow(info.getError());
            }
        }

        return (ManagedObjectReference) info.getResult();
    }

    private boolean populateVAppProperties(VirtualMachineConfigSpec spec,
            ArrayOfVAppPropertyInfo currentProps) {
        if (this.bootDisk == null) {
            return false;
        }

        boolean customizationsApplied = false;
        int nextKey = 1;
        if (currentProps != null) {
            nextKey = currentProps.getVAppPropertyInfo().stream()
                    .mapToInt(VAppPropertyInfo::getKey)
                    .max()
                    .orElse(1);
            nextKey++;
        }

        String ovfEnv = getFileItemByPath(this.bootDisk, OVF_PROPERTY_ENV);
        if (ovfEnv != null) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = Utils.fromJson(ovfEnv, Map.class);
            if (!map.isEmpty()) {
                customizationsApplied = true;
                VmConfigSpec configSpec = new VmConfigSpec();
                configSpec.getOvfEnvironmentTransport().add(OvfDeployer.TRANSPORT_ISO);
                if (currentProps == null) {
                    currentProps = new ArrayOfVAppPropertyInfo();
                }

                currentProps.getVAppPropertyInfo().forEach(pi -> {
                    if (map.containsKey(pi.getId())) {
                        VAppPropertySpec ps = new VAppPropertySpec();
                        ps.setOperation(ArrayUpdateOperation.EDIT);
                        pi.setValue(map.remove(pi.getId()));

                        ps.setInfo(pi);
                        configSpec.getProperty().add(ps);
                    }
                });

                // only new key/values
                for (Entry<String, String> entry : map.entrySet()) {
                    VAppPropertyInfo pi = new VAppPropertyInfo();
                    pi.setId(entry.getKey());
                    pi.setType("string");
                    pi.setKey(nextKey++);
                    pi.setValue(entry.getValue());

                    VAppPropertySpec ps = new VAppPropertySpec();
                    ps.setOperation(ArrayUpdateOperation.ADD);
                    ps.setInfo(pi);
                    configSpec.getProperty().add(ps);
                }
                spec.setVAppConfig(configSpec);
            }
        }
        return customizationsApplied;
    }

    /**
     * Puts the cloud-config user data in the OVF environment
     *
     * @param spec
     * @param currentProps
     */
    private boolean populateCloudConfig(VirtualMachineConfigSpec spec,
            ArrayOfVAppPropertyInfo currentProps) {
        if (this.bootDisk == null) {
            return false;
        }

        boolean customizationsApplied = false;
        int nextKey = 1;
        if (currentProps != null) {
            nextKey = currentProps.getVAppPropertyInfo().stream()
                    .mapToInt(VAppPropertyInfo::getKey)
                    .max()
                    .orElse(1);
            nextKey++;
        }

        VmConfigSpec configSpec = new VmConfigSpec();
        configSpec.getOvfEnvironmentTransport().add(OvfDeployer.TRANSPORT_ISO);

        String cloudConfig = getFileItemByPath(this.bootDisk, CLOUD_CONFIG_PROPERTY_USER_DATA);
        if (cloudConfig != null) {
            VAppPropertySpec propertySpec = new VAppPropertySpec();

            VAppPropertyInfo userDataInfo = null;
            if (currentProps != null) {
                userDataInfo = currentProps.getVAppPropertyInfo().stream()
                        .filter(p -> p.getId().equals(CLOUD_CONFIG_PROPERTY_USER_DATA))
                        .findFirst()
                        .orElse(null);
                if (userDataInfo == null) {
                    // try coreOS key
                    userDataInfo = currentProps.getVAppPropertyInfo().stream()
                            .filter(p -> p.getId().equals(COREOS_CLOUD_CONFIG_PROPERTY_USER_DATA))
                            .findFirst()
                            .orElse(null);
                    if (userDataInfo != null) {
                        VAppPropertyInfo coreosEncoding = currentProps.getVAppPropertyInfo()
                                .stream()
                                .filter(p -> p.getId()
                                        .equals(COREOS_CLOUD_CONFIG_PROPERTY_USER_DATA_ENCODING))
                                .findFirst().orElse(null);
                        if (coreosEncoding != null) {
                            VAppPropertySpec pSpec = new VAppPropertySpec();
                            coreosEncoding.setValue(CLOUD_CONFIG_BASE64_ENCODING);
                            pSpec.setOperation(ArrayUpdateOperation.EDIT);
                            pSpec.setInfo(coreosEncoding);
                            configSpec.getProperty().add(pSpec);
                        }
                    }
                }
            }

            if (userDataInfo != null) {
                propertySpec.setOperation(ArrayUpdateOperation.EDIT);
            } else {
                userDataInfo = new VAppPropertyInfo();
                userDataInfo.setId(CLOUD_CONFIG_PROPERTY_USER_DATA);
                userDataInfo.setType("string");
                userDataInfo.setKey(nextKey++);
                propertySpec.setOperation(ArrayUpdateOperation.ADD);
            }
            String encodedUserData = Base64.getEncoder().encodeToString(cloudConfig.getBytes());
            userDataInfo.setValue(encodedUserData);

            propertySpec.setInfo(userDataInfo);
            configSpec.getProperty().add(propertySpec);
            customizationsApplied = true;
        }

        String publicKeys = getFileItemByPath(this.bootDisk, CLOUD_CONFIG_PROPERTY_PUBLIC_KEYS);
        if (publicKeys != null) {
            VAppPropertySpec propertySpec = new VAppPropertySpec();

            VAppPropertyInfo sshKeyInfo = null;
            if (currentProps != null) {
                sshKeyInfo = currentProps.getVAppPropertyInfo().stream()
                        .filter(p -> p.getId().equals(CLOUD_CONFIG_PROPERTY_PUBLIC_KEYS))
                        .findFirst()
                        .orElse(null);
            }
            if (sshKeyInfo != null) {
                propertySpec.setOperation(ArrayUpdateOperation.EDIT);
            } else {
                sshKeyInfo = new VAppPropertyInfo();
                sshKeyInfo.setType("string");
                sshKeyInfo.setId(CLOUD_CONFIG_PROPERTY_PUBLIC_KEYS);
                sshKeyInfo.setKey(nextKey++);
                propertySpec.setOperation(ArrayUpdateOperation.ADD);
            }
            sshKeyInfo.setValue(publicKeys);

            propertySpec.setInfo(sshKeyInfo);
            configSpec.getProperty().add(propertySpec);
            customizationsApplied = true;
        }

        String hostname = getFileItemByPath(this.bootDisk, CLOUD_CONFIG_PROPERTY_HOSTNAME);
        if (hostname != null) {
            VAppPropertySpec propertySpec = new VAppPropertySpec();

            VAppPropertyInfo hostInfo = null;
            if (currentProps != null) {
                hostInfo = currentProps.getVAppPropertyInfo().stream()
                        .filter(p -> p.getId().equals(CLOUD_CONFIG_PROPERTY_HOSTNAME))
                        .findFirst()
                        .orElse(null);
                if (hostInfo == null) {
                    // try coreOS key
                    hostInfo = currentProps.getVAppPropertyInfo().stream()
                            .filter(p -> p.getId().equals(COREOS_CLOUD_CONFIG_PROPERTY_HOSTNAME))
                            .findFirst()
                            .orElse(null);
                }
            }

            if (hostInfo != null) {
                propertySpec.setOperation(ArrayUpdateOperation.EDIT);
            } else {
                hostInfo = new VAppPropertyInfo();
                hostInfo.setId(CLOUD_CONFIG_PROPERTY_USER_DATA);
                hostInfo.setType("string");
                hostInfo.setKey(nextKey++);
                propertySpec.setOperation(ArrayUpdateOperation.ADD);
            }
            hostInfo.setValue(hostname);

            propertySpec.setInfo(hostInfo);
            configSpec.getProperty().add(propertySpec);
            customizationsApplied = true;
        }

        if (customizationsApplied) {
            spec.setVAppConfig(configSpec);
        }

        return customizationsApplied;
    }

    private String getFileItemByPath(DiskStateExpanded bootDisk, String fileName) {
        if (bootDisk != null && bootDisk.bootConfig != null && bootDisk.bootConfig.files != null) {
            for (FileEntry e : bootDisk.bootConfig.files) {
                if (Objects.equals(fileName, e.path)) {
                    return e.contents;
                }
            }
        }

        return null;
    }

    /**
     * Decides in which folder to put the newly created vm.
     *
     * @return
     * @throws InvalidPropertyFaultMsg
     * @throws RuntimeFaultFaultMsg
     * @throws FinderException
     */
    private ManagedObjectReference getVmFolder()
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, FinderException {

        // look for a configured folder in compute state
        String folderPath = CustomProperties.of(this.ctx.child)
                .getString(RESOURCE_GROUP_NAME);

        if (folderPath == null) {
            // look for a configured folder in parent
            folderPath = CustomProperties.of(this.ctx.parent)
                    .getString(RESOURCE_GROUP_NAME);
        }

        Element vmFolderElement = this.finder.vmFolder();
        if (folderPath == null) {
            return vmFolderElement.object;
        } else {
            return getExistingOrCreateNewFolder(vmFolderElement, folderPath);
        }
    }

    private ManagedObjectReference getExistingOrCreateNewFolder(Element folderElement, String folderPath) throws
            InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, FinderException {
        try {
            folderElement = this.finder.folder(folderPath);
        } catch (FinderException finderEx) {
            try {
                return this.get.createFolder(folderElement, folderPath);
            } catch (InvalidNameFaultMsg | DuplicateNameFaultMsg faultMsg) {
                throw new FinderException(String.format("Unable to create folder in the path: '%s'", folderPath), faultMsg.getCause());
            }
        }
        return folderElement.object;
    }

    /**
     * Creates a spec used to create the VM.
     *
     * @param datastoreName
     * @return
     * @throws InvalidPropertyFaultMsg
     * @throws FinderException
     * @throws RuntimeFaultFaultMsg
     */
    private VirtualMachineConfigSpec buildVirtualMachineConfigSpec(String datastoreName)
            throws InvalidPropertyFaultMsg, FinderException, RuntimeFaultFaultMsg {
        String displayName = this.ctx.child.name;

        VirtualMachineConfigSpec spec = new VirtualMachineConfigSpec();
        spec.setName(displayName);
        spec.setNumCPUs((int) this.ctx.child.description.cpuCount);
        spec.setGuestId(VirtualMachineGuestOsIdentifier.OTHER_GUEST_64.value());
        spec.setMemoryMB(toMemoryMb(this.ctx.child.description.totalMemoryBytes));

        VirtualMachineFileInfo files = new VirtualMachineFileInfo();
        // Use a full path to the config file to avoid creating a VM with the same name
        String path = String.format("[%s] %s/%s.vmx", datastoreName, displayName, displayName);
        files.setVmPathName(path);
        spec.setFiles(files);

        for (NetworkInterfaceStateWithDetails ni : this.ctx.nics) {
            VirtualDevice nic = createNic(ni, null);
            addDeviceToVm(spec, nic);
        }

        VirtualDevice scsi = createScsiController();
        addDeviceToVm(spec, scsi);

        return spec;
    }

    private void addDeviceToVm(VirtualMachineConfigSpec spec, VirtualDevice dev) {
        VirtualDeviceConfigSpec change = new VirtualDeviceConfigSpec();
        change.setDevice(dev);
        change.setOperation(VirtualDeviceConfigSpecOperation.ADD);
        spec.getDeviceChange().add(change);
    }

    private void addRemoveDeviceFromVm(VirtualMachineConfigSpec spec, VirtualDevice dev) {
        VirtualDeviceConfigSpec change = new VirtualDeviceConfigSpec();
        change.setDevice(dev);
        change.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);
        spec.getDeviceChange().add(change);
    }

    private VirtualDevice createScsiController() {
        VirtualLsiLogicController scsiCtrl = new VirtualLsiLogicController();
        scsiCtrl.setBusNumber(0);
        scsiCtrl.setKey(-1);
        scsiCtrl.setSharedBus(VirtualSCSISharing.NO_SHARING);

        return scsiCtrl;
    }

    private VirtualEthernetCard createNic(NetworkInterfaceStateWithDetails nicWithDetails,
            Integer controllerKey)
            throws FinderException, InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        VirtualEthernetCard nic = new VirtualE1000();
        nic.setAddressType(VirtualEthernetCardMacType.GENERATED.value());
        nic.setKey(-1);
        nic.setControllerKey(controllerKey);

        // Currently the network backing information is stored in both places subnet and network
        // If it were to exist at one state object, then it would reduce complexity further.
        // Question: Is it acceptable for querying subnet first and network later? Or the order
        // should be reversed?

        QueryConfigTargetRequest queryConfigTargetRequest = new
                QueryConfigTargetRequest(this.get, getVimPort(), this.ctx.computeMoRef);

        VirtualDeviceBackingInfo deviceBackingInfo = NetworkDeviceBackingFactory
                .getNetworkDeviceBackingInfo(nicWithDetails.subnet, queryConfigTargetRequest);

        if (deviceBackingInfo == null) {
            deviceBackingInfo = NetworkDeviceBackingFactory
                    .getNetworkDeviceBackingInfo(nicWithDetails.network, queryConfigTargetRequest);
        }

        nic.setBacking(deviceBackingInfo);

        return nic;
    }

    /**
     * Convert bytes to MB rounding up to the nearest 4MB block.
     *
     * @param bytes
     * @return
     */
    private long toMemoryMb(long bytes) {
        long mb = bytes / 1024 / 1024;
        return mb / 4 * 4;
    }

    /**
     * If there is a datastore that is specified for the disk in custom properties, then it will
     * be used, otherwise fall back to default datastore selection if there is no storage policy
     * specified for this disk. If storage policy is specified for this disk, then that will be
     * honored.
     */
    private ManagedObjectReference getDataStoreForDisk(DiskStateExpanded diskState,
            List<VirtualMachineDefinedProfileSpec> pbmSpec)
            throws FinderException, InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        ManagedObjectReference datastore = null;
        if (diskState.storageDescription != null) {
            datastore = this.finder.datastore(diskState.storageDescription.id).object;
        }
        return datastore != null ? datastore : (pbmSpec == null ? getDatastore() : null);
    }

    /**
     * Finds the datastore to use for the VM from the ComputeState.description.datastoreId.
     */
    private ManagedObjectReference getDatastore()
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, FinderException {
        if (this.datastore != null) {
            return this.datastore;
        }

        String datastorePath = this.ctx.child.description.dataStoreId;

        if (datastorePath == null) {
            ArrayOfManagedObjectReference datastores = findDatastoresForPlacement(
                    this.ctx.computeMoRef);
            if (datastores == null || datastores.getManagedObjectReference().isEmpty()) {
                this.datastore = this.finder.defaultDatastore().object;
            } else {
                this.datastore = datastores.getManagedObjectReference().get(0);
            }
        } else {
            this.datastore = this.finder.datastore(datastorePath).object;
        }

        return this.datastore;
    }

    private ArrayOfManagedObjectReference findDatastoresForPlacement(ManagedObjectReference target)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        if (VimNames.TYPE_RESOURCE_POOL.equals(target.getType())) {
            ManagedObjectReference owner = this.get.entityProp(target, VimNames.PROPERTY_OWNER);
            return findDatastoresForPlacement(owner);
        }
        // at this point a target is either host or ComputeResource: both have a property
        // "datastore"
        return this.get.entityProp(target, VimPath.res_datastore);
    }

    private ManagedObjectReference getResourcePool()
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        if (this.resourcePool != null) {
            return this.resourcePool;
        }

        if (VimNames.TYPE_HOST.equals(this.ctx.computeMoRef.getType())) {
            // find the ComputeResource representing this host and use its root resource pool
            ManagedObjectReference parentCompute = this.get.entityProp(this.ctx.computeMoRef,
                    VimPath.host_parent);
            this.resourcePool = this.get.entityProp(parentCompute, VimPath.res_resourcePool);
        } else if (
                VimNames.TYPE_CLUSTER_COMPUTE_RESOURCE.equals(this.ctx.computeMoRef.getType())
                        ||
                        VimNames.TYPE_COMPUTE_RESOURCE
                                .equals(this.ctx.computeMoRef.getType())) {
            // place in the root resource pool of a cluster
            this.resourcePool = this.get
                    .entityProp(this.ctx.computeMoRef, VimPath.res_resourcePool);
        } else if (VimNames.TYPE_RESOURCE_POOL.equals(this.ctx.computeMoRef.getType())) {
            // place in the resource pool itself
            this.resourcePool = this.ctx.computeMoRef;
        } else {
            throw new IllegalArgumentException("Cannot place instance on " +
                    VimUtils.convertMoRefToString(this.ctx.computeMoRef));
        }

        return this.resourcePool;
    }

    private ManagedObjectReference getHost()
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        if (this.host != null) {
            return this.host;
        }

        if (VimNames.TYPE_HOST.equals(this.ctx.computeMoRef.getType())) {
            this.host = this.ctx.computeMoRef;
        }

        return this.host;
    }

    public ManagedObjectReference getVm() {
        return this.vm;
    }

    public ComputeState createInstanceFromLibraryItem(ImageState image) throws Exception {
        VapiConnection vapi = VapiConnection.createFromVimConnection(this.connection);
        vapi.login();
        try {
            LibraryClient client = vapi.newLibraryClient();

            List<VirtualMachineDefinedProfileSpec> pbmSpec = getPbmProfileSpec(this.bootDisk);
            ManagedObjectReference datastore = getDataStoreForDisk(this.bootDisk, pbmSpec);

            Map<String, String> mapping = new HashMap<>();
            ObjectNode result = client.deployOvfLibItem(image.id, this.ctx.child.name, getVmFolder(),
                    datastore, pbmSpec != null && !pbmSpec.isEmpty() ? pbmSpec.iterator().next() : null,
                    getResourcePool(), mapping, getDiskProvisioningType(this.bootDisk));

            if (!result.get("succeeded").asBoolean()) {
                // Log here to understand why deploy from library fails.
                logger.warn("Error deploying from library {}", result.toString());
                throw new Exception("Error deploying from library");
            }

            ManagedObjectReference ref = new ManagedObjectReference();
            ref.setType(VimNames.TYPE_VM);
            ref.setValue(VapiClient.getString(result,
                    "resource_id",
                    VapiClient.K_OPTIONAL,
                    VapiClient.K_STRUCTURE,
                    "com.vmware.vcenter.ovf.library_item.deployable_identity",
                    "id"));

            this.vm = ref;

            customizeAfterClone();

            ComputeState state = new ComputeState();
            state.resourcePoolLink = VimUtils
                    .firstNonNull(this.ctx.child.resourcePoolLink, this.ctx.parent.resourcePoolLink);

            return state;
        } finally {
            vapi.close();
        }
    }

    private Lock getLock(String key) {
        return lockPerUri.computeIfAbsent(key, u -> new ReentrantLock());
    }
}
