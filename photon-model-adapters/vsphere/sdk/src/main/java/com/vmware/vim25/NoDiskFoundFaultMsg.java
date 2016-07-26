
package com.vmware.vim25;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.1.6
 * 2016-07-18T20:02:09.010+03:00
 * Generated source version: 3.1.6
 */

@WebFault(name = "NoDiskFoundFault", targetNamespace = "urn:vim25")
public class NoDiskFoundFaultMsg extends Exception {
    public static final long serialVersionUID = 1L;
    
    private com.vmware.vim25.NoDiskFound noDiskFoundFault;

    public NoDiskFoundFaultMsg() {
        super();
    }
    
    public NoDiskFoundFaultMsg(String message) {
        super(message);
    }
    
    public NoDiskFoundFaultMsg(String message, Throwable cause) {
        super(message, cause);
    }

    public NoDiskFoundFaultMsg(String message, com.vmware.vim25.NoDiskFound noDiskFoundFault) {
        super(message);
        this.noDiskFoundFault = noDiskFoundFault;
    }

    public NoDiskFoundFaultMsg(String message, com.vmware.vim25.NoDiskFound noDiskFoundFault, Throwable cause) {
        super(message, cause);
        this.noDiskFoundFault = noDiskFoundFault;
    }

    public com.vmware.vim25.NoDiskFound getFaultInfo() {
        return this.noDiskFoundFault;
    }
}