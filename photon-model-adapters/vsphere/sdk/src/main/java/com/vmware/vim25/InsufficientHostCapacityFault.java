
package com.vmware.vim25;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for InsufficientHostCapacityFault complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="InsufficientHostCapacityFault"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:vim25}InsufficientResourcesFault"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="host" type="{urn:vim25}ManagedObjectReference" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InsufficientHostCapacityFault", propOrder = {
    "host"
})
@XmlSeeAlso({
    InsufficientHostCpuCapacityFault.class,
    InsufficientHostMemoryCapacityFault.class,
    InsufficientPerCpuCapacity.class
})
public class InsufficientHostCapacityFault
    extends InsufficientResourcesFault
{

    protected ManagedObjectReference host;

    /**
     * Gets the value of the host property.
     * 
     * @return
     *     possible object is
     *     {@link ManagedObjectReference }
     *     
     */
    public ManagedObjectReference getHost() {
        return host;
    }

    /**
     * Sets the value of the host property.
     * 
     * @param value
     *     allowed object is
     *     {@link ManagedObjectReference }
     *     
     */
    public void setHost(ManagedObjectReference value) {
        this.host = value;
    }

}
