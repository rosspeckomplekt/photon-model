
package com.vmware.vim25;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.1.6
 * 2016-07-18T20:02:09.009+03:00
 * Generated source version: 3.1.6
 */

@WebFault(name = "HostIncompatibleForRecordReplayFault", targetNamespace = "urn:vim25")
public class HostIncompatibleForRecordReplayFaultMsg extends Exception {
    public static final long serialVersionUID = 1L;
    
    private com.vmware.vim25.HostIncompatibleForRecordReplay hostIncompatibleForRecordReplayFault;

    public HostIncompatibleForRecordReplayFaultMsg() {
        super();
    }
    
    public HostIncompatibleForRecordReplayFaultMsg(String message) {
        super(message);
    }
    
    public HostIncompatibleForRecordReplayFaultMsg(String message, Throwable cause) {
        super(message, cause);
    }

    public HostIncompatibleForRecordReplayFaultMsg(String message, com.vmware.vim25.HostIncompatibleForRecordReplay hostIncompatibleForRecordReplayFault) {
        super(message);
        this.hostIncompatibleForRecordReplayFault = hostIncompatibleForRecordReplayFault;
    }

    public HostIncompatibleForRecordReplayFaultMsg(String message, com.vmware.vim25.HostIncompatibleForRecordReplay hostIncompatibleForRecordReplayFault, Throwable cause) {
        super(message, cause);
        this.hostIncompatibleForRecordReplayFault = hostIncompatibleForRecordReplayFault;
    }

    public com.vmware.vim25.HostIncompatibleForRecordReplay getFaultInfo() {
        return this.hostIncompatibleForRecordReplayFault;
    }
}