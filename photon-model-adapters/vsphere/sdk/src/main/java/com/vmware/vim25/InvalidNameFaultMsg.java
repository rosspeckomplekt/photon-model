
package com.vmware.vim25;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.1.6
 * 2016-07-18T20:02:08.968+03:00
 * Generated source version: 3.1.6
 */

@WebFault(name = "InvalidNameFault", targetNamespace = "urn:vim25")
public class InvalidNameFaultMsg extends Exception {
    public static final long serialVersionUID = 1L;
    
    private com.vmware.vim25.InvalidName invalidNameFault;

    public InvalidNameFaultMsg() {
        super();
    }
    
    public InvalidNameFaultMsg(String message) {
        super(message);
    }
    
    public InvalidNameFaultMsg(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidNameFaultMsg(String message, com.vmware.vim25.InvalidName invalidNameFault) {
        super(message);
        this.invalidNameFault = invalidNameFault;
    }

    public InvalidNameFaultMsg(String message, com.vmware.vim25.InvalidName invalidNameFault, Throwable cause) {
        super(message, cause);
        this.invalidNameFault = invalidNameFault;
    }

    public com.vmware.vim25.InvalidName getFaultInfo() {
        return this.invalidNameFault;
    }
}