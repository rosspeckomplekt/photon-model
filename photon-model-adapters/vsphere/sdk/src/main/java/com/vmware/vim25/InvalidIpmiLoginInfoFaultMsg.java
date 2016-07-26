
package com.vmware.vim25;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.1.6
 * 2016-07-18T20:02:09.040+03:00
 * Generated source version: 3.1.6
 */

@WebFault(name = "InvalidIpmiLoginInfoFault", targetNamespace = "urn:vim25")
public class InvalidIpmiLoginInfoFaultMsg extends Exception {
    public static final long serialVersionUID = 1L;
    
    private com.vmware.vim25.InvalidIpmiLoginInfo invalidIpmiLoginInfoFault;

    public InvalidIpmiLoginInfoFaultMsg() {
        super();
    }
    
    public InvalidIpmiLoginInfoFaultMsg(String message) {
        super(message);
    }
    
    public InvalidIpmiLoginInfoFaultMsg(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidIpmiLoginInfoFaultMsg(String message, com.vmware.vim25.InvalidIpmiLoginInfo invalidIpmiLoginInfoFault) {
        super(message);
        this.invalidIpmiLoginInfoFault = invalidIpmiLoginInfoFault;
    }

    public InvalidIpmiLoginInfoFaultMsg(String message, com.vmware.vim25.InvalidIpmiLoginInfo invalidIpmiLoginInfoFault, Throwable cause) {
        super(message, cause);
        this.invalidIpmiLoginInfoFault = invalidIpmiLoginInfoFault;
    }

    public com.vmware.vim25.InvalidIpmiLoginInfo getFaultInfo() {
        return this.invalidIpmiLoginInfoFault;
    }
}