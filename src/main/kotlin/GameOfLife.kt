import kotlin.random.Random

/* 0 0 0 0 | A */
/* 0 0 1 0 | A */
/* 0 1 0 0 | A */
/* 0 1 1 0 | A */

/* 0 0 0 1 | B */
/* 0 1 0 1 | B */
/* 1 0 0 1 | B */
/* 1 1 0 1 | B */

/* 0 0 1 1 | C */
/* 0 1 1 1 | C */
/* 1 0 1 1 | C */
/* 1 1 1 1 | C */

/* 1 0 0 0 | D */
/* 1 0 1 0 | D */
/* 1 1 0 0 | D */
/* 1 1 1 0 | D */

/** ----------------------- */

/* 0 0 0 0 | 0 */

/* 0 0 1 0 | 1 */
/* 0 1 0 0 | 1 */
/* 0 0 0 1 | 1 */
/* 1 0 0 0 | 1 */

/* 0 1 1 0 | 2 */
/* 0 1 0 1 | 2 */
/* 1 0 0 1 | 2 */
/* 0 0 1 1 | 2 */
/* 1 0 1 0 | 2 */
/* 1 1 0 0 | 2 */

/* 1 1 0 1 | 3 */
/* 0 1 1 1 | 3 */
/* 1 0 1 1 | 3 */
/* 1 1 1 0 | 3 */

/* 1 1 1 1 | 4 */

/** ----------------------- */

/* 0 0 0 0 | A */
/* 0 0 0 1 | B */
/* 0 0 1 0 | A */
/* 0 0 1 1 | C */
/* 0 1 0 0 | A */
/* 0 1 0 1 | B */
/* 0 1 1 0 | A */
/* 0 1 1 1 | C */
/* 1 0 0 0 | D */
/* 1 0 0 1 | B */
/* 1 0 1 0 | D */
/* 1 0 1 1 | C */
/* 1 1 0 0 | D */
/* 1 1 0 1 | B */
/* 1 1 1 0 | D */
/* 1 1 1 1 | C */

/*
 * actions
 * 0A: attack
 * 1B: defend
 * 2C: heal
 * 3D: spawn
 */

/*
 * 0 0 0 0 | header
 * 0 0 0 0 | value
 * 0 0 0 0 | header
 * 0 0 0 0 | value
 * 0 0 0 0 | header
 * 0 0 0 0 | value
 * 0 0 0 0 | header
 * 0 0 0 0 | value
 */

object BitUtil {
	fun get(code: Int, offset: Int, length: Int): Int {
		return code.ushr(offset).and(0x1.shl(length) - 1)
	}

	fun set(code: Int, value: Int, offset: Int, length: Int): Int {
		val max = (0x1.shl(length) - 1)
		return code.and(max.shl(offset).inv()).or(
			value.coerceIn(0..max).shl(offset)
		)
	}
}

/**
 * 0 | alive
 * ...
 * 0 0 0 0 0 0 0 0 | life
 * 0 0 0 0 0 0 0 0 | age
 */
object State {
	const val START_LIFE = 4
	const val MAX_LIFE = 32
	const val DEAD_AGE = 64

	fun getAlive(code: Int): Boolean {
		return BitUtil.get(code, 31, 1) == 1
	}

	fun getLife(code: Int): Int {
		return BitUtil.get(code, 8, 8)
	}
	fun setLife(code: Int, life: Int): Int {
		return BitUtil.set(code, life, 8, 8)
	}

	fun getAge(code: Int): Int {
		return BitUtil.get(code, 0, 8)
	}
	fun setAge(code: Int, age: Int): Int {
		return BitUtil.set(code, age, 0, 8)
	}

	fun initial(): Int {
		return START_LIFE.shl(8).or(0).or(1.shl(31))
	}
}

object Organism {
	const val ACTION_ATTACK = 0
	const val ACTION_DEFEND = 1
	const val ACTION_HEAL = 2
	const val ACTION_SPAWN = 3

	const val FAMILY_THRESHOLD = 0.75

