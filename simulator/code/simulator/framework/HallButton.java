package simulator.framework;

import simulator.payloads.HallCallPayload.WriteableHallCallPayload;
import simulator.payloads.HallLightPayload.ReadableHallLightPayload;

class HallButton extends Button
{
  private ReadableHallLightPayload light;
  private WriteableHallCallPayload call;

  public HallButton(boolean verbose, WriteableHallCallPayload call, ReadableHallLightPayload light)
  {
      super(verbose);
      this.call = call;
      this.light = light;
  }

  @Override
  boolean isLit()
  {
      return light.lighted();
  }

  @Override
  void press()
  {
      call.set(true);
  }

  @Override
  void release()
  {
      call.set(false);
  }
  
  @Override
  public String toString()
  {
      return "HallButton"  + ReplicationComputer.makeReplicationString(call.getFloor(), call.getHallway(), call.getDirection());
  }
}
