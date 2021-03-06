
package com.vmware.pbm;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import com.vmware.vim25.ManagedObjectReference;


/**
 * <p>Java class for PbmQueryDefaultRequirementProfileRequestType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PbmQueryDefaultRequirementProfileRequestType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="_this" type="{urn:vim25}ManagedObjectReference"/&gt;
 *         &lt;element name="hub" type="{urn:pbm}PbmPlacementHub"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PbmQueryDefaultRequirementProfileRequestType", propOrder = {
    "_this",
    "hub"
})
public class PbmQueryDefaultRequirementProfileRequestType {

    @XmlElement(required = true)
    protected ManagedObjectReference _this;
    @XmlElement(required = true)
    protected PbmPlacementHub hub;

    /**
     * Gets the value of the this property.
     * 
     * @return
     *     possible object is
     *     {@link ManagedObjectReference }
     *     
     */
    public ManagedObjectReference getThis() {
        return _this;
    }

    /**
     * Sets the value of the this property.
     * 
     * @param value
     *     allowed object is
     *     {@link ManagedObjectReference }
     *     
     */
    public void setThis(ManagedObjectReference value) {
        this._this = value;
    }

    /**
     * Gets the value of the hub property.
     * 
     * @return
     *     possible object is
     *     {@link PbmPlacementHub }
     *     
     */
    public PbmPlacementHub getHub() {
        return hub;
    }

    /**
     * Sets the value of the hub property.
     * 
     * @param value
     *     allowed object is
     *     {@link PbmPlacementHub }
     *     
     */
    public void setHub(PbmPlacementHub value) {
        this.hub = value;
    }

}
