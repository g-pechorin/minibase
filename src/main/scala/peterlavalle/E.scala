package peterlavalle

trait E[+V] {
	def flatMap[O](f: V => E[O]): E[O]

	def map[O](f: V => O): E[O]

	def nonEmpty: Boolean

	def value: V

	def |[Q >: V, R <: Q](r: => E[R]): E[Q]
}

object E {

	implicit class extension[A](data: Iterable[E[A]]) {
		def dove: E[List[A]] =
			if (data.isEmpty)
				E(List())
			else
				for {
					head <- data.head
					tail <- data.tail.dove
				} yield {
					head :: tail
				}
	}


	def !(message: String): E[Nothing] =
		E ! {
			// make an exception, but, remove this frame from it so that the one-line error shows where it was called
			val exception = new Exception(message)
			exception.setStackTrace(exception.getStackTrace.tail)
			exception
		}

	def !(exception: Exception): E[Nothing] = Failure(exception)

	def apply[O](o: O): E[O] = Success(o)

	def unapply[Q](arg: E[Q]): Option[Q] =
		arg match {
			case Success(q) =>
				Some(q)
			case _ =>
				None
		}

	case class Failure(exception: Exception) extends E[Nothing] {
		override def flatMap[O](f: Nothing => E[O]): E[O] = this

		override def map[O](f: Nothing => O): E[O] = this

		override def |[Q >: Nothing, R <: Q](r: => E[R]): E[Q] = r

		override def nonEmpty: Boolean = false

		override def value: Nothing = throw exception
	}

	case class Success[T](value: T) extends E[T] {
		override def flatMap[O](f: T => E[O]): E[O] = f(value)

		override def map[O](f: T => O): E[O] = E(f(value))

		override def |[Q >: T, R <: Q](r: => E[R]): E[Q] = this

		override def nonEmpty: Boolean = true
	}

}
