
package com.vmware.vim25;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for GuestRegValueDataSpec complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="GuestRegValueDataSpec"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:vim25}DynamicData"&gt;
 *       &lt;sequence&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GuestRegValueDataSpec")
@XmlSeeAlso({
    GuestRegValueDwordSpec.class,
    GuestRegValueQwordSpec.class,
    GuestRegValueStringSpec.class,
    GuestRegValueExpandStringSpec.class,
    GuestRegValueMultiStringSpec.class,
    GuestRegValueBinarySpec.class
})
public class GuestRegValueDataSpec
    extends DynamicData
{


}
