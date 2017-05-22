
package com.vmware.vim25;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.1.6
 * 2017-05-25T13:48:06.939+05:30
 * Generated source version: 3.1.6
 */

@WebFault(name = "RequestCanceledFault", targetNamespace = "urn:vim25")
public class RequestCanceledFaultMsg extends Exception {
    public static final long serialVersionUID = 1L;
    
    private com.vmware.vim25.RequestCanceled requestCanceledFault;

    public RequestCanceledFaultMsg() {
        super();
    }
    
    public RequestCanceledFaultMsg(String message) {
        super(message);
    }
    
    public RequestCanceledFaultMsg(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestCanceledFaultMsg(String message, com.vmware.vim25.RequestCanceled requestCanceledFault) {
        super(message);
        this.requestCanceledFault = requestCanceledFault;
    }

    public RequestCanceledFaultMsg(String message, com.vmware.vim25.RequestCanceled requestCanceledFault, Throwable cause) {
        super(message, cause);
        this.requestCanceledFault = requestCanceledFault;
    }

    public com.vmware.vim25.RequestCanceled getFaultInfo() {
        return this.requestCanceledFault;
    }
}
