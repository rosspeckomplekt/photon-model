
package com.vmware.vim25;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.1.6
 * 2016-07-18T20:02:09.022+03:00
 * Generated source version: 3.1.6
 */

@WebFault(name = "InvalidPropertyFault", targetNamespace = "urn:vim25")
public class InvalidPropertyFaultMsg extends Exception {
    public static final long serialVersionUID = 1L;
    
    private com.vmware.vim25.InvalidProperty invalidPropertyFault;

    public InvalidPropertyFaultMsg() {
        super();
    }
    
    public InvalidPropertyFaultMsg(String message) {
        super(message);
    }
    
    public InvalidPropertyFaultMsg(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPropertyFaultMsg(String message, com.vmware.vim25.InvalidProperty invalidPropertyFault) {
        super(message);
        this.invalidPropertyFault = invalidPropertyFault;
    }

    public InvalidPropertyFaultMsg(String message, com.vmware.vim25.InvalidProperty invalidPropertyFault, Throwable cause) {
        super(message, cause);
        this.invalidPropertyFault = invalidPropertyFault;
    }

    public com.vmware.vim25.InvalidProperty getFaultInfo() {
        return this.invalidPropertyFault;
    }
}