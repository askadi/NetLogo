// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.headless

import org.scalatest.FunSuite
import org.nlogo.compiler.Compiler
import org.nlogo.core.TokenizerInterface
import org.nlogo.util.Femto
import org.nlogo.api.NetLogoLegacyDialect
import org.nlogo.api.NetLogoThreeDDialect

class TestAllTokens extends FunSuite {
  val tokenMappers = Seq(
    NetLogoLegacyDialect.tokenMapper,
    NetLogoThreeDDialect.tokenMapper)
  tokenMappers.foreach { tokenMapper =>
    test("all listed primitives exist for " + tokenMapper.getClass.getSimpleName) {
      tokenMapper.allCommandNames.foreach(n => assert(tokenMapper.getCommand(n).nonEmpty))
      tokenMapper.allReporterNames.foreach(n => assert(tokenMapper.getReporter(n).nonEmpty))
    }
  }
}
