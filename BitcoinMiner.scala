package bitcoinMiner

import akka.actor._
import akka.routing.RoundRobinRouter

object miner extends App {
 
	getCoins(nrOfWorkers = 4, nrOfZeroes = 3, coinsPerWorker = 3)

	sealed trait BitcoinMessage
	case object InitiateMiner extends BitcoinMessage
	case object StartMining extends BitcoinMessage
	case class Result(value: String) extends BitcoinMessage

	def hexBuilder(x: Int): String = {
		val sb = new StringBuilder(64)
		for (i <- 1 to x) {
			sb.append("0")
		}
		for (i <- x+1 to 64) {
			sb.append("F")
		}
		sb.toString
	}

	def MD5(s : String): String = {
		val m = java.security.MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"))
		m.map("%02x".format(_)).mkString
	}

	def randomString(len: Int): String = {
	    val rand = new scala.util.Random(System.nanoTime)
	    val sb = new StringBuilder(len)
	    val ab = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
	    for (i <- 0 until len) {
	      sb.append(ab(rand.nextInt(ab.length)))
	    }
	    sb.toString
	}

	class Worker(maxHexValue: String, coinsPerWorker: Int) extends Actor {

		def receive = {
			case StartMining =>
				for (i <- 0 until coinsPerWorker) {
					var hash :String = ""
					var input: String = ""
					do {
						input = "abpal;" + randomString(5)
						hash = MD5(input)
					}while(hash.compareToIgnoreCase(maxHexValue) > 0)
					sender() ! Result(input + " " + hash)
				}
		}

	}

	class Master(nrOfWorkers: Int, nrOfZeroes: Int, coinsPerWorker: Int) extends Actor {

		val maxHexValue = hexBuilder(nrOfZeroes)

		var coinsMined = 0

		val worker = context.actorOf(Props(new Worker(
			maxHexValue, coinsPerWorker)).withRouter(RoundRobinRouter(nrOfWorkers)),
			"worker")

		def receive = {
			case InitiateMiner =>
				for (i <- 0 until 25) worker ! StartMining
			case Result(value) =>
				coinsMined += 1
				println(value + " " + coinsMined)
				if (coinsMined == 50)
					context.system.shutdown()
		}

	}

	def getCoins(nrOfWorkers: Int, nrOfZeroes: Int, coinsPerWorker: Int) {

		val system = ActorSystem("BitcoinMinerSystem")

		val master = system.actorOf(Props(new Master(
			nrOfWorkers, nrOfZeroes, coinsPerWorker)),
			"master")

		master ! InitiateMiner

	}
}