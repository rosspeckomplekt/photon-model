
package com.vmware.pbm;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.1.6
 * 2017-05-25T13:48:23.446+05:30
 * Generated source version: 3.1.6
 */

@WebFault(name = "PbmFaultFault", targetNamespace = "urn:pbm")
public class PbmFaultFaultMsg extends Exception {
    public static final long serialVersionUID = 1L;
    
    private com.vmware.pbm.PbmFault pbmFaultFault;

    public PbmFaultFaultMsg() {
        super();
    }
    
    public PbmFaultFaultMsg(String message) {
        super(message);
    }
    
    public PbmFaultFaultMsg(String message, Throwable cause) {
        super(message, cause);
    }

    public PbmFaultFaultMsg(String message, com.vmware.pbm.PbmFault pbmFaultFault) {
        super(message);
        this.pbmFaultFault = pbmFaultFault;
    }

    public PbmFaultFaultMsg(String message, com.vmware.pbm.PbmFault pbmFaultFault, Throwable cause) {
        super(message, cause);
        this.pbmFaultFault = pbmFaultFault;
    }

    public com.vmware.pbm.PbmFault getFaultInfo() {
        return this.pbmFaultFault;
    }
}
