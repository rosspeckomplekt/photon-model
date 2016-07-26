
package com.vmware.vim25;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for VirtualEthernetCardLegacyNetworkDeviceName.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="VirtualEthernetCardLegacyNetworkDeviceName"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="bridged"/&gt;
 *     &lt;enumeration value="nat"/&gt;
 *     &lt;enumeration value="hostonly"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "VirtualEthernetCardLegacyNetworkDeviceName")
@XmlEnum
public enum VirtualEthernetCardLegacyNetworkDeviceName {

    @XmlEnumValue("bridged")
    BRIDGED("bridged"),
    @XmlEnumValue("nat")
    NAT("nat"),
    @XmlEnumValue("hostonly")
    HOSTONLY("hostonly");
    private final String value;

    VirtualEthernetCardLegacyNetworkDeviceName(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static VirtualEthernetCardLegacyNetworkDeviceName fromValue(String v) {
        for (VirtualEthernetCardLegacyNetworkDeviceName c: VirtualEthernetCardLegacyNetworkDeviceName.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}