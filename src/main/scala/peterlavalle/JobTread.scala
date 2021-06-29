package peterlavalle

import java.util

class JobTread() extends AutoCloseable {
	private val todo = new util.LinkedList[Runnable]()
	private val thread: Thread =
		new Thread() {
			override def run(): Unit = {
				def next: Runnable =
					todo.synchronized {

						while (todo.isEmpty) {
							todo.wait()
						}

						if (null == todo.getFirst) {
							require(1 == todo.size())
							null
						} else {
							todo.removeFirst()
						}
					}

				next match {
					case null =>
					case next =>
						next.run()
						run()
				}
			}

			todo.synchronized {
				start()
			}
		}

	def !(action: => Unit): Unit = {
		todo.synchronized {
			todo.notify()
			require(todo.isEmpty || null != todo.getLast)
			todo.add(() => action)
		}
	}

	override def close(): Unit =
		todo.synchronized {
			todo.add(null)
			todo.notify()
			thread.join()
		}
}
