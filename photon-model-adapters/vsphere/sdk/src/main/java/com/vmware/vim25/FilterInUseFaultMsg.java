
package com.vmware.vim25;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.1.6
 * 2016-07-18T20:02:09.001+03:00
 * Generated source version: 3.1.6
 */

@WebFault(name = "FilterInUseFault", targetNamespace = "urn:vim25")
public class FilterInUseFaultMsg extends Exception {
    public static final long serialVersionUID = 1L;
    
    private com.vmware.vim25.FilterInUse filterInUseFault;

    public FilterInUseFaultMsg() {
        super();
    }
    
    public FilterInUseFaultMsg(String message) {
        super(message);
    }
    
    public FilterInUseFaultMsg(String message, Throwable cause) {
        super(message, cause);
    }

    public FilterInUseFaultMsg(String message, com.vmware.vim25.FilterInUse filterInUseFault) {
        super(message);
        this.filterInUseFault = filterInUseFault;
    }

    public FilterInUseFaultMsg(String message, com.vmware.vim25.FilterInUse filterInUseFault, Throwable cause) {
        super(message, cause);
        this.filterInUseFault = filterInUseFault;
    }

    public com.vmware.vim25.FilterInUse getFaultInfo() {
        return this.filterInUseFault;
    }
}