
package com.vmware.vim25;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.1.6
 * 2016-07-18T20:02:08.955+03:00
 * Generated source version: 3.1.6
 */

@WebFault(name = "HostConfigFaultFault", targetNamespace = "urn:vim25")
public class HostConfigFaultFaultMsg extends Exception {
    public static final long serialVersionUID = 1L;
    
    private com.vmware.vim25.HostConfigFault hostConfigFaultFault;

    public HostConfigFaultFaultMsg() {
        super();
    }
    
    public HostConfigFaultFaultMsg(String message) {
        super(message);
    }
    
    public HostConfigFaultFaultMsg(String message, Throwable cause) {
        super(message, cause);
    }

    public HostConfigFaultFaultMsg(String message, com.vmware.vim25.HostConfigFault hostConfigFaultFault) {
        super(message);
        this.hostConfigFaultFault = hostConfigFaultFault;
    }

    public HostConfigFaultFaultMsg(String message, com.vmware.vim25.HostConfigFault hostConfigFaultFault, Throwable cause) {
        super(message, cause);
        this.hostConfigFaultFault = hostConfigFaultFault;
    }

    public com.vmware.vim25.HostConfigFault getFaultInfo() {
        return this.hostConfigFaultFault;
    }
}