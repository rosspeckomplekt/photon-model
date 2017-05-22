
package com.vmware.vim25;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.1.6
 * 2017-05-25T13:48:07.464+05:30
 * Generated source version: 3.1.6
 */

@WebFault(name = "AnswerFileUpdateFailedFault", targetNamespace = "urn:vim25")
public class AnswerFileUpdateFailedFaultMsg extends Exception {
    public static final long serialVersionUID = 1L;
    
    private com.vmware.vim25.AnswerFileUpdateFailed answerFileUpdateFailedFault;

    public AnswerFileUpdateFailedFaultMsg() {
        super();
    }
    
    public AnswerFileUpdateFailedFaultMsg(String message) {
        super(message);
    }
    
    public AnswerFileUpdateFailedFaultMsg(String message, Throwable cause) {
        super(message, cause);
    }

    public AnswerFileUpdateFailedFaultMsg(String message, com.vmware.vim25.AnswerFileUpdateFailed answerFileUpdateFailedFault) {
        super(message);
        this.answerFileUpdateFailedFault = answerFileUpdateFailedFault;
    }

    public AnswerFileUpdateFailedFaultMsg(String message, com.vmware.vim25.AnswerFileUpdateFailed answerFileUpdateFailedFault, Throwable cause) {
        super(message, cause);
        this.answerFileUpdateFailedFault = answerFileUpdateFailedFault;
    }

    public com.vmware.vim25.AnswerFileUpdateFailed getFaultInfo() {
        return this.answerFileUpdateFailedFault;
    }
}