	val headerMap = intArrayOf(
		/* 0 0 0 0 | A */ 0,
		/* 0 0 0 1 | B */ 1,
		/* 0 0 1 0 | A */ 0,
		/* 0 0 1 1 | C */ 2,
		/* 0 1 0 0 | A */ 0,
		/* 0 1 0 1 | B */ 1,
		/* 0 1 1 0 | A */ 0,
		/* 0 1 1 1 | C */ 2,
		/* 1 0 0 0 | D */ 3,
		/* 1 0 0 1 | B */ 1,
		/* 1 0 1 0 | D */ 3,
		/* 1 0 1 1 | C */ 2,
		/* 1 1 0 0 | D */ 3,
		/* 1 1 0 1 | B */ 1,
		/* 1 1 1 0 | D */ 3,
		/* 1 1 1 1 | C */ 2,
	)

	fun getHeader(code: Int, slot: Int) = headerMap[BitUtil.get(code, 8 * slot + 4, 4)]
	fun getTrigger(code: Int, slot: Int) = BitUtil.get(code, 8 * slot, 4).countOneBits() + 3

	fun getActions(code: Int, neighbors: Int): ArrayList<Int> {
		val actions = ArrayList<Int>(4)
		for (i in 0 until 4) {
			val action = getHeader(code, i)

			if (action != ACTION_SPAWN && neighbors == getTrigger(code, i))
				actions.add(action)
		}
		return actions
	}

	fun canSpawn(code: Int, neighbors: Int): Boolean {
		for (i in 0 until 4) {
			val action = getHeader(code, i)
			if (action == ACTION_SPAWN && neighbors == getTrigger(code, i)) return true
		}
		return false
	}

	fun getColor(code: Int): Int {
		val c = getColorChannel(code, 0)
		val m = getColorChannel(code, 1)
		val y = getColorChannel(code, 2)

		val r = (0xff * c).toInt()
		val g = (0xff * m).toInt()
		val b = (0xff * y).toInt()

		return 0xFF000000.toInt().or(r.shl(16)).or(g.shl(8)).or(b)
	}

	fun getColorChannel(code: Int, channel: Int): Float {
		val top = BitUtil.get(code, 8 * channel + 4, 4)
		val bottom = BitUtil.get(code, 8 * channel, 4)

		return (
			(top.ushr(3).and(0x1) + bottom.ushr(3).and(0x1)) * 27 +
			(top.ushr(2).and(0x1) + bottom.ushr(2).and(0x1)) * 9 +
			(top.ushr(1).and(0x1) + bottom.ushr(1).and(0x1)) * 3 +
			(top.and(0x1) + bottom.and(0x1))
		) / 80.0f
	}

	fun sameFamily(first: Int, second: Int): Boolean {
		return first.xor(second).inv().countOneBits() >= (FAMILY_THRESHOLD * 32).toInt()
	}

	/** @return indices */
	fun findSimilarPair(codes: List<Int>): Pair<Int, Int>? {
		if (codes.size < 2) return null

		val options = Array(codes.size) { i ->
			val list = ArrayList<Int>()

			for (j in codes.indices) {
				if (i == j) continue
				if (sameFamily(codes[i], codes[j])) list.add(j)
			}

			list
		}

		val indices = codes.indices.shuffled()
		for (i in indices.indices) {
			val firstIndex = indices[i]
			if (options[firstIndex].isEmpty()) continue

			return firstIndex to options[firstIndex].random()
		}

		return null
	}

	fun breed(parent0: Int, parent1: Int, mutationRate: Float): Int {
		var newCode = 0

		for (i in 0 until 28) {
			var bit = (if (Random.nextBoolean()) parent0 else parent1).shr(i).and(0x1)
			if (Random.nextFloat() < mutationRate) bit = bit.inv().and(0x1)
			newCode = newCode.or(bit.shl(i))
		}

		return newCode
	}
}

class Board(val width: Int, val height: Int) {
	companion object {
		val offsets = arrayOf(
			-1 to  0,
			 0 to -1,
			 1 to  0,
			 0 to  1,
			-1 to -1,
			 1 to  1,
			-1 to  1,
			 1 to -1,
		)
	}

	val genomeGrid = arrayOf(IntArray(width * height), IntArray(width * height))
	val stateGrid = arrayOf(IntArray(width * height), IntArray(width * height))

	val health = IntArray(width * height)

	var phase = 0
	fun otherPhase() = if (phase == 0) 1 else 0

	inline fun getGenome() = genomeGrid[phase]
	inline fun getState() = stateGrid[phase]

	inline fun mod(a: Int, b: Int) = ((a % b) + b) % b

