import kotlin.random.Random

class Board(val width: Int, val height: Int) {
	var grid = IntArray(width * height)

	fun mod(a: Int, b: Int) = ((a % b) + b) % b

	fun accessDirect(x: Int, y: Int) = grid[y * width + x]

	fun access(x: Int, y: Int) = grid[mod(y, height) * width + mod(x, width)]

	fun clear() {
		for (i in 0 until width * height) {
			grid[i] = 0
		}
	}

	fun randomPopulate() {
		for (i in 0 until width * height) {
			grid[i] = if (Random.nextInt(3) == 0) 1 else 0
		}
	}

	fun doStep() {
		val newGrid = IntArray(width * height)

		for (y in 0 until height) {
			for (x in 0 until width) {
				val neighbors = access(x - 1, y) +
					access(x + 1, y) +
					access(x, y - 1) +
					access(x, y + 1) +
					access(x - 1, y - 1) +
					access(x + 1, y + 1) +
					access(x - 1, y + 1) +
					access(x + 1, y - 1)

				newGrid[y * width + x] = if (grid[y * width + x] == 0) {
					if (neighbors == 3) 1 else 0
				} else {
					if (neighbors < 2 || neighbors > 3) 0 else 1
				}
			}
		}

		grid = newGrid
	}
}
