
package com.vmware.vim25;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DvsPortLinkUpEvent complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DvsPortLinkUpEvent"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:vim25}DvsEvent"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="portKey" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="runtimeInfo" type="{urn:vim25}DVPortStatus" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DvsPortLinkUpEvent", propOrder = {
    "portKey",
    "runtimeInfo"
})
public class DvsPortLinkUpEvent
    extends DvsEvent
{

    @XmlElement(required = true)
    protected String portKey;
    protected DVPortStatus runtimeInfo;

    /**
     * Gets the value of the portKey property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPortKey() {
        return portKey;
    }

    /**
     * Sets the value of the portKey property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPortKey(String value) {
        this.portKey = value;
    }

    /**
     * Gets the value of the runtimeInfo property.
     * 
     * @return
     *     possible object is
     *     {@link DVPortStatus }
     *     
     */
    public DVPortStatus getRuntimeInfo() {
        return runtimeInfo;
    }

    /**
     * Sets the value of the runtimeInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link DVPortStatus }
     *     
     */
    public void setRuntimeInfo(DVPortStatus value) {
        this.runtimeInfo = value;
    }

}
