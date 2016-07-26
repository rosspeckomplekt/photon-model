
package com.vmware.vim25;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.1.6
 * 2016-07-18T20:02:09.024+03:00
 * Generated source version: 3.1.6
 */

@WebFault(name = "HostPowerOpFailedFault", targetNamespace = "urn:vim25")
public class HostPowerOpFailedFaultMsg extends Exception {
    public static final long serialVersionUID = 1L;
    
    private com.vmware.vim25.HostPowerOpFailed hostPowerOpFailedFault;

    public HostPowerOpFailedFaultMsg() {
        super();
    }
    
    public HostPowerOpFailedFaultMsg(String message) {
        super(message);
    }
    
    public HostPowerOpFailedFaultMsg(String message, Throwable cause) {
        super(message, cause);
    }

    public HostPowerOpFailedFaultMsg(String message, com.vmware.vim25.HostPowerOpFailed hostPowerOpFailedFault) {
        super(message);
        this.hostPowerOpFailedFault = hostPowerOpFailedFault;
    }

    public HostPowerOpFailedFaultMsg(String message, com.vmware.vim25.HostPowerOpFailed hostPowerOpFailedFault, Throwable cause) {
        super(message, cause);
        this.hostPowerOpFailedFault = hostPowerOpFailedFault;
    }

    public com.vmware.vim25.HostPowerOpFailed getFaultInfo() {
        return this.hostPowerOpFailedFault;
    }
}