
package com.vmware.vim25;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DvsUpgradedEvent complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DvsUpgradedEvent"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:vim25}DvsEvent"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="productInfo" type="{urn:vim25}DistributedVirtualSwitchProductSpec"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DvsUpgradedEvent", propOrder = {
    "productInfo"
})
public class DvsUpgradedEvent
    extends DvsEvent
{

    @XmlElement(required = true)
    protected DistributedVirtualSwitchProductSpec productInfo;

    /**
     * Gets the value of the productInfo property.
     * 
     * @return
     *     possible object is
     *     {@link DistributedVirtualSwitchProductSpec }
     *     
     */
    public DistributedVirtualSwitchProductSpec getProductInfo() {
        return productInfo;
    }

    /**
     * Sets the value of the productInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link DistributedVirtualSwitchProductSpec }
     *     
     */
    public void setProductInfo(DistributedVirtualSwitchProductSpec value) {
        this.productInfo = value;
    }

}