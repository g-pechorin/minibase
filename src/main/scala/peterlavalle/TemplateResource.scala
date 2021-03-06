package peterlavalle

import java.io.InputStream
import java.util

import scala.io.{BufferedSource, Source}
import scala.reflect.ClassTag
import scala.util.matching.Regex

trait TemplateResource {
	lazy val rSlot: Regex = {
		val rSlot: Regex = ".*(<@([\\w\\-]*)/>).*".r

		"package <@pack/> {" match {
			case rSlot("<@pack/>", "pack") =>
				rSlot
		}
	}

	lazy val rLineComment: Regex = {
		val rLineComment: Regex = "(.*)<@--.*?--/>(.*)".r

		require(
			"fooworld" == ("foo<@--bar--/>world" replaceAll(rLineComment.regex, "$1$2"))
		)
		rLineComment
	}

	def link[T <: AnyRef](data: String => AnyRef)(implicit tTag: ClassTag[T]): Link =
		LinkBind(tTag.runtimeClass.getSimpleName, data)

	def data(name: String)(data: String => AnyRef): InputStream = {
		new InputStream {
			lazy val bound: Iterator[Byte] = {
				for {
					line <- bind(name)(data).map((_: String) + "\n")
					char <- line.getBytes("UTF-8")
				} yield {
					char
				}
			}.iterator

			override def read(): Int =
				if (bound.hasNext)
					bound.next().toInt
				else
					-1
		}
	}

	def bind(name: String)(data: String => AnyRef): Stream[String] = {

		val wrap: String => Stream[String] = {

			val cache = new util.HashMap[String, Stream[String]]()


			implicit class PiHashMap[K, V](map: util.HashMap[K, V]) {
				def opt(key: K)(value: => V): V =
					if (map containsKey key)
						map get key
					else {
						map put(key, value)
						map get key
					}
			}

			(key: String) =>
				cache.opt(key) {

					def unravel(value: AnyRef): Stream[String] =
						value match {
							case text: String =>
								Stream(text)

							case list: Iterable[_] =>
								list.toStream.flatMap {
									case ref: AnyRef =>
										unravel(ref)
									case _ =>
										error("this was impossible?")
								}

							case LinkBind(simpleName: String, sub) =>
								bind(
									name
										.reverse
										.dropWhile('.' != (_: Char))
										.reverse + simpleName + '.' + name
										.reverse
										.takeWhile('.' != (_: Char))
										.reverse
								) {
									key: String =>
										sub(key) ifNull data(key)
								}

							case failed =>
								sys.error(
									s"failed to unravel $key `$failed`"
								)
						}

					unravel(data(key))
				}
		}

		def loop(todo: Stream[String]): Stream[String] =
			todo match {
				case rLineComment(l, r) #:: tail =>
					loop(
						(l + r) #:: tail
					)

				case (line@rSlot(slot, name)) #:: tail =>
					loop(
						wrap(name).map {
							text: String =>
								line.replace(slot, text)
						} ++ tail
					)
				case head #:: tail =>
					head #:: loop(tail)
				case Stream() =>
					Stream()
			}

		loop {

			def classes(clazz: Class[_]): Stream[Class[_]] = {
				if (null == clazz || classOf[Object] == clazz)
					Stream()
				else {
					val supe: Class[_] = clazz.getSuperclass
					val tail: Stream[Class[_]] = clazz.getInterfaces.toStream

					val more = (supe #:: tail)

					clazz #:: (more flatMap classes)
				}
			}

			classes(getClass)
				.map {
					clazz =>
						clazz getResourceAsStream (clazz.getSimpleName.reverse.dropWhile('$' == (_: Char)).reverse + '.' + name)
				}
				.filter(null != _) match {
				case Stream(resourceStream: InputStream) =>
					require(
						null != resourceStream,
						s"cannot find resource `???.$name`"
					)
					try {
						val bufferedSource: BufferedSource = Source.fromInputStream(resourceStream)
						try {
							bufferedSource.mkString.splitLines
						} finally {
							bufferedSource.close()
						}
					} finally {
						resourceStream.close()
					}
			}

		}

	}

	sealed trait Link

	private case class LinkBind(simpleName: String, data: String => AnyRef) extends Link

}
