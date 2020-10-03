import sbt.Keys._
import sbt._
import complete._
import DefaultParsers._

import scala.Console
import scala.util.matching._

object Man extends AutoPlugin {
  override val trigger = allRequirements

  override lazy val globalSettings =
    Seq(
      commands in Global ++= Seq(command)
    )

  private val mainReadme = "README.md"
  private val exerciseReadme = "exercises/src/test/resources/README.md"

  private val manHelp = Help(
    "man",
    ("man <e>", "Displays the README.md file. Use <noarg> for setup README.md or <e> for exercise README.md"),
    "Displays the README.md file. Use <noarg> for setup README.md or <e> for exercise README.md"
  )

  private val optionParser = OptSpace ~> StringBasic.?.examples(FixedSetExamples(List("e")))

  private def command: Command = Command("man", manHelp)(_ => optionParser) {
    case (state, Some("e")) =>
      printManPage(exerciseReadme)
      state
    case (state, None) =>
      printManPage(mainReadme)
      state
    case (state, Some(other)) =>
      println(s"invalid argument $other")
      state
  }

  private val bulletRx: Regex = """- """.r
  private val boldRx: Regex = """(\*\*)(\w*)(\*\*)""".r
  private val codeRx: Regex = """(`)([^`]+)(`)""".r
  private val startCodeBlockRx: Regex = """^```(bash|scala|java)$""".r
  private val endCodeBlockRx: Regex = """^```$""".r
  private val numberRx: Regex = """^(\d{1,3})(\. )""".r
  private val urlRx: Regex = """(\()(htt[a-zA-Z0-9\-\.\/:]*)(\))""".r
  private val green = Console.GREEN
  private val magenta = Console.MAGENTA
  private val red = Console.RED
  private val yellow = Console.YELLOW
  private val reset = Console.RESET

  private def printManPage(path: String) {
    var inCodeBlock = false

    IO.readLines(new sbt.File(path)) foreach {
      case line if !inCodeBlock && line.startsWith("#") =>
        Console.println(red + line + reset)

      case line if !inCodeBlock && line.matches(".*" + bulletRx.toString() + ".*") =>
        val coloredLine = bulletRx.replaceAllIn(line, red + bulletRx.toString() + reset)
        Console.println(formatLine(coloredLine))

      case line if !inCodeBlock && line.matches(numberRx.toString() + ".*") =>
        val coloredLine = numberRx.replaceAllIn(line, _ match { case numberRx(n, s) => f"$red$n$s$reset" })
        Console.println(formatLine(coloredLine))

      case line if line.matches(startCodeBlockRx.toString()) =>
        inCodeBlock = true
        Console.print(green)

      case line if line.matches(endCodeBlockRx.toString()) =>
        inCodeBlock = false
        Console.print(reset)

      case line =>
        Console.println(formatLine(line))
    }
  }

  private def formatLine(line: String) = {
    rxFormat(rxFormat(rxFormat(line, codeRx, green), boldRx, yellow), urlRx, magenta, keepWrapper = true)
  }

  private def rxFormat(
    line: String,
    regex: Regex,
    startColor: String,
    keepWrapper: Boolean = false
  ): String = {
    line match {
      case `line` if line.matches(".*" + regex.toString + ".*") =>
        regex replaceAllIn (line, _ match {
          case regex(start, in, stop) =>
            if (keepWrapper)
              f"$start$startColor$in$reset$stop"
            else
              f"$startColor$in$reset"
        })
      case _ =>
        line
    }
  }

}

