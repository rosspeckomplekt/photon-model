
package com.vmware.vim25;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for FaultToleranceMetaSpec complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="FaultToleranceMetaSpec"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:vim25}DynamicData"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="metaDataDatastore" type="{urn:vim25}ManagedObjectReference"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FaultToleranceMetaSpec", propOrder = {
    "metaDataDatastore"
})
public class FaultToleranceMetaSpec
    extends DynamicData
{

    @XmlElement(required = true)
    protected ManagedObjectReference metaDataDatastore;

    /**
     * Gets the value of the metaDataDatastore property.
     * 
     * @return
     *     possible object is
     *     {@link ManagedObjectReference }
     *     
     */
    public ManagedObjectReference getMetaDataDatastore() {
        return metaDataDatastore;
    }

    /**
     * Sets the value of the metaDataDatastore property.
     * 
     * @param value
     *     allowed object is
     *     {@link ManagedObjectReference }
     *     
     */
    public void setMetaDataDatastore(ManagedObjectReference value) {
        this.metaDataDatastore = value;
    }

}
