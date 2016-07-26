
package com.vmware.vim25;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.1.6
 * 2016-07-18T20:02:09.066+03:00
 * Generated source version: 3.1.6
 */

@WebFault(name = "InvalidLicenseFault", targetNamespace = "urn:vim25")
public class InvalidLicenseFaultMsg extends Exception {
    public static final long serialVersionUID = 1L;
    
    private com.vmware.vim25.InvalidLicense invalidLicenseFault;

    public InvalidLicenseFaultMsg() {
        super();
    }
    
    public InvalidLicenseFaultMsg(String message) {
        super(message);
    }
    
    public InvalidLicenseFaultMsg(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidLicenseFaultMsg(String message, com.vmware.vim25.InvalidLicense invalidLicenseFault) {
        super(message);
        this.invalidLicenseFault = invalidLicenseFault;
    }

    public InvalidLicenseFaultMsg(String message, com.vmware.vim25.InvalidLicense invalidLicenseFault, Throwable cause) {
        super(message, cause);
        this.invalidLicenseFault = invalidLicenseFault;
    }

    public com.vmware.vim25.InvalidLicense getFaultInfo() {
        return this.invalidLicenseFault;
    }
}