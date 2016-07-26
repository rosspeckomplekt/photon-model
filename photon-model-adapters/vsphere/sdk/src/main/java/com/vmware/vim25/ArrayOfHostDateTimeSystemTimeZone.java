
package com.vmware.vim25;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfHostDateTimeSystemTimeZone complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfHostDateTimeSystemTimeZone"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="HostDateTimeSystemTimeZone" type="{urn:vim25}HostDateTimeSystemTimeZone" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfHostDateTimeSystemTimeZone", propOrder = {
    "hostDateTimeSystemTimeZone"
})
public class ArrayOfHostDateTimeSystemTimeZone {

    @XmlElement(name = "HostDateTimeSystemTimeZone")
    protected List<HostDateTimeSystemTimeZone> hostDateTimeSystemTimeZone;

    /**
     * Gets the value of the hostDateTimeSystemTimeZone property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the hostDateTimeSystemTimeZone property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getHostDateTimeSystemTimeZone().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link HostDateTimeSystemTimeZone }
     * 
     * 
     */
    public List<HostDateTimeSystemTimeZone> getHostDateTimeSystemTimeZone() {
        if (hostDateTimeSystemTimeZone == null) {
            hostDateTimeSystemTimeZone = new ArrayList<HostDateTimeSystemTimeZone>();
        }
        return this.hostDateTimeSystemTimeZone;
    }

}