
package com.vmware.vim25;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfVirtualMachineCpuIdInfoSpec complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfVirtualMachineCpuIdInfoSpec"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="VirtualMachineCpuIdInfoSpec" type="{urn:vim25}VirtualMachineCpuIdInfoSpec" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfVirtualMachineCpuIdInfoSpec", propOrder = {
    "virtualMachineCpuIdInfoSpec"
})
public class ArrayOfVirtualMachineCpuIdInfoSpec {

    @XmlElement(name = "VirtualMachineCpuIdInfoSpec")
    protected List<VirtualMachineCpuIdInfoSpec> virtualMachineCpuIdInfoSpec;

    /**
     * Gets the value of the virtualMachineCpuIdInfoSpec property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the virtualMachineCpuIdInfoSpec property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getVirtualMachineCpuIdInfoSpec().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link VirtualMachineCpuIdInfoSpec }
     * 
     * 
     */
    public List<VirtualMachineCpuIdInfoSpec> getVirtualMachineCpuIdInfoSpec() {
        if (virtualMachineCpuIdInfoSpec == null) {
            virtualMachineCpuIdInfoSpec = new ArrayList<VirtualMachineCpuIdInfoSpec>();
        }
        return this.virtualMachineCpuIdInfoSpec;
    }

}