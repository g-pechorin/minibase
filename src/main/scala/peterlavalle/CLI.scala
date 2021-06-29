package peterlavalle

import java.io.File

trait CLI {

	final def main(args: Array[String]): Unit = main.call(args)

	def main: cli

	implicit class stringExtension(name: String) {
		def env[V: Parse](value: => V): Arg[V] =
			new Arg[V](name, () => value, true)

		def arg[V: Parse](value: => V): Arg[V] =
			new Arg[V](name, () => value, false)
	}

	implicit object parseBoolean extends Parse[Boolean] {
		override def parse(text: String): Boolean = text.toBoolean
	}

	implicit object parseInt extends Parse[Int] {
		override def parse(text: String): Int = text.toInt
	}

	implicit object parseFile extends Parse[File] {
		override def parse(text: String): File = new File(text)
	}

	implicit protected class cli private[CLI](private[CLI] val call: Array[String] => Unit)

	trait Parse[V] {
		def parse(text: String): V

		TODO("do some funky loading here so that boolean is a thing easier")
	}

	protected class Arg[V: Parse] private[CLI](name: String, value: () => V, env: Boolean) {

		private implicit class extensionArrayString(args: Array[String]) {
			def opt(name: String): Option[String] =
				args
					.find((_: String).startsWith(name + "="))
					.map((_: String).drop((name + "=").length))
		}

		implicit class extensionF[O](f: V => O) {
			def cure: Array[String] => O =
				(args: Array[String]) =>
					f(
						args
							.opt(name)
							.orElse {
								System
									.getProperties.toList
									.mapl((_: AnyRef).toString)
									.mapr((_: AnyRef).toString)
									.find((_: (String, String))._1 == name)
									.map((_: (String, String))._2)
							}
							.orElse {
								if (!env)
									None
								else
									System
										.getenv().toList
										.find((_: (String, String))._1 == name)
										.map((_: (String, String))._2)
							}
							.map(implicitly[Parse[V]].parse)
							.getOrElse {
								value()
							}
					)
		}

		def flatMap(f: V => cli): cli = (args: Array[String]) => f.cure(args).call(args)

		def map(f: V => Unit): cli = f.cure

	}

}