	inline fun index(x: Int, y: Int) = y * width + x

	inline fun wrapIndex(x: Int, y: Int) = mod(y, height) * width + mod(x, width)

	fun clear() {
		val genome = getGenome()
		val state = getState()

		for (i in 0 until width * height) {
			genome[i] = 0
			state[i] = 0
		}
	}

	fun randomPopulate() {
		val genome = getGenome()
		val state = getState()

		for (i in 0 until width * height) {
			if (Random.nextInt(3) == 0) {
				genome[i] = Random.nextInt()
				state[i] = State.initial()
			} else {
				genome[i] = 0
				state[i] = 0
			}
		}
	}

	fun doStep() {
		val oldGenome = genomeGrid[phase]
		val newGenome = genomeGrid[otherPhase()]

		val oldState = stateGrid[phase]
		val newState = stateGrid[otherPhase()]

		/* prepare new buffers */
		for (i in 0 until width * height) {
			newGenome[i] = 0
			newState[i] = 0
		}

		/* set initial health */
		for (i in 0 until width * height) {
			val state = oldState[i]
			health[i] = if (State.getAlive(state)) State.getLife(state) else 0
		}

		/* organisms fighting for survival */
		for (y in 0 until height) {
			for (x in 0 until width) {
				val current = oldGenome[index(x, y)]
				val state = oldState[index(x, y)]

				if (State.getAlive(state)) {
					val neighbors = offsets.count { (ox, oy) -> State.getAlive(oldState[wrapIndex(x + ox, y + oy)]) }

					val actions = Organism.getActions(current, neighbors)
					actions.forEach { action ->
						when (action) {
							Organism.ACTION_ATTACK -> {
								offsets.forEach { (ox, oy) ->
									if (State.getAlive(oldState[wrapIndex(x + ox, y + oy)])) {
										if (!Organism.sameFamily(current, oldGenome[wrapIndex(x + ox, y + oy)])) {
											health[wrapIndex(x + ox, y + oy)] -= 1
										}
									}
								}
							}
							Organism.ACTION_DEFEND -> {
								health[index(x, y)] += 1
							}
							Organism.ACTION_HEAL -> {
								offsets.forEach { (ox, oy) ->
									if (State.getAlive(oldState[wrapIndex(x + ox, y + oy)])) {
										if (Organism.sameFamily(current, oldGenome[wrapIndex(x + ox, y + oy)])) {
											health[wrapIndex(x + ox, y + oy)] += 1
										}
									}
								}
							}
						}
					}
					/* organism did not like this configuration */
					if (actions.isEmpty()) {
						health[index(x, y)] -= 1
					}
				}
			}
		}

		/* organisms reproducing */
		for (y in 0 until height) {
			for (x in 0 until width) {
				val state = oldState[index(x, y)]

				if (!State.getAlive(state)) {
					val neighborhood = offsets.mapNotNull { (ox, oy) ->
						val index = wrapIndex(x + ox, y + oy)

						if (State.getAlive(oldState[index]))
							index to oldGenome[index]
						else
							null
					}

					val breeders = neighborhood.filter { (_, genome) -> Organism.canSpawn(genome, neighborhood.size) }

					val pairIndices = Organism.findSimilarPair(breeders.map { (_, genome) -> genome })
					if (pairIndices != null) {
						val newOrganism = Organism.breed(
							neighborhood[pairIndices.first].second,
							neighborhood[pairIndices.second].second,
							0.01f
						)

						newGenome[index(x, y)] = newOrganism
						newState[index(x, y)] = State.initial()
						health[neighborhood[pairIndices.first].first] -= 1
						health[neighborhood[pairIndices.second].first] -= 1
					}
				}
			}
		}

		/* organisms dying */
		for (i in 0 until width * height) {
			/* freshly created organism */
			if (State.getAlive(newState[i])) continue

			val life = health[i]
			val state = oldState[i]
			val newAge = State.getAge(state) + 1

			if (!State.getAlive(state) || life <= 0 || newAge >= State.DEAD_AGE) {
				newState[i] = 0
				newGenome[i] = 0
			} else {
				newState[i] = State.setLife(State.setAge(state, newAge), life.coerceAtMost(State.MAX_LIFE))
				newGenome[i] = oldGenome[i]
			}
		}

		phase = otherPhase()
	}
}
