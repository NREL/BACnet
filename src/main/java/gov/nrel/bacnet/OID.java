package gov.nrel.bacnet;


import java.util.Vector;

import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

public class OID {
	public OID(ObjectIdentifier t_oid, String t_objectName, String t_presentValue,
			String t_units) {
		oid = t_oid;
		objectName = t_objectName;
		presentValue = t_presentValue;
		units = t_units;

		children = new Vector<OID>();
		trendLog = new Vector<TrendLogData>();
	}

	ObjectIdentifier oid;
	String objectName;
	String presentValue;
	String units;
	String apiKey;

	public Vector<TrendLogData> trendLog;

	public Vector<OID> children;
}