
package com.vmware.vim25;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for VirtualCdromPassthroughBackingOption complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="VirtualCdromPassthroughBackingOption"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:vim25}VirtualDeviceDeviceBackingOption"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="exclusive" type="{urn:vim25}BoolOption"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "VirtualCdromPassthroughBackingOption", propOrder = {
    "exclusive"
})
public class VirtualCdromPassthroughBackingOption
    extends VirtualDeviceDeviceBackingOption
{

    @XmlElement(required = true)
    protected BoolOption exclusive;

    /**
     * Gets the value of the exclusive property.
     * 
     * @return
     *     possible object is
     *     {@link BoolOption }
     *     
     */
    public BoolOption getExclusive() {
        return exclusive;
    }

    /**
     * Sets the value of the exclusive property.
     * 
     * @param value
     *     allowed object is
     *     {@link BoolOption }
     *     
     */
    public void setExclusive(BoolOption value) {
        this.exclusive = value;
    }

}