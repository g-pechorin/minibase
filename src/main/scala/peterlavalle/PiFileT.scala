package peterlavalle

import java.io._

import scala.io.{BufferedSource, Source}


trait PiFileT {

	object Folder {
		def unapply(any: Any): Option[File] = {
			any match {
				case folder: File if folder.isDirectory =>
					Some(folder)
				case _ =>
					None
			}
		}
	}

	implicit class PiFile(from: File) {

		val AbsoluteFile: File = from.getAbsoluteFile

		def <<(input: InputStream) = PiOutputStream(new FileOutputStream(AbsoluteFile.EnsureParent)) << input

		def EnsureParent: File = {
			require(ParentFile.EnsureMkDirs.exists())
			AbsoluteFile
		}

		def EnsureMkDirs: File = {
			if (!(AbsoluteFile.isDirectory || AbsoluteFile.mkdirs()))
				require(AbsoluteFile.isDirectory || AbsoluteFile.mkdirs())
			AbsoluteFile
		}

		def ParentFile: File = AbsoluteFile.getParentFile.getAbsoluteFile

		def ls: Stream[File] =
			if (!AbsoluteFile.isDirectory)
				null
			else
				AbsoluteFile
					.listFiles()
					.toStream
					.sortBy {
						file: File =>
							(file.isFile, file.getName)
								.toString()
					}

		def **(p: String => Boolean): Stream[String] = {

			def loop(root: File): Stream[String] = {

				require(!root.isFile)

				require(root.isDirectory == root.exists())

				val list: Stream[String] =
					root
						.list() match {
						case null => Stream.empty
						case list => list.toStream
					}

				list.filter((p: String) => (root / p).isFile) ++ {
					list.map((name: String) => name -> root / name)
						.filterNot((_: (String, File))._2.isFile)
						.flatMap {
							case (name, file) =>
								loop(file.AbsoluteFile).map(name + '/' + (_: String))
						}
				}
			}

			loop(AbsoluteFile)
				.filter(p)
		}

		def ioWriteLines(text: String): File =
			ioWriteLines(text.splitLines)

		def ioWriteLines(lines: Seq[String]): File = {
			val file = AbsoluteFile

			require(file.exists() == file.isFile)

			lines.foldLeft(
				new FileWriter(file.EnsureParent).asInstanceOf[Writer]
			)((_: Writer) append (_: String) append "\n")
				.close()


			file
		}

		def isEmpty: Boolean = {
			require(AbsoluteFile.isDirectory)
			AbsoluteFile.list().isEmpty
		}

		def extend(f: String => String): File = ParentFile / f(AbsoluteFile.getName)

		def /(path: String): File =
			if (path.startsWith("../"))
				AbsoluteFile.ParentFile / path.drop(3)
			else
				new File(AbsoluteFile, path).getAbsoluteFile

		def reWriteLine(regex: String)(make: String => String): E[File] =
			Source.fromFile(AbsoluteFile).using {
				src: BufferedSource =>

					lazy val mkString: String = src.mkString

					@scala.annotation.tailrec
					def loop(todo: Stream[String], done: List[String]): E[File] = {
						todo match {
							case Stream() =>
								E ! s"no lines match `$regex`"

							case line #:: tail if line matches regex =>
								if (tail.exists((_: String) matches regex))
									E ! s"multiple lines match `$regex`"
								else {
									tail.foldLeft {
										done
											.reverse
											.foldLeft(new FileWriter(AbsoluteFile): Writer)((_: Writer) append (_: String) append "\n")
											.append(make(line)).append("\n")
									}((_: Writer) append (_: String) append "\n").close()
									E(AbsoluteFile)
								}

							case head #:: tail =>
								loop(
									tail,
									head :: done
								)
						}
					}

					loop(
						mkString.splitLines,
						Nil
					)
			}

		def FreshFolder: File = {
			require(!AbsoluteFile.isFile)
			if (AbsoluteFile.exists())
				AbsoluteFile.listFiles().foreach(_.Unlink)
			AbsoluteFile.EnsureMkDirs
		}

		def Unlink: Unit = {
			if (AbsoluteFile.isDirectory)
				AbsoluteFile.listFiles()
					.foreach(_.Unlink)
			if (AbsoluteFile.exists())
				require(
					AbsoluteFile.delete() || !AbsoluteFile.exists(),
					"failed to delete " + AbsolutePath
				)
		}

		def AbsolutePath: String = AbsoluteFile.getAbsolutePath.replace('\\', '/')

		def * : Set[String] =
			AbsoluteFile.list() match {
				case null => Set()
				case list =>
					list.toSet
			}
	}

}
