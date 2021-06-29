package peterlavalle

import org.scalatest.funsuite.AnyFunSuite

class PiIterableTest extends AnyFunSuite {

	test("drop same") {
		val left = "aabc".toIterable
		val right = "aa bc"

		assert {
			(left dropSame right) == (Stream('b', 'c'), Stream(' ', 'b', 'c'))
		}
	}

	test("starts with") {
		val left = "aabc".toIterable
		val right = "aa bc".toIterable

		assert {
			!(left leadBy right)
		}
		assert {
			!(right leadBy left)
		}
		assert {
			!(left leadBy right)
		}
		assert {
			(right leadBy left.take(2))
		}
	}

}
