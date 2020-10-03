import sbt.Keys.{name, shellPrompt}
import sbt._

import scala.Console

object ShellPrompt extends AutoPlugin {
  override val trigger = allRequirements

  override lazy val projectSettings =
    Seq(
      shellPrompt := { state =>
        val exerciseName = if(bookmarkFile.exists())
          Console.BLUE + IO.readLines(bookmarkFile).head + Console.RESET
        else
          Console.BLUE + name.value + Console.RESET

        val man = Console.RED + "man [e]" + Console.RESET

        val courseName = if(courseNameFile.exists())
          IO.readLines(courseNameFile).head
        else
          "project"

        s"$man > $courseName > $exerciseName > "
      }
    )

  private val bookmarkFile = new File(".bookmark")
  private val courseNameFile = new File(".courseName")
}

