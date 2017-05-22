
package com.vmware.vim25;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.1.6
 * 2017-05-25T13:48:07.460+05:30
 * Generated source version: 3.1.6
 */

@WebFault(name = "SSPIChallengeFault", targetNamespace = "urn:vim25")
public class SSPIChallengeFaultMsg extends Exception {
    public static final long serialVersionUID = 1L;
    
    private com.vmware.vim25.SSPIChallenge sspiChallengeFault;

    public SSPIChallengeFaultMsg() {
        super();
    }
    
    public SSPIChallengeFaultMsg(String message) {
        super(message);
    }
    
    public SSPIChallengeFaultMsg(String message, Throwable cause) {
        super(message, cause);
    }

    public SSPIChallengeFaultMsg(String message, com.vmware.vim25.SSPIChallenge sspiChallengeFault) {
        super(message);
        this.sspiChallengeFault = sspiChallengeFault;
    }

    public SSPIChallengeFaultMsg(String message, com.vmware.vim25.SSPIChallenge sspiChallengeFault, Throwable cause) {
        super(message, cause);
        this.sspiChallengeFault = sspiChallengeFault;
    }

    public com.vmware.vim25.SSPIChallenge getFaultInfo() {
        return this.sspiChallengeFault;
    }
}
