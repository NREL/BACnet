/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2006-2011 Serotonin Software Technologies Inc. http://serotoninsoftware.com
 * @author Matthew Lohbihler
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * When signing a commercial license with Serotonin Software Technologies Inc.,
 * the following extension to GPL is made. A special exception to the GPL is 
 * included to allow you to distribute a combined work that includes BAcnet4J 
 * without being obliged to provide the source code for any proprietary components.
 */
package com.serotonin.bacnet4j.type.constructed;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.serotonin.bacnet4j.Network;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.util.IpAddressUtils;
import com.serotonin.util.queue.ByteQueue;

public class Address extends BaseType {
    private static final long serialVersionUID = -3376358193474831753L;
    private final Unsigned16 networkNumber;
    private final OctetString ipAddressAndPort;

    public Address(Unsigned16 networkNumber, OctetString macAddress) {
        this.networkNumber = networkNumber;
        this.ipAddressAndPort = macAddress;
    }

    public Address(byte[] ipAddress, int port) {
        this(null, ipAddress, port);
    }

    public Address(Network network, byte[] ipAddress, int port) {
        if (network == null)
            networkNumber = new Unsigned16(0);
        else
            networkNumber = new Unsigned16(network.getNetworkNumber());

        byte[] ipMacAddress = new byte[ipAddress.length + 2];
        System.arraycopy(ipAddress, 0, ipMacAddress, 0, ipAddress.length);
        ipMacAddress[ipAddress.length] = (byte) (port >> 8);
        ipMacAddress[ipAddress.length + 1] = (byte) port;
        ipAddressAndPort = new OctetString(ipMacAddress);
    }

    @Override
    public void write(ByteQueue queue) {
        write(queue, networkNumber);
        write(queue, ipAddressAndPort);
    }

    public Address(ByteQueue queue) throws BACnetException {
        networkNumber = read(queue, Unsigned16.class);
        ipAddressAndPort = read(queue, OctetString.class);
    }

    public OctetString getIpAddressAndPort() {
        return ipAddressAndPort;
    }

    public UnsignedInteger getNetworkNumber() {
        return networkNumber;
    }

    @Override
    public String toString() {
        return "Address(networkNumber=" + networkNumber + ", macAddress=" + ipAddressAndPort + ")";
    }

    public InetAddress getInetAddress() throws UnknownHostException {
        return InetAddress.getByAddress(getIpBytes());
    }

    public int getPort() {
        byte[] b = ipAddressAndPort.getBytes();
        if (b.length == 6)
            return ((b[4] & 0xff) << 8) | (b[5] & 0xff);
        return -1;
    }

    public String toIpString() {
        return IpAddressUtils.toIpString(getIpBytes());
    }

    public String toIpPortString() {
        return toIpString() + ":" + getPort();
    }

    private byte[] getIpBytes() {
        if (ipAddressAndPort.getLength() == 4)
            return ipAddressAndPort.getBytes();

        byte[] b = new byte[4];
        System.arraycopy(ipAddressAndPort.getBytes(), 0, b, 0, 4);
        return b;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((ipAddressAndPort == null) ? 0 : ipAddressAndPort.hashCode());
        result = PRIME * result + ((networkNumber == null) ? 0 : networkNumber.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Address other = (Address) obj;
        if (ipAddressAndPort == null) {
            if (other.ipAddressAndPort != null)
                return false;
        }
        else if (!ipAddressAndPort.equals(other.ipAddressAndPort))
            return false;
        if (networkNumber == null) {
            if (other.networkNumber != null)
                return false;
        }
        else if (!networkNumber.equals(other.networkNumber))
            return false;
        return true;
    }
}
