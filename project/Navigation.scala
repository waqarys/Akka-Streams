import sbt._
import complete._
import DefaultParsers._
import sbt.Keys.commands

object Navigation extends AutoPlugin {

  override val trigger = allRequirements

  override lazy val globalSettings =
    Seq(
      commands in Global ++= Seq(
        prevExerciseCommand,
        nextExerciseCommand,
        gotoExerciseCommand,
        gotoFirstExerciseCommand,
        pullSolutionCommand,
        pullTemplateCommand,
        listExercisesCommand,
        showExerciseCommand
      )
    )

  private val solutionDir = new File(".cue/")
  private val bookmarkFile = new File(".bookmark")
  private val testPath = "src/test"
  private val multiJVMPath = "src/multi-jvm"
  private val mainPath = "src/main"
  private val exerciseList = if(solutionDir.exists())
    solutionDir.list().filter(_.contains("exercise_")).sorted.toList
  else
    List()

  private val exerciseParser: Parser[String] = Space ~> StringBasic.examples(FixedSetExamples((0 until exerciseList.size).map(_.toString)))
  private val basicParser = Space ~> StringBasic

  createBookmark()

  private val gotoExerciseHelp = Help(
    "gotoExerciseNr",
    ("gotoExerciseNr <exercise number>", "Moves to the specified exercise, bringing tests into scope."),
    "Moves to the specified exercise, bringing tests into scope."
  )

  private val gotoExerciseCommand = Command("gotoExerciseNr", gotoExerciseHelp)(state => exerciseParser) {
    case (state, arg) =>
      val exercise = exerciseList(arg.toInt)
      copyTests(exercise)
      println(s"Moved to $exercise")
      state
  }

  private val gotoFirstExerciseHelp = Help(
    "gotoFirstExercise",
    ("gotoFirstExercise", "Moves to the first exercise, bringing tests into scope."),
    "Moves to the first exercise, bringing tests into scope."
  )

  private val gotoFirstExerciseCommand = Command.command("gotoFirstExercise", gotoFirstExerciseHelp) {
    state =>
      val exercise = exerciseList.head
      copyTests(exercise)
      println("Moved to first exercise in course")
      state
  }

  private val nextExerciseHelp = Help(
    "nextExercise",
    ("nextExercise", "Brings new tests into scope for the next exercise."),
    "Brings new tests into scope for the next exercise."
  )

  private val nextExerciseCommand = Command.command("nextExercise", nextExerciseHelp) {
    state =>
      val exercise = nextExercise()

      if(currentExercise() == exerciseList.last) {
        println("[WARNING] You're already at the last exercise")
      } else {
        copyTests(exercise)
        println(s"Moved to $exercise")
      }
      state
  }

  private val prevExerciseHelp = Help(
    "prevExercise",
    ("prevExercise", "Brings new tests into scope for the previous exercise."),
    "Brings new tests into scope for the previous exercise."
  )

  private val prevExerciseCommand = Command.command("prevExercise", prevExerciseHelp) {
    state =>
      val exercise = prevExercise()

      if(currentExercise() == exerciseList.head) {
        println("[WARNING] You're already at the first exercise")
      } else {
        copyTests(exercise)
        println(s"Moved to $exercise")
      }
      state
  }

  private val pullSolutionHelp = Help(
    "pullSolution",
    ("pullSolution", "Pulls the solution for the current exercise. NOTE: This will overwrite your code!!!"),
    "Pulls the solution for the current exercise. NOTE: This will overwrite your code!!!"
  )

  private val pullSolutionCommand = Command.command("pullSolution", pullSolutionHelp) {
    state =>
      val exercise = currentExercise()
      copySolution(exercise)
      println(s"Pulled solution for $exercise")
      state
  }

  private val pullTemplateHelp = Help(
    "pullTemplate <template file>",
    ("pullTemplate", "Pulls the specified file from the current exercise solution. NOTE: This will overwrite any of your code in the named file!!!"),
    "Pulls the specified file from the current exercise solution. NOTE: This will overwrite any of your code in the named file!!!"
  )

  private val pullTemplateCommand = Command("pullTemplate", pullTemplateHelp)(state => basicParser) {
    (state, file) =>
      val exercise = currentExercise()
      copySolutionFile(exercise, file)
      println(s"Pulled template for $file from $exercise")
      state
  }

  private val listExercisesHelp = Help(
    "listExercises",
    ("listExercises", "Displays a list of all exercises."),
    "Displays a list of all exercises."
  )

  private val listExercisesCommand = Command.command("listExercises", listExercisesHelp) {
    state =>
      println(exerciseList.mkString("\n"))
      state
  }

  private val showExerciseHelp = Help(
    "showExerciseID",
    ("showExerciseID", "Displays the current exercise."),
    "Displays the current exercise."
  )

  private val showExerciseCommand = Command.command("showExerciseID", showExerciseHelp) {
    state =>
      println(currentExercise())
      state
  }

  private def currentExercise(): String = {
    IO.readLines(bookmarkFile).head
  }

  private def nextExercise(): String = {
    val nextIndex = math.min(exerciseList.size - 1, exerciseList.indexOf(currentExercise()) + 1)
    exerciseList(nextIndex)
  }

  private def prevExercise(): String = {
    val prevIndex = math.max(0, exerciseList.indexOf(currentExercise()) - 1)
    exerciseList(prevIndex)
  }

  private def createBookmark(): Unit = {
    if(!bookmarkFile.exists()) {
      if(exerciseList.nonEmpty)
        writeBookmark(exerciseList.head)
    }
  }

  private def writeBookmark(exercise: String): Unit = {
    IO.writeLines(bookmarkFile, Seq(exercise))
  }

  private def copyTests(exercise: String): Unit = {
    val exerciseDir = new File(solutionDir, exercise)

    if(exerciseDir.exists()) {
      val testSource = new File(exerciseDir, testPath)
      val testDestination = new File(s"exercises/$testPath")

      IO.delete(testDestination)
      IO.copyDirectory(testSource, testDestination)

      val multiJVMSource = new File(exerciseDir, multiJVMPath)
      val multiJVMDestination = new File(s"exercises/$multiJVMPath")

      IO.delete(multiJVMDestination)

      if (multiJVMSource.exists()) {
        IO.copyDirectory(multiJVMSource, multiJVMDestination)
      }

      writeBookmark(exercise)
    } else {
      println(s"${exerciseDir.getAbsolutePath} folder not found.")
    }
  }

  private def copySolutionFile(exercise: String, file: String): Unit = {
    val exerciseDir = new File(solutionDir, exercise)

    if(exerciseDir.exists()) {
      val solutionSource = new File(exerciseDir, mainPath)
      val solutionDestination = new File(s"exercises/$mainPath")

      IO.copyFile(new File(solutionSource, file), new File(solutionDestination, file))
      IO.copyDirectory(solutionSource, solutionDestination)
    } else {
      println(s"${exerciseDir.getAbsolutePath} folder not found.")
    }
  }

  private def copySolution(exercise: String): Unit = {
    val exerciseDir = new File(solutionDir, exercise)

    if(exerciseDir.exists()) {
      val solutionSource = new File(exerciseDir, mainPath)
      val solutionDestination = new File(s"exercises/$mainPath")

      IO.delete(solutionDestination)
      IO.copyDirectory(solutionSource, solutionDestination)
    } else {
      println(s"${exerciseDir.getAbsolutePath} folder not found.")
    }
  }
}

