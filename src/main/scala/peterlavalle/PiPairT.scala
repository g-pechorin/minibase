package peterlavalle

trait PiPairT {

	implicit class PiPair[L, R](pair: (L, R)) {
		def pass[O](f: (L, R) => O): O =
			f(pair._1, pair._2)
	}

}
