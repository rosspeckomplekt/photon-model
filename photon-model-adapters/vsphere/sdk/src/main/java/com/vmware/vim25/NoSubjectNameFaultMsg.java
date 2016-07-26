
package com.vmware.vim25;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.1.6
 * 2016-07-18T20:02:09.052+03:00
 * Generated source version: 3.1.6
 */

@WebFault(name = "NoSubjectNameFault", targetNamespace = "urn:vim25")
public class NoSubjectNameFaultMsg extends Exception {
    public static final long serialVersionUID = 1L;
    
    private com.vmware.vim25.NoSubjectName noSubjectNameFault;

    public NoSubjectNameFaultMsg() {
        super();
    }
    
    public NoSubjectNameFaultMsg(String message) {
        super(message);
    }
    
    public NoSubjectNameFaultMsg(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSubjectNameFaultMsg(String message, com.vmware.vim25.NoSubjectName noSubjectNameFault) {
        super(message);
        this.noSubjectNameFault = noSubjectNameFault;
    }

    public NoSubjectNameFaultMsg(String message, com.vmware.vim25.NoSubjectName noSubjectNameFault, Throwable cause) {
        super(message, cause);
        this.noSubjectNameFault = noSubjectNameFault;
    }

    public com.vmware.vim25.NoSubjectName getFaultInfo() {
        return this.noSubjectNameFault;
    }
}