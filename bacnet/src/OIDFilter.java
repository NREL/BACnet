import java.util.regex.Pattern;

import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

public class OIDFilter {
    String objectType;
    String instanceNumber;
    int timeBetweenScans;
    String apiKey;

    public boolean matches(ObjectIdentifier t_oid)
    {
      String strObjectType = t_oid.getObjectType().toString();
      String strInstanceNumber = Integer.toString(t_oid.getInstanceNumber());

      return Pattern.matches(objectType, strObjectType)
        && Pattern.matches(instanceNumber, strInstanceNumber);
    }
  }