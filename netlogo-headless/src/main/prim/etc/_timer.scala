// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.prim.etc

import org.nlogo.nvm.{ Context, Reporter }

class _timer extends Reporter {
  override def report(context: Context): java.lang.Double =
    Double.box(report_1(context))
  def report_1(context: Context): Double =
    world.timer.read
}
