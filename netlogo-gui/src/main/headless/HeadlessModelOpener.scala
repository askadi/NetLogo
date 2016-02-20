// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.headless

import org.nlogo.agent.{BooleanConstraint, ChooserConstraint, InputBoxConstraint, NumericConstraint, SliderConstraint}
import org.nlogo.api.{ FileIO, LogoException, ModelReader, ModelSection,
                        NetLogoLegacyDialect, NetLogoThreeDDialect, SourceOwner, ValueConstraint, Version}
import org.nlogo.core.{ Button, CompilerException, ConstraintSpecification, LogoList, Model, Monitor, Program }
import org.nlogo.plot.PlotLoader
import org.nlogo.core.Shape.{ LinkShape => CoreLinkShape, VectorShape => CoreVectorShape }
import org.nlogo.shape.{LinkShape, VectorShape}
import org.nlogo.api.StringUtils.escapeString
import org.nlogo.api.PreviewCommands

import org.nlogo.shape.{ShapeConverter, LinkShape, VectorShape}

object HeadlessModelOpener {
  def protocolSection(path: String) =
    ModelReader.parseModel(FileIO.file2String(path)).get(ModelSection.BehaviorSpace).mkString("", "\n", "\n")
}

// this class is an abomination
// everything works off of side effects, asking the workspace to update something
// but not only that, some of the internals of this class work off side effects as well
// when they don't have to. - JC 10/27/09
class HeadlessModelOpener(ws: HeadlessWorkspace) {

  @throws(classOf[CompilerException])
  @throws(classOf[LogoException])
  def openFromModel(model: Model) {
    // get out if the model is opened. (WHY? - JC 10/27/09)
    if (ws.modelOpened) throw new IllegalStateException
    ws.modelOpened = true

    // get out if unknown version
    if (!Version.knownVersion(model.version))
      throw new IllegalStateException("unknown NetLogo version: " + model.version)

    // this should check model.version, but we aren't there yet
    val dialect = if (Version.is3D) NetLogoThreeDDialect else NetLogoLegacyDialect

    // read system dynamics modeler diagram
    val sdmLines = model.otherSections("org.nlogo.sdm")
    if (!sdmLines.isEmpty) ws.aggregateManager.load(sdmLines.mkString("", "\n", "\n"), ws)

    // read procedures, compile them.
    val results = {
      import collection.JavaConverters._

      val additionalSources: Seq[SourceOwner] = if (sdmLines.isEmpty) Seq() else Seq(ws.aggregateManager)
      val code = model.code
      val newProg = Program.fromDialect(dialect).copy(interfaceGlobals = model.interfaceGlobals)
      ws.compiler.compileProgram(code, additionalSources, newProg, ws.getExtensionManager, ws.getCompilationEnvironment)
    }
    ws.setProcedures(results.proceduresMap)
    ws.codeBits.clear() //(WTH IS THIS? - JC 10/27/09)

    // Read preview commands. If the model doesn't specify preview commands, the default ones will be used.
    val previewCommandsSource = model.previewCommands.mkString("\n")
    ws.previewCommands = PreviewCommands(previewCommandsSource)

    // parse turtle and link shapes, updating the workspace.
    attachWorldShapes(model.turtleShapes, model.linkShapes)

    ws.getHubNetManager.load(model.otherSections("org.nlogo.hubnet.client").toArray, model.version)

    ws.init()
    ws.world.program(results.program)

    // test code is mixed with actual code here, which is a bit funny.
    if (ws.compilerTestingMode)
      testCompileWidgets(model)
    else
      finish(model.constraints, results.program, model.interfaceGlobalCommands.mkString("\n"))
  }

  private def attachWorldShapes(turtleShapes: List[CoreVectorShape], linkShapes: List[CoreLinkShape]) = {
    import collection.JavaConverters._
    ws.world.turtleShapeList.replaceShapes(turtleShapes.map(ShapeConverter.baseVectorShapeToVectorShape))
    if (turtleShapes.isEmpty)
      ws.world.turtleShapeList.add(VectorShape.getDefaultShape)

    ws.world.linkShapeList.replaceShapes(linkShapes.map(ShapeConverter.baseLinkShapeToLinkShape))
    if (linkShapes.isEmpty)
      ws.world.linkShapeList.add(LinkShape.getDefaultLinkShape)
  }

  private def finish(constraints: Map[String, ConstraintSpecification], program: Program, interfaceGlobalCommands: String) {
    ws.world.realloc()

    val errors = ws.plotManager.compileAllPlots()
    if(errors.nonEmpty) throw errors(0)

    import ConstraintSpecification._
    for ((vname, spec) <- constraints) {
      val con: ValueConstraint = spec match {
        case NumericConstraintSpecification(default) => new NumericConstraint(default)
        case ChoiceConstraintSpecification(vals, defaultIndex) => new ChooserConstraint(
          LogoList.fromIterator(vals.iterator),
          defaultIndex
        )
        case BooleanConstraintSpecification(default) => new BooleanConstraint(default)
        case StringInputConstraintSpecification(typeName, default) => new InputBoxConstraint(typeName, default)
        case NumericInputConstraintSpecification(typeName, default) => new InputBoxConstraint(typeName, default)
      }
      ws.world.observer().variableConstraint(ws.world.observerOwnsIndexOf(vname.toUpperCase), con)
    }

    ws.command(interfaceGlobalCommands)
  }

  private def testCompileWidgets(model: Model) {
    val errors = ws.plotManager.compileAllPlots()
    if(errors.nonEmpty) throw errors(0)
    for (widget <- model.widgets)
      try {
        widget match {
          case b: Button  => ws.compileCommands(b.source)
          case m: Monitor => ws.compileReporter(m.source)
        }
      } catch {
        case ex: CompilerException =>
          println("compiling: \"" + widget + "\"")
          throw ex
      }
  }
}
