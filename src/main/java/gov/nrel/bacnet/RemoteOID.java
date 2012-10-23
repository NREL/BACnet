package gov.nrel.bacnet;

import com.serotonin.bacnet4j.Network;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class RemoteOID {
	int instanceNumber;
	UnsignedInteger networkNumber;
	OctetString macAddress;
	byte[] networkAddress;

	ObjectType objectType;
	int objectNumber;

	@Override
	public int hashCode() {
		return instanceNumber
				^ (networkNumber == null ? 0 : networkNumber.hashCode())
				^ (macAddress == null ? 0 : macAddress.hashCode())
				^ (networkAddress == null ? 0 : java.util.Arrays
						.hashCode(networkAddress))
				^ (objectType == null ? 0 : objectType.hashCode())
				^ objectNumber;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof RemoteOID)) {
			return false;
		}

		RemoteOID that = (RemoteOID) o;

		return instanceNumber == that.instanceNumber
				&& m_equals(networkNumber, that.networkNumber)
				&& m_equals(macAddress, that.macAddress)
				&& m_equals(networkAddress, that.networkAddress)
				&& m_equals(objectType, that.objectType)
				&& objectNumber == that.objectNumber;
	}

	private static <T> boolean m_equals(T t_lhs, T t_rhs) {
		if (t_lhs == null && t_rhs == null) {
			return true;
		}

		if (t_lhs == null || t_rhs == null) {
			return false;
		}

		return t_lhs.equals(t_rhs);
	}

	public RemoteOID(RemoteDevice t_rd, ObjectIdentifier t_oid) {
		instanceNumber = t_rd.getInstanceNumber();

		networkNumber = null;
		macAddress = null;
		networkAddress = null;

		Address a = t_rd.getAddress();

		if (a != null) {
			networkNumber = a.getNetworkNumber();
			macAddress = a.getIpAddressAndPort();
		}

		Network n = t_rd.getNetwork();

		if (n != null) {
			networkAddress = n.getNetworkAddress();
		}

		if (t_oid != null) {
			objectType = t_oid.getObjectType();
			objectNumber = t_oid.getInstanceNumber();
		} else {
			objectType = null;
			objectNumber = 0;
		}
	}
}