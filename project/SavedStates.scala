import sbt._
import complete._
import DefaultParsers._
import sbt.Keys.commands

object SavedStates extends AutoPlugin {

  override val trigger = allRequirements

  override lazy val globalSettings =
    Seq(
      commands in Global ++= Seq(
        saveStateCommand,
        restoreStateCommand,
        listStatesCommand
      )
    )

  private val bookmarkFile = new File(".bookmark")
  private val savedStatesDir = new File(".savedStates/")

  private val stateParser: Parser[String] = Space ~> StringBasic.examples(FixedSetExamples(statesList))

  private val saveStateHelp = Help(
    "saveState",
    ("saveState", "Save the current state of your exercise."),
    "Save the current state of your exercise."
  )

  private val saveStateCommand = Command.command("saveState", saveStateHelp) {
    state =>
      val exercise = currentExercise()
      saveState(exercise)
      println(s"Saved state for $exercise")
      state
  }

  private val restoreStateHelp = Help(
    "restoreState",
    ("restoreState", "Restore a previously saved exercise state."),
    "Restore a previously saved exercise state."
  )

  private val restoreStateCommand = Command("restoreState", restoreStateHelp) (state => stateParser) {
    case (state, exercise) =>
      restoreState(exercise)
      println(s"Restored state for $exercise")
      state
  }

  private val listStatesHelp = Help(
    "savedStates",
    ("savedStates", "List all saved states."),
    "List all saved states."
  )

  private val listStatesCommand = Command.command("savedStates", listStatesHelp) {
    state =>
      println(statesList.mkString("\n"))
      state
  }

  private def statesList = if(savedStatesDir.exists()) {
    savedStatesDir.list().sorted.toList
  } else {
    List()
  }

  private def currentExercise(): String = {
    IO.readLines(bookmarkFile).head
  }

  private def saveState(exercise: String): Unit = {
    val sourceDir = new File("exercises")
    val destDir = new File(savedStatesDir, exercise)

    IO.delete(destDir)
    IO.copyDirectory(sourceDir, destDir)
  }

  private def restoreState(exercise: String): Unit = {
    val sourceDir = new File(savedStatesDir, exercise)
    val destDir = new File("exercises")

    IO.delete(destDir)
    IO.copyDirectory(sourceDir, destDir)

    writeBookmark(exercise)
  }

  private def writeBookmark(exercise: String): Unit = {
    IO.writeLines(bookmarkFile, Seq(exercise))
  }
}

