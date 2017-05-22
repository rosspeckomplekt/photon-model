
package com.vmware.vim25;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.1.6
 * 2017-05-25T13:48:07.155+05:30
 * Generated source version: 3.1.6
 */

@WebFault(name = "NotSupportedFault", targetNamespace = "urn:vim25")
public class NotSupportedFaultMsg extends Exception {
    public static final long serialVersionUID = 1L;
    
    private com.vmware.vim25.NotSupported notSupportedFault;

    public NotSupportedFaultMsg() {
        super();
    }
    
    public NotSupportedFaultMsg(String message) {
        super(message);
    }
    
    public NotSupportedFaultMsg(String message, Throwable cause) {
        super(message, cause);
    }

    public NotSupportedFaultMsg(String message, com.vmware.vim25.NotSupported notSupportedFault) {
        super(message);
        this.notSupportedFault = notSupportedFault;
    }

    public NotSupportedFaultMsg(String message, com.vmware.vim25.NotSupported notSupportedFault, Throwable cause) {
        super(message, cause);
        this.notSupportedFault = notSupportedFault;
    }

    public com.vmware.vim25.NotSupported getFaultInfo() {
        return this.notSupportedFault;
    }
}
