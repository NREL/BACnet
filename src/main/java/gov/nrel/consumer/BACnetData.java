package gov.nrel.consumer;

class BACnetData
{
  public BACnetData(com.serotonin.bacnet4j.type.primitive.ObjectIdentifier oid, com.serotonin.bacnet4j.type.Encodable value, int instanceNumber,
      long curTime)
  {
    this.oid = oid;
    this.value = value;
    this.instanceNumber = instanceNumber;
    this.curTime = curTime;
  }


  public int instanceNumber;
  public com.serotonin.bacnet4j.type.primitive.ObjectIdentifier oid;
  public com.serotonin.bacnet4j.type.Encodable value;
  public long curTime;

}

