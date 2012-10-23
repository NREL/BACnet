package gov.nrel.bacnet;
import java.util.Vector;
import java.util.regex.Pattern;

import com.serotonin.bacnet4j.Network;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.type.constructed.Address;


public class DeviceFilter {
    String instanceNumber;
    String networkNumber;
    String macAddress;
    String networkAddress;

    public int timeBetweenScans;

    public Vector<OIDFilter> oidFilters;

    public boolean matches(RemoteDevice t_rd)
    {
      String strInstanceNumber = Integer.toString(t_rd.getInstanceNumber());
      String strNetworkNumber = "";
      String strMacAddress = "";
      String strNetworkAddress = "";


      Address a = t_rd.getAddress();

      if (a != null)
      {
        strNetworkNumber = a.getNetworkNumber().toString();
        strMacAddress = a.getIpAddressAndPort().toString();
      }

      Network n = t_rd.getNetwork();

      if (n != null)
      {
        strNetworkAddress = n.getNetworkAddress().toString();
      }


      return Pattern.matches(instanceNumber, strInstanceNumber)
        && Pattern.matches(networkNumber, strNetworkNumber)
        && Pattern.matches(macAddress, strMacAddress)
        && Pattern.matches(networkAddress, strNetworkAddress);

    }
  }