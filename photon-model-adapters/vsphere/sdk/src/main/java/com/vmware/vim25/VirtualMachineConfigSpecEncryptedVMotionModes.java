
package com.vmware.vim25;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for VirtualMachineConfigSpecEncryptedVMotionModes.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="VirtualMachineConfigSpecEncryptedVMotionModes"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="disabled"/&gt;
 *     &lt;enumeration value="opportunistic"/&gt;
 *     &lt;enumeration value="required"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "VirtualMachineConfigSpecEncryptedVMotionModes")
@XmlEnum
public enum VirtualMachineConfigSpecEncryptedVMotionModes {

    @XmlEnumValue("disabled")
    DISABLED("disabled"),
    @XmlEnumValue("opportunistic")
    OPPORTUNISTIC("opportunistic"),
    @XmlEnumValue("required")
    REQUIRED("required");
    private final String value;

    VirtualMachineConfigSpecEncryptedVMotionModes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static VirtualMachineConfigSpecEncryptedVMotionModes fromValue(String v) {
        for (VirtualMachineConfigSpecEncryptedVMotionModes c: VirtualMachineConfigSpecEncryptedVMotionModes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
