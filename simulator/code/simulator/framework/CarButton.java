package simulator.framework;

import simulator.payloads.CarCallPayload.WriteableCarCallPayload;
import simulator.payloads.CarLightPayload.ReadableCarLightPayload;

class CarButton extends Button
{
  private ReadableCarLightPayload light;
  private WriteableCarCallPayload call;

  public CarButton(boolean verbose, WriteableCarCallPayload call, ReadableCarLightPayload light)
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
      return "CarButton" + ReplicationComputer.makeReplicationString(call.getFloor(), call.getHallway());
  }
}
