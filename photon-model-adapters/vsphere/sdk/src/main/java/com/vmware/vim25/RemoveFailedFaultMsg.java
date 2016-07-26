
package com.vmware.vim25;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.1.6
 * 2016-07-18T20:02:09.063+03:00
 * Generated source version: 3.1.6
 */

@WebFault(name = "RemoveFailedFault", targetNamespace = "urn:vim25")
public class RemoveFailedFaultMsg extends Exception {
    public static final long serialVersionUID = 1L;
    
    private com.vmware.vim25.RemoveFailed removeFailedFault;

    public RemoveFailedFaultMsg() {
        super();
    }
    
    public RemoveFailedFaultMsg(String message) {
        super(message);
    }
    
    public RemoveFailedFaultMsg(String message, Throwable cause) {
        super(message, cause);
    }

    public RemoveFailedFaultMsg(String message, com.vmware.vim25.RemoveFailed removeFailedFault) {
        super(message);
        this.removeFailedFault = removeFailedFault;
    }

    public RemoveFailedFaultMsg(String message, com.vmware.vim25.RemoveFailed removeFailedFault, Throwable cause) {
        super(message, cause);
        this.removeFailedFault = removeFailedFault;
    }

    public com.vmware.vim25.RemoveFailed getFaultInfo() {
        return this.removeFailedFault;
    }
}