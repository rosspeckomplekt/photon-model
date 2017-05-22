
package com.vmware.vim25;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.1.6
 * 2017-05-25T13:48:06.969+05:30
 * Generated source version: 3.1.6
 */

@WebFault(name = "PatchNotApplicableFault", targetNamespace = "urn:vim25")
public class PatchNotApplicableFaultMsg extends Exception {
    public static final long serialVersionUID = 1L;
    
    private com.vmware.vim25.PatchNotApplicable patchNotApplicableFault;

    public PatchNotApplicableFaultMsg() {
        super();
    }
    
    public PatchNotApplicableFaultMsg(String message) {
        super(message);
    }
    
    public PatchNotApplicableFaultMsg(String message, Throwable cause) {
        super(message, cause);
    }

    public PatchNotApplicableFaultMsg(String message, com.vmware.vim25.PatchNotApplicable patchNotApplicableFault) {
        super(message);
        this.patchNotApplicableFault = patchNotApplicableFault;
    }

    public PatchNotApplicableFaultMsg(String message, com.vmware.vim25.PatchNotApplicable patchNotApplicableFault, Throwable cause) {
        super(message, cause);
        this.patchNotApplicableFault = patchNotApplicableFault;
    }

    public com.vmware.vim25.PatchNotApplicable getFaultInfo() {
        return this.patchNotApplicableFault;
    }
}
