// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.nvm

import org.nlogo.core.CompilationEnvironment
import scala.collection.Seq
import scala.Tuple2

import org.nlogo.agent.Agent
import org.nlogo.api.CommandRunnable
import org.nlogo.core.AgentKind
import org.nlogo.core.CompilerException
import org.nlogo.api.CompilerServices
import org.nlogo.api.HubNetInterface
import org.nlogo.api.ImporterUser
import org.nlogo.api.JobOwner
import org.nlogo.api.LogoException
import org.nlogo.api.OutputDestination
import org.nlogo.api.PreviewCommands
import org.nlogo.api.RandomServices
import org.nlogo.api.ReporterRunnable
import org.nlogo.api.{ Workspace => APIWorkspace }

import java.util.{ Map => JMap, WeakHashMap }

trait Workspace extends APIWorkspace
  with ImporterUser
  with JobManagerOwner
  with CompilerServices
  with RandomServices {

  def world: org.nlogo.agent.World

  def getProcedures: JMap[String, Procedure]

  def setProcedures(procedures: JMap[String, Procedure]): Unit

  def aggregateManager: org.nlogo.api.AggregateManagerInterface

  def requestDisplayUpdate(force: Boolean): Unit

  def breathe(): Unit // called when engine comes up for air

  def joinForeverButtons(agent: org.nlogo.agent.Agent): Unit

  def addJobFromJobThread(job: Job): Unit

  def getExtensionManager: ExtensionManager

  def getCompilationEnvironment: CompilationEnvironment

  @throws(classOf[LogoException])
  def waitFor(runnable: CommandRunnable): Unit

  @throws(classOf[LogoException])
  def waitForResult[T](runnable: ReporterRunnable[T]): T

  @throws(classOf[java.io.IOException])
  def importWorld(reader: java.io.Reader): Unit

  @throws(classOf[java.io.IOException])
  def importWorld(path: String): Unit

  @throws(classOf[java.io.IOException])
  def importDrawing(path: String): Unit

  def clearDrawing(): Unit

  @throws(classOf[java.io.IOException])
  def exportDrawing(path: String, format: String): Unit

  @throws(classOf[java.io.IOException])
  def exportView(path: String, format: String): Unit

  def exportView: java.awt.image.BufferedImage

  @throws(classOf[java.io.IOException])
  def exportInterface(path: String): Unit

  @throws(classOf[java.io.IOException])
  def exportWorld(path: String): Unit

  @throws(classOf[java.io.IOException])
  def exportWorld(writer: java.io.PrintWriter): Unit

  @throws(classOf[java.io.IOException])
  def exportOutput(path: String): Unit

  @throws(classOf[java.io.IOException])
  def exportPlot(plotName: String, path: String): Unit

  @throws(classOf[java.io.IOException])
  def exportAllPlots(path: String): Unit

  def inspectAgent(agent: org.nlogo.api.Agent, radius: Double): Unit

  def inspectAgent(agentClass: AgentKind, agent: org.nlogo.agent.Agent, radius: Double): Unit

  def stopInspectingAgent(agent: org.nlogo.agent.Agent): Unit

  def stopInspectingDeadAgents(): Unit

  def getAndCreateDrawing(): java.awt.image.BufferedImage

  def getHubNetManager: HubNetInterface

  @throws(classOf[LogoException])
  def waitForQueuedEvents(): Unit

  @throws(classOf[LogoException])
  def outputObject(obj: AnyRef, owner: AnyRef, addNewline: Boolean, readable: Boolean, destination: OutputDestination): Unit

  def clearOutput(): Unit

  @throws(classOf[LogoException])
  def clearAll(): Unit

  @throws(classOf[CompilerException])
  def compileForRun(source: String, context: Context, reporter: Boolean): Procedure

  @throws(classOf[java.io.IOException])
  def convertToNormal(): String

  def getModelPath: String

  def setModelPath(path: String): Unit

  def getModelDir: String

  def getModelFileName: String

  def fileManager: FileManager

  // kludgy this is AnyRef, but we don't want to have a compile-time dependency
  // on org.nlogo.plot. - ST 2/12/08
  def plotManager: AnyRef

  def updatePlots(c: Context): Unit

  def setupPlots(c: Context): Unit

  def previewCommands: PreviewCommands

  def tick(c: Context, originalInstruction: Instruction): Unit

  def resetTicks(c: Context): Unit

  def clearTicks(): Unit

  @throws(classOf[java.net.MalformedURLException])
  def attachModelDir(filePath: String): String

  @throws(classOf[CompilerException])
  def evaluateCommands(owner: JobOwner, source: String): Unit

  @throws(classOf[CompilerException])
  def evaluateCommands(owner: JobOwner, source: String, waitForCompletion: Boolean): Unit

  @throws(classOf[CompilerException])
  def evaluateCommands(owner: JobOwner, source: String, agent: org.nlogo.agent.Agent, waitForCompletion: Boolean): Unit

  @throws(classOf[CompilerException])
  def evaluateCommands(owner: JobOwner, source: String, agents: org.nlogo.agent.AgentSet, waitForCompletion: Boolean): Unit

  @throws(classOf[CompilerException])
  def evaluateReporter(owner: JobOwner, source: String): AnyRef

  @throws(classOf[CompilerException])
  def evaluateReporter(owner: JobOwner, source: String, agent: org.nlogo.agent.Agent): AnyRef

  @throws(classOf[CompilerException])
  def evaluateReporter(owner: JobOwner, source: String, agents: org.nlogo.agent.AgentSet): AnyRef

  @throws(classOf[CompilerException])
  def compileCommands(source: String): Procedure

  @throws(classOf[CompilerException])
  def compileCommands(source: String, agentClass: AgentKind): Procedure

  @throws(classOf[CompilerException])
  def compileReporter(source: String): Procedure

  def runCompiledCommands(owner: JobOwner, procedure: Procedure): Boolean

  def runCompiledReporter(owner: JobOwner, procedure: Procedure): AnyRef

  @throws(classOf[InterruptedException])
  def dispose(): Unit

  def patchSize: Double

  def changeTopology(wrapX: Boolean, wrapY: Boolean): Unit

  @throws(classOf[java.io.IOException])
  @throws(classOf[CompilerException])
  @throws(classOf[LogoException])
  def open(modelPath: String): Unit

  @throws(classOf[CompilerException])
  @throws(classOf[LogoException])
  def openString(modelContents: String) : Unit

  def magicOpen(name: String): Unit

  def changeLanguage(): Unit

  def startLogging(properties: String): Unit

  def zipLogFiles(filename: String): Unit

  def deleteLogFiles(): Unit

  def getIsApplet: Boolean

  def compiler: CompilerInterface

  def isHeadless: Boolean

  def behaviorSpaceRunNumber: Int

  def behaviorSpaceRunNumber(n: Int): Unit

  def behaviorSpaceExperimentName: String

  def behaviorSpaceExperimentName(name: String): Unit

  // for now this only works in HeadlessWorkspace, returns null in GUIWorkspace.
  // this whole error handling stuff is a complete mess and needs to be redone
  // for the next release after 4.1 - ST 3/10.09
  def lastLogoException: LogoException

  def clearLastLogoException: Unit

  def lastRunTimes: WeakHashMap[Job, WeakHashMap[Agent, WeakHashMap[Command, MutableLong]]] // for _every

  def completedActivations: WeakHashMap[Activation, Boolean] // for _thunkdidfinish

  def profilingEnabled: Boolean

  def profilingTracer: Tracer

  /*
   * Primarily for use by Custom Logging extension
   */
  def logCustomMessage(msg: String): Unit
  def logCustomGlobals(nameValuePairs: Seq[Tuple2[String, String]]): Unit
}
