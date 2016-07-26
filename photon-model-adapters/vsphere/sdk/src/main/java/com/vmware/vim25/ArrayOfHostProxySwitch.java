
package com.vmware.vim25;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfHostProxySwitch complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfHostProxySwitch"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="HostProxySwitch" type="{urn:vim25}HostProxySwitch" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfHostProxySwitch", propOrder = {
    "hostProxySwitch"
})
public class ArrayOfHostProxySwitch {

    @XmlElement(name = "HostProxySwitch")
    protected List<HostProxySwitch> hostProxySwitch;

    /**
     * Gets the value of the hostProxySwitch property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the hostProxySwitch property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getHostProxySwitch().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link HostProxySwitch }
     * 
     * 
     */
    public List<HostProxySwitch> getHostProxySwitch() {
        if (hostProxySwitch == null) {
            hostProxySwitch = new ArrayList<HostProxySwitch>();
        }
        return this.hostProxySwitch;
    }

}